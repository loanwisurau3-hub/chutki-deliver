package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import com.example.data.SecurityUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

data class MapsLaunchPlan(
    val url: String,
    val nativeNavigationUri: String? = null,
    val usedExactOrigin: Boolean,
    val remoteOriginSkipped: Boolean
)

/**
 * Resolves the rider's exact live GPS location using a multi-tier Android location pipeline
 * and launches Google Maps with the rider's start location automatically filled:
 *
 * 1. Exact live GPS (within [MAX_DRIVABLE_DISTANCE_KM]) -> `/maps/dir/?api=1&origin=<lat>,<lng>&destination=...`
 * 2. Saved local rider GPS fallback (within [MAX_DRIVABLE_DISTANCE_KM]) -> `/maps/dir/?api=1&origin=<lat>,<lng>&destination=...`
 * 3. No GPS fix yet / permission denied -> `google.navigation:q=<dest>&mode=d` (auto-locks "Your location"
 *    inside Google Maps) with `/maps/dir/?api=1&origin=My+Location&destination=...` web fallback so
 *    "Choose start location" is NEVER left blank.
 * 4. Remote / cross-continent emulator GPS (> [MAX_DRIVABLE_DISTANCE_KM]) with no local fallback ->
 *    opens the exact destination pin (`/maps/search/?api=1&query=...`) so Google Maps never fails
 *    with "Can't seem to find a way there".
 */
object RiderMapsNavigator {

    private const val FRESH_FIX_MAX_AGE_MS = 15_000L
    private const val HIGH_ACCURACY_TIMEOUT_MS = 3_500L
    private const val BALANCED_ACCURACY_TIMEOUT_MS = 2_000L
    private const val LAST_KNOWN_TIMEOUT_MS = 1_500L
    private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

    const val MAX_DRIVABLE_DISTANCE_KM = 1500.0
    private const val SERVICE_HUB_LAT = 26.4524
    private const val SERVICE_HUB_LNG = 89.9723

    private val COORD_PAIR_REGEX = Regex("""(-?\d{1,2}\.\d{3,})\s*,\s*(-?\d{1,3}\.\d{3,})""")

    fun isValidCoord(lat: Double?, lng: Double?): Boolean {
        if (lat == null || lng == null) return false
        if (!lat.isFinite() || !lng.isFinite()) return false
        if (lat == 0.0 && lng == 0.0) return false
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

    /**
     * Extracts `(lat, lng)` from common Google Maps or `geo:` URLs (e.g. `?q=26.4524,89.9723`,
     * `/@26.4524,89.9723,17z`, `query=26.4524,89.9723`, `geo:26.4524,89.9723`).
     */
    fun extractLatLngFromLocationLink(link: String?): Pair<Double, Double>? {
        val trimmed = link?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val decoded = runCatching { URLDecoder.decode(trimmed, "UTF-8") }.getOrDefault(trimmed)
        val match = COORD_PAIR_REGEX.find(decoded) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        return if (isValidCoord(lat, lng)) lat to lng else null
    }

    fun qualifyLocalAddress(rawLabel: String): String {
        val clean = rawLabel.trim().trimEnd(',')
        if (clean.isBlank()) return "Gossaigaon, Assam, India"
        val lower = clean.lowercase(Locale.US)
        return when {
            "india" in lower -> clean
            "assam" in lower -> "$clean, India"
            "gossaigaon" in lower -> "$clean, Assam, India"
            else -> "$clean, Gossaigaon, Assam, India"
        }
    }

    /**
     * Builds the optimal Google Maps launch plan so the rider's start location is always
     * automatically filled when available, never left blank as "Choose start location",
     * and never broken by cross-continent emulator coordinates.
     */
    fun buildMapsLaunchPlan(
        originLat: Double?,
        originLng: Double?,
        destLat: Double?,
        destLng: Double?,
        destLabelFallback: String,
        locationLink: String? = null,
        fallbackOriginLat: Double? = null,
        fallbackOriginLng: Double? = null
    ): MapsLaunchPlan {
        val safeLink = locationLink?.trim()?.takeIf {
            it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("https://", ignoreCase = true) ||
                it.startsWith("geo:", ignoreCase = true)
        }
        val linkCoords = extractLatLngFromLocationLink(safeLink)
        val resolvedDestCoords: Pair<Double, Double>? = when {
            isValidCoord(destLat, destLng) -> destLat!! to destLng!!
            linkCoords != null -> linkCoords
            else -> null
        }

        val destinationParam = when {
            resolvedDestCoords != null ->
                "${formatCoord(resolvedDestCoords.first)},${formatCoord(resolvedDestCoords.second)}"
            destLabelFallback.isNotBlank() ->
                encodeQueryParam(qualifyLocalAddress(destLabelFallback))
            safeLink != null ->
                return MapsLaunchPlan(
                    url = safeLink,
                    nativeNavigationUri = null,
                    usedExactOrigin = false,
                    remoteOriginSkipped = false
                )
            else ->
                encodeQueryParam("Gossaigaon, Assam, India")
        }

        val refDestLat = resolvedDestCoords?.first ?: SERVICE_HUB_LAT
        val refDestLng = resolvedDestCoords?.second ?: SERVICE_HUB_LNG

        val primaryValid = isValidCoord(originLat, originLng)
        val primaryDistKm = if (primaryValid) {
            SecurityUtils.calculateDistanceKm(originLat, originLng, refDestLat, refDestLng)
        } else {
            null
        }
        val primaryWithinRange = primaryValid && (primaryDistKm == null || primaryDistKm <= MAX_DRIVABLE_DISTANCE_KM)

        val fallbackValid = isValidCoord(fallbackOriginLat, fallbackOriginLng)
        val fallbackDistKm = if (fallbackValid) {
            SecurityUtils.calculateDistanceKm(fallbackOriginLat, fallbackOriginLng, refDestLat, refDestLng)
        } else {
            null
        }
        val fallbackWithinRange = fallbackValid && (fallbackDistKm == null || fallbackDistKm <= MAX_DRIVABLE_DISTANCE_KM)

        val chosenLocalOrigin: Pair<Double, Double>? = when {
            primaryWithinRange -> originLat!! to originLng!!
            fallbackWithinRange -> fallbackOriginLat!! to fallbackOriginLng!!
            else -> null
        }

        if (chosenLocalOrigin != null) {
            val originParam = "${formatCoord(chosenLocalOrigin.first)},${formatCoord(chosenLocalOrigin.second)}"
            val url = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$originParam" +
                "&destination=$destinationParam" +
                "&travelmode=driving&dir_action=navigate"
            return MapsLaunchPlan(
                url = url,
                nativeNavigationUri = null,
                usedExactOrigin = true,
                remoteOriginSkipped = false
            )
        }

        val isPrimaryRemote = primaryValid && primaryDistKm != null && primaryDistKm > MAX_DRIVABLE_DISTANCE_KM
        val isFallbackRemote = fallbackValid && fallbackDistKm != null && fallbackDistKm > MAX_DRIVABLE_DISTANCE_KM
        if (isPrimaryRemote || isFallbackRemote) {
            val pinUrl = "https://www.google.com/maps/search/?api=1&query=$destinationParam"
            return MapsLaunchPlan(
                url = pinUrl,
                nativeNavigationUri = null,
                usedExactOrigin = false,
                remoteOriginSkipped = true
            )
        }

        // No device GPS fix available yet (e.g. GPS cold start or permission denied):
        // 1. Prefer native `google.navigation:q=<dest>&mode=d` so Google Maps auto-locks "Your location".
        // 2. Include `&origin=My+Location` in the fallback URL so "Choose start location" is never blank.
        val nativeNavUri = "google.navigation:q=$destinationParam&mode=d"
        val directionsWithMyLocationUrl = "https://www.google.com/maps/dir/?api=1" +
            "&origin=My+Location" +
            "&destination=$destinationParam" +
            "&travelmode=driving&dir_action=navigate"

        return MapsLaunchPlan(
            url = directionsWithMyLocationUrl,
            nativeNavigationUri = nativeNavUri,
            usedExactOrigin = false,
            remoteOriginSkipped = false
        )
    }

    fun buildDirectionsUrl(
        originLat: Double?,
        originLng: Double?,
        destLat: Double?,
        destLng: Double?,
        destLabelFallback: String,
        locationLink: String? = null,
        fallbackOriginLat: Double? = null,
        fallbackOriginLng: Double? = null
    ): String = buildMapsLaunchPlan(
        originLat = originLat,
        originLng = originLng,
        destLat = destLat,
        destLng = destLng,
        destLabelFallback = destLabelFallback,
        locationLink = locationLink,
        fallbackOriginLat = fallbackOriginLat,
        fallbackOriginLng = fallbackOriginLng
    ).url

    private fun encodeQueryParam(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    /**
     * Obtains the freshest exact GPS fix for the rider (via foreground service telemetry,
     * high-accuracy & balanced FusedLocationProviderClient, and Android LocationManager)
     * and launches Google Maps with the rider's exact start location automatically set.
     */
    @SuppressLint("MissingPermission")
    suspend fun openDirectionsWithExactRiderLocation(
        context: Context,
        fallbackOriginLat: Double?,
        fallbackOriginLng: Double?,
        destLat: Double?,
        destLng: Double?,
        destLabelFallback: String,
        locationLink: String? = null
    ) {
        val (liveOriginLat, liveOriginLng) = resolveLiveDeviceLocation(context)

        val plan = buildMapsLaunchPlan(
            originLat = liveOriginLat,
            originLng = liveOriginLng,
            destLat = destLat,
            destLng = destLng,
            destLabelFallback = destLabelFallback,
            locationLink = locationLink,
            fallbackOriginLat = fallbackOriginLat,
            fallbackOriginLng = fallbackOriginLng
        )

        if (plan.remoteOriginSkipped) {
            Toast.makeText(
                context,
                "Device GPS is >1500 km from Gossaigaon (remote/emulator GPS). Showing exact destination pin.",
                Toast.LENGTH_LONG
            ).show()
        }

        // 1. When native `google.navigation:` URI is planned (no numeric GPS fix yet),
        // launch Google Maps Navigation directly so Maps auto-locks "Your location".
        if (!plan.nativeNavigationUri.isNullOrBlank()) {
            val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse(plan.nativeNavigationUri)).apply {
                setPackage(GOOGLE_MAPS_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launchedNativeNav = runCatching {
                if (navIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(navIntent)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (launchedNativeNav) return
        }

        // 2. Launch exact-origin Directions (or destination pin) in Google Maps app.
        val uri = Uri.parse(plan.url)
        val googleMapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(GOOGLE_MAPS_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launchedMapsApp = runCatching {
            if (googleMapsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(googleMapsIntent)
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (!launchedMapsApp) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallbackIntent) }
                .onFailure {
                    Toast.makeText(context, "Unable to open Maps app.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolveLiveDeviceLocation(context: Context): Pair<Double?, Double?> {
        val now = System.currentTimeMillis()
        val live = RiderLocationForegroundService.telemetry.value

        // Tier 1: Fresh foreground service GPS fix (<= 15 seconds old)
        if (isValidCoord(live.lat, live.lng) && (now - live.lastFixAtMs) <= FRESH_FIX_MAX_AGE_MS) {
            return live.lat to live.lng
        }

        if (RiderLocationForegroundService.hasLocationPermission(context)) {
            val fused = LocationServices.getFusedLocationProviderClient(context)

            // Tier 2: High-accuracy live FusedLocationProvider fix
            val highAccuracyCts = CancellationTokenSource()
            val highAccuracyFix = withTimeoutOrNull(HIGH_ACCURACY_TIMEOUT_MS) {
                runCatching {
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, highAccuracyCts.token).await()
                }.getOrNull()
            }
            if (highAccuracyFix != null && isValidCoord(highAccuracyFix.latitude, highAccuracyFix.longitude)) {
                return highAccuracyFix.latitude to highAccuracyFix.longitude
            }
            highAccuracyCts.cancel()

            // Tier 3: Fast balanced-power fix (Wi-Fi / Cell / Emulator fast lock)
            val balancedCts = CancellationTokenSource()
            val balancedFix = withTimeoutOrNull(BALANCED_ACCURACY_TIMEOUT_MS) {
                runCatching {
                    fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, balancedCts.token).await()
                }.getOrNull()
            }
            if (balancedFix != null && isValidCoord(balancedFix.latitude, balancedFix.longitude)) {
                return balancedFix.latitude to balancedFix.longitude
            }
            balancedCts.cancel()

            // Tier 4: Fused lastLocation
            val fusedLast = withTimeoutOrNull(LAST_KNOWN_TIMEOUT_MS) {
                runCatching { fused.lastLocation.await() }.getOrNull()
            }
            if (fusedLast != null && isValidCoord(fusedLast.latitude, fusedLast.longitude)) {
                return fusedLast.latitude to fusedLast.longitude
            }

            // Tier 5: Android LocationManager providers (GPS / NETWORK / PASSIVE)
            val lmBest = getBestLocationManagerFix(context)
            if (lmBest != null && isValidCoord(lmBest.latitude, lmBest.longitude)) {
                return lmBest.latitude to lmBest.longitude
            }
        }

        // Tier 6: Older foreground telemetry fix if any
        if (isValidCoord(live.lat, live.lng)) {
            return live.lat to live.lng
        }
        return null to null
    }

    @SuppressLint("MissingPermission")
    private fun getBestLocationManagerFix(context: Context): Location? {
        val lm = runCatching {
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        }.getOrNull() ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var best: Location? = null
        for (provider in providers) {
            val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (!isValidCoord(loc.latitude, loc.longitude)) continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best
    }
}
