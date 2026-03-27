package com.devdroid.campuscommute.driver

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class DriverLocationManager(
    private val context: Context,
    private val busId: String = "bus56"
) {
    private val dbRef = FirebaseDatabase.getInstance().getReference("locations/$busId")
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 5000L // every 5 seconds
    ).setMinUpdateIntervalMillis(4000L).build()

    private var locationCallback: LocationCallback? = null
    private var isSharing = false

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isSharing) return
        isSharing = true

        // One-time immediate location
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) sendLocationToFirebase(loc)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) sendLocationToFirebase(loc)
                else Log.w("DriverUpdate", "⚠️ Location is null")
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        Log.d("DriverUpdate", "📡 Started location sharing for $busId")
    }

    fun stopLocationUpdates() {
        if (!isSharing) return
        isSharing = false

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("DriverUpdate", "🛑 Stopped location updates for $busId")
        }

        // 🔹 Clear Firebase location (set to null)
        val nullData = mapOf(
            "lat" to null,
            "lng" to null,
            "timestamp" to null
        )
        dbRef.updateChildren(nullData).addOnSuccessListener {
            Log.d("DriverUpdate", "🧹 Cleared location for $busId")
        }.addOnFailureListener {
            Log.e("DriverUpdate", "❌ Failed to clear location: ${it.message}")
        }
    }

    private fun sendLocationToFirebase(location: Location) {
        if (!isSharing) return

        val data = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )

        dbRef.setValue(data)
            .addOnSuccessListener {
                Log.d("DriverUpdate", "✅ Updated: $data")
            }
            .addOnFailureListener {
                Log.e("DriverUpdate", "❌ Firebase update failed: ${it.message}")
            }
    }
}
