package com.devdroid.campuscommute.ui.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.devdroid.campuscommute.utils.NotificationUtil
import com.devdroid.campuscommute.utils.RequestNotificationPermission

data class CurrentBusStop(
    val busId: String,
    val locationName: String,
    val lat: Double,
    val lng: Double
)

@Composable
fun UserLocationDisplayCard(userBusId: String) {

    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var parkedLocation by remember { mutableStateOf<CurrentBusStop?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    // NEW: track last seen location and initial load state
    var lastLocationName by remember { mutableStateOf<String?>(null) }
    var isInitialSnapshot by remember { mutableStateOf(true) }

    // Request notification permission before starting listener
    var permissionGranted by remember { mutableStateOf(false) }
    RequestNotificationPermission(onGranted = { permissionGranted = true })

    if (permissionGranted) {

        DisposableEffect(userBusId, refreshKey) {
            isLoading = true

            val docRef = db.collection("bus_status").document(userBusId)
            var listener: ListenerRegistration? = null

            listener = docRef.addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val newLocation = snapshot.getString("locationName") ?: "Unknown"

                    // ONLY send notification if:
                    // 1) it's not the very first snapshot after the listener starts, AND
                    // 2) the location name changed compared to lastLocationName
                    val shouldNotify = !isInitialSnapshot && (newLocation != lastLocationName)

                    // Update stored last location (always update so future comparisons work)
                    lastLocationName = newLocation
                    isInitialSnapshot = false

                    if (shouldNotify) {
                        NotificationUtil.sendLocalNotification(
                            context = context,
                            title = "Bus Location Updated",
                            message = "Bus $userBusId is now at: $newLocation"
                        )
                    }

                    parkedLocation = CurrentBusStop(
                        busId = snapshot.getString("busId") ?: userBusId,
                        locationName = newLocation,
                        lat = snapshot.getDouble("lat") ?: 0.0,
                        lng = snapshot.getDouble("lng") ?: 0.0
                    )
                } else {
                    // Document missing / bus not currently reporting - update UI and reset
                    parkedLocation = null

                    // If you want to notify when bus goes offline, you can implement that here.
                    // For now, we will not notify on missing documents to avoid excessive alerts.
                }
            }

            onDispose { listener?.remove() }
        }
    }

    val onRefresh: () -> Unit = {
        // Reset initial-snapshot state so refresh won't immediately trigger a notification.
        isInitialSnapshot = true
        refreshKey++
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bus Location Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Status",
                        tint = Color(0xFF266FEF)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Crossfade(targetState = isLoading, label = "loading_state") { loading ->
                if (loading) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Fetching real-time status...", color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    LocationContent(parkedLocation)
                }
            }
        }
    }
}

@Composable
fun LocationContent(parkedLocation: CurrentBusStop?) {

    val (statusText, statusColor, bgColor) = when {
        parkedLocation != null -> Triple(
            parkedLocation.locationName,
            Color(0xFF388E3C),
            Color(0xFFE8F5E9)
        )
        else -> Triple(
            "Bus is not in campus",
            Color(0xFFD32F2F),
            Color(0xFFFFEBEE)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        Text("Current Status:", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PinDrop,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }
        }
    }
}
