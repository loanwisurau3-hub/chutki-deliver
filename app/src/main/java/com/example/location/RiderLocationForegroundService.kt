package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.data.FirebaseFleetRepository
import com.example.data.SecurityUtils
import com.example.notifications.FleetNotificationHelper
import com.example.security.SecurityShield
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LiveLocationTelemetry(
    val isRunning: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracyMeters: Int? = null,
    val speedKmh: Int? = null,
    val bearingDeg: Int? = null,
    val batteryPct: Int? = null,
    val lastFixAtMs: Long = 0L,
    val lastFirebaseSyncAtMs: Long = 0L,
    val permissionMissing: Boolean = false,
    val mockLocationDetected: Boolean = false,
    val spoofReason: String? = null
)

class RiderLocationForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.example.location.ACTION_START_LOCATION_TRACKING"
        const val ACTION_STOP = "com.example.location.ACTION_STOP_LOCATION_TRACKING"
        const val ACTION_FORCE_SYNC = "com.example.location.ACTION_FORCE_SYNC"
        const val EXTRA_UID = "extra_rider_uid"
        const val EXTRA_ONLINE = "extra_rider_online"

        const val UPDATE_INTERVAL_MS = 10_000L

        private val _telemetry = MutableStateFlow(LiveLocationTelemetry())
        val telemetry: StateFlow<LiveLocationTelemetry> = _telemetry.asStateFlow()

        fun hasLocationPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }

        fun start(context: Context, uid: String, online: Boolean) {
            if (uid.isBlank()) return
            if (!hasLocationPermission(context)) {
                _telemetry.update { it.copy(isRunning = false, permissionMissing = true) }
                return
            }
            val intent = Intent(context, RiderLocationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_UID, uid)
                putExtra(EXTRA_ONLINE, online)
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure { err ->
                Log.w("RiderLocationFGS", "Could not start location foreground service: ${err.message}")
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, RiderLocationForegroundService::class.java))
            }
            _telemetry.value = LiveLocationTelemetry()
        }

        fun requestImmediateSync(context: Context, uid: String, online: Boolean) {
            if (uid.isBlank() || !hasLocationPermission(context)) return
            val intent = Intent(context, RiderLocationForegroundService::class.java).apply {
                action = ACTION_FORCE_SYNC
                putExtra(EXTRA_UID, uid)
                putExtra(EXTRA_ONLINE, online)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var repo: FirebaseFleetRepository

    private var activeUid: String? = null
    @Volatile
    private var riderOnline: Boolean = true
    @Volatile
    private var riderSuspended: Boolean = false
    @Volatile
    private var latestLocation: Location? = null
    @Volatile
    private var activeLockedOrderIds: Set<String> = emptySet()

    private val lastTrailSavedAt = mutableMapOf<String, Long>()
    private val lastTrailCoords = mutableMapOf<String, Pair<Double, Double>>()
    private var lastValidLat: Double? = null
    private var lastValidLng: Double? = null
    private var lastValidFixAtMs: Long = 0L
    private var lastSpoofLogAtMs: Long = 0L

    private var locationCallback: LocationCallback? = null
    private var syncTickerJob: Job? = null
    private var ordersObserverJob: Job? = null
    private var profileObserverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        repo = FirebaseFleetRepository(applicationContext)
        FleetNotificationHelper.ensureChannels(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTrackingAndSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                if (!hasLocationPermission(this)) {
                    _telemetry.update { it.copy(isRunning = false, permissionMissing = true) }
                    stopSelf()
                    return START_NOT_STICKY
                }
                val uid = intent?.getStringExtra(EXTRA_UID).orEmpty()
                if (intent?.hasExtra(EXTRA_ONLINE) == true) {
                    riderOnline = intent.getBooleanExtra(EXTRA_ONLINE, true)
                }

                if (!promoteToForeground()) {
                    return START_NOT_STICKY
                }

                if (uid.isNotBlank() && uid != activeUid) {
                    activeUid = uid
                    startObservers(uid)
                    startLocationUpdates()
                    startTenSecondSyncLoop(uid)
                } else if (uid.isBlank() && activeUid == null) {
                    stopTrackingAndSelf()
                    return START_NOT_STICKY
                }
                return START_REDELIVER_INTENT
            }
            ACTION_FORCE_SYNC -> {
                if (!hasLocationPermission(this)) {
                    stopTrackingAndSelf()
                    return START_NOT_STICKY
                }
                val uid = intent.getStringExtra(EXTRA_UID).orEmpty().ifBlank { activeUid.orEmpty() }
                if (intent.hasExtra(EXTRA_ONLINE)) {
                    riderOnline = intent.getBooleanExtra(EXTRA_ONLINE, true)
                }
                if (!promoteToForeground()) return START_NOT_STICKY
                if (uid.isNotBlank()) {
                    if (uid != activeUid) {
                        activeUid = uid
                        startObservers(uid)
                        startLocationUpdates()
                        startTenSecondSyncLoop(uid)
                    }
                    latestLocation?.let { loc ->
                        val lat = Math.round(loc.latitude * 1_000_000.0) / 1_000_000.0
                        val lng = Math.round(loc.longitude * 1_000_000.0) / 1_000_000.0
                        val accuracy = if (loc.hasAccuracy()) Math.round(loc.accuracy) else null
                        val speedKmh = if (loc.hasSpeed()) Math.round(loc.speed * 3.6f).coerceAtLeast(0) else null
                        val bearing = if (loc.hasBearing()) Math.round(loc.bearing).coerceIn(0, 360) else null
                        serviceScope.launch {
                            pushLocationToFirebase(uid, lat, lng, accuracy, speedKmh, bearing)
                        }
                    }
                }
                return START_REDELIVER_INTENT
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun promoteToForeground(): Boolean {
        val current = _telemetry.value
        val notif = FleetNotificationHelper.buildLocationForegroundNotification(
            context = this,
            lat = current.lat,
            lng = current.lng,
            accuracyMeters = current.accuracyMeters
        )
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        return runCatching {
            ServiceCompat.startForeground(
                this,
                FleetNotificationHelper.NOTIF_ID_LOCATION,
                notif,
                fgsType
            )
            _telemetry.update { it.copy(isRunning = true, permissionMissing = false) }
            true
        }.onFailure { err ->
            Log.e("RiderLocationFGS", "startForeground(location) failed: ${err.message}", err)
            _telemetry.update { it.copy(isRunning = false) }
            stopSelf()
        }.getOrDefault(false)
    }

    private fun updateForegroundNotification(lat: Double, lng: Double, accuracy: Int?) {
        if (!_telemetry.value.isRunning) return
        if (!FleetNotificationHelper.hasNotificationPermission(this)) return
        val notif = FleetNotificationHelper.buildLocationForegroundNotification(
            context = this,
            lat = lat,
            lng = lng,
            accuracyMeters = accuracy
        )
        runCatching {
            NotificationManagerCompat.from(this).notify(FleetNotificationHelper.NOTIF_ID_LOCATION, notif)
        }
    }

    private fun startObservers(uid: String) {
        ordersObserverJob?.cancel()
        profileObserverJob?.cancel()

        profileObserverJob = serviceScope.launch {
            repo.warmUp()
            repo.observeRiderProfile(uid).collect { profile ->
                riderOnline = profile.online
                riderSuspended = profile.suspended
            }
        }

        ordersObserverJob = serviceScope.launch {
            repo.warmUp()
            repo.observeAssignedOrders(uid).collect { orders ->
                activeLockedOrderIds = orders.values
                    .filter {
                        it.status in setOf(
                            "PICKED_UP",
                            "OUT_FOR_DELIVERY",
                            "CANCELLATION_REQUESTED",
                            "EMERGENCY_INVESTIGATION"
                        )
                    }
                    .map { it.orderId }
                    .toSet()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission(this)) {
            _telemetry.update { it.copy(isRunning = false, permissionMissing = true) }
            stopTrackingAndSelf()
            return
        }
        if (locationCallback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                onLocationFixReceived(loc)
            }
        }
        locationCallback = cb

        runCatching {
            fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) onLocationFixReceived(loc)
            }
        }.onFailure { err ->
            Log.e("RiderLocationFGS", "Failed requesting location updates: ${err.message}", err)
        }
    }

    private fun readBatteryPercentage(): Int? = runCatching {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) ((level * 100) / scale).coerceIn(0, 100) else null
    }.getOrNull()

    private fun onLocationFixReceived(location: Location) {
        val now = System.currentTimeMillis()
        val lat = Math.round(location.latitude * 1_000_000.0) / 1_000_000.0
        val lng = Math.round(location.longitude * 1_000_000.0) / 1_000_000.0
        val fraudCheck = SecurityShield.validateLocationFix(
            lat = lat,
            lng = lng,
            isMockProvider = SecurityShield.isLocationMocked(location),
            prevLat = lastValidLat,
            prevLng = lastValidLng,
            prevFixAtMs = lastValidFixAtMs,
            nowMs = now
        )
        if (!fraudCheck.isValid) {
            _telemetry.update {
                it.copy(
                    isRunning = true,
                    mockLocationDetected = true,
                    spoofReason = fraudCheck.reason
                )
            }
            val uid = activeUid
            if (!uid.isNullOrBlank() && (now - lastSpoofLogAtMs >= 120_000L)) {
                lastSpoofLogAtMs = now
                val eventType = if (fraudCheck.isTeleported) "GPS_TELEPORT_SPOOFING" else "MOCK_GPS_SPOOFING_DETECTED"
                val primaryOrderId = activeLockedOrderIds.firstOrNull() ?: "SHIFT_GPS"
                repo.logSecurityEvent(uid, primaryOrderId, eventType, fraudCheck.reason, "BLOCKED")
            }
            return
        }

        lastValidLat = lat
        lastValidLng = lng
        lastValidFixAtMs = now
        latestLocation = location
        val accuracy = if (location.hasAccuracy()) Math.round(location.accuracy) else null
        val speedKmh = if (location.hasSpeed()) Math.round(location.speed * 3.6f).coerceAtLeast(0) else null
        val bearingDeg = if (location.hasBearing()) Math.round(location.bearing).coerceIn(0, 360) else null
        val batteryPct = readBatteryPercentage()
        val shouldImmediateSync = _telemetry.value.lastFirebaseSyncAtMs == 0L

        _telemetry.update {
            it.copy(
                isRunning = true,
                lat = lat,
                lng = lng,
                accuracyMeters = accuracy,
                speedKmh = speedKmh,
                bearingDeg = bearingDeg,
                batteryPct = batteryPct,
                lastFixAtMs = now,
                permissionMissing = false,
                mockLocationDetected = false,
                spoofReason = null
            )
        }
        updateForegroundNotification(lat, lng, accuracy)

        val uid = activeUid
        if (shouldImmediateSync && !uid.isNullOrBlank()) {
            serviceScope.launch {
                pushLocationToFirebase(uid, lat, lng, accuracy, speedKmh, bearingDeg)
            }
        }
    }

    private fun startTenSecondSyncLoop(uid: String) {
        syncTickerJob?.cancel()
        syncTickerJob = serviceScope.launch {
            repo.warmUp()
            while (isActive) {
                delay(UPDATE_INTERVAL_MS)
                val loc = latestLocation ?: continue
                val lat = Math.round(loc.latitude * 1_000_000.0) / 1_000_000.0
                val lng = Math.round(loc.longitude * 1_000_000.0) / 1_000_000.0
                val accuracy = if (loc.hasAccuracy()) Math.round(loc.accuracy) else null
                val speedKmh = if (loc.hasSpeed()) Math.round(loc.speed * 3.6f).coerceAtLeast(0) else null
                val bearingDeg = if (loc.hasBearing()) Math.round(loc.bearing).coerceIn(0, 360) else null
                pushLocationToFirebase(uid, lat, lng, accuracy, speedKmh, bearingDeg)
            }
        }
    }

    private suspend fun pushLocationToFirebase(
        uid: String,
        lat: Double,
        lng: Double,
        accuracy: Int?,
        speedKmh: Int?,
        bearingDeg: Int?
    ) {
        if (riderSuspended) return
        val batteryPct = readBatteryPercentage()

        repo.updateRiderLocation(
            uid = uid,
            lat = lat,
            lng = lng,
            accuracy = accuracy,
            online = riderOnline,
            speedKmh = speedKmh,
            bearing = bearingDeg,
            batteryPct = batteryPct
        )

        val now = System.currentTimeMillis()
        for (orderId in activeLockedOrderIds) {
            val prev = lastTrailCoords[orderId]
            val movedKm = if (prev != null) {
                SecurityUtils.calculateDistanceKm(prev.first, prev.second, lat, lng)
            } else {
                1.0
            }
            val saveTrail = (now - (lastTrailSavedAt[orderId] ?: 0L) >= 20_000L) &&
                (movedKm == null || movedKm > 0.025)
            if (saveTrail) {
                lastTrailSavedAt[orderId] = now
                lastTrailCoords[orderId] = lat to lng
            }
            repo.updateOrderLocationAndTrail(orderId, lat, lng, accuracy, saveTrail)
        }

        _telemetry.update { it.copy(batteryPct = batteryPct, lastFirebaseSyncAtMs = now) }
    }

    private fun stopTrackingAndSelf() {
        syncTickerJob?.cancel()
        ordersObserverJob?.cancel()
        profileObserverJob?.cancel()
        locationCallback?.let { runCatching { fusedClient.removeLocationUpdates(it) } }
        locationCallback = null
        activeUid = null
        latestLocation = null
        activeLockedOrderIds = emptySet()
        lastTrailSavedAt.clear()
        lastTrailCoords.clear()
        _telemetry.value = LiveLocationTelemetry()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        syncTickerJob?.cancel()
        ordersObserverJob?.cancel()
        profileObserverJob?.cancel()
        locationCallback?.let { runCatching { fusedClient.removeLocationUpdates(it) } }
        locationCallback = null
        activeUid = null
        latestLocation = null
        activeLockedOrderIds = emptySet()
        lastTrailSavedAt.clear()
        lastTrailCoords.clear()
        _telemetry.value = LiveLocationTelemetry()
        serviceScope.cancel()
        super.onDestroy()
    }
}
