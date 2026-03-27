package com.devdroid.campuscommute.ui.screens.driver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devdroid.campuscommute.R
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Database Reference
    private val dbRef = FirebaseDatabase.getInstance().getReference("locations")
    private var busId: String? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Define the callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val currentBusId = busId ?: return

                result.lastLocation?.let { location ->
                    Log.d("LocationService", "📍 Location: ${location.latitude}, ${location.longitude}")

                    val data = mapOf(
                        "lat" to location.latitude,
                        "lng" to location.longitude,
                        "speed" to location.speed,
                        "timestamp" to System.currentTimeMillis()
                    )

                    // Write to Firebase
                    dbRef.child(currentBusId).setValue(data)
                        .addOnSuccessListener { Log.d("LocationService", "✅ Firebase Updated") }
                        .addOnFailureListener { Log.e("LocationService", "❌ Firebase Failed: ${it.message}") }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == "ACTION_START") {
            busId = intent.getStringExtra("BUS_ID")
            if (busId != null) {
                startForegroundService()
                startLocationUpdates()
            } else {
                stopSelf()
            }
        } else if (action == "ACTION_STOP") {
            stopLocationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startForegroundService() {
        val channelId = "location_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Bus Tracking", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bus $busId Tracking Active")
            .setContentText("Sharing location with students...")
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon exists
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}