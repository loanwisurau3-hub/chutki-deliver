package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

sealed class GpsRadarState {
    data object Hidden : GpsRadarState()
    data object Connecting : GpsRadarState()
    data class Live(val accuracyMeters: Int?, val isNetworkOnline: Boolean) : GpsRadarState()
    data class Error(val message: String, val badge: String) : GpsRadarState()
}

class LocationTracker(private val context: Context) {

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    private var callback: LocationCallback? = null

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isNetworkOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @SuppressLint("MissingPermission")
    fun start(
        onLocation: (Location) -> Unit,
        onPermissionMissing: () -> Unit
    ) {
        if (!hasLocationPermission()) {
            onPermissionMissing()
            return
        }
        if (callback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2500L)
            .setMaxUpdateDelayMillis(10000L)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(onLocation)
            }
        }
        callback = cb
        client.requestLocationUpdates(request, cb, Looper.getMainLooper())
        client.lastLocation.addOnSuccessListener { loc -> loc?.let(onLocation) }
    }

    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}
