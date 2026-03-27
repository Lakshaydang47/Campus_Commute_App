package com.devdroid.campuscommute.ui.screens.driver

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.CAMPUS_STOP_POINTS
import com.devdroid.campuscommute.data.CamStop // Assuming you put CamStop here
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// ⚠️ IMPORTANT: Pass the driver's Bus ID as a String (e.g., "15") ⚠️
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverParkedLocationScreen(navController: NavController, driverBusId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Set Parking Location", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Bus #$driverBusId: Select your current stop:",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
            items(CAMPUS_STOP_POINTS) { stop ->
                BusStopSelectionCard(stop = stop) { selectedStop ->
                    // ⚡ Action on tap ⚡
                    updateBusParkingLocation(db, driverBusId, selectedStop) {
                        Toast.makeText(context, "Location updated: ${selectedStop.name}", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() // Navigate back after success
                    }
                }
            }
        }
    }
}

// Helper Composable for each stop point card
@Composable
fun BusStopSelectionCard(stop: CamStop, onStopSelected: (CamStop) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStopSelected(stop) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PinDrop,
                contentDescription = null,
                tint = Color(0xFF266FEF),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stop.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stop.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

// Helper function to write to Firestore
fun updateBusParkingLocation(
    db: FirebaseFirestore,
    busId: String,
    stop: CamStop,
    onSuccess: () -> Unit
) {
    val timestamp = System.currentTimeMillis()

    val busStatusData = hashMapOf(
        "busId" to busId,
        "locationName" to stop.name,
        "timestamp" to timestamp,
        "lat" to stop.latitude,
        "lng" to stop.longitude
    )

    db.collection("bus_status").document(busId)
        .set(busStatusData, SetOptions.merge())
        .addOnSuccessListener {

            // ⭐ CALL NOTIFICATION FUNCTION HERE ⭐
            sendLocationNotification(busId, stop, timestamp)

            onSuccess()
        }
}


fun sendLocationNotification(busId: String, stop: CamStop, timestamp: Long) {

    val ref = FirebaseDatabase.getInstance().getReference("notifications")

    val notificationId = ref.push().key ?: return

    val data = mapOf(
        "id" to notificationId,
        "busId" to busId,
        "title" to "Bus Location Updated",
        "message" to "Bus is now at: ${stop.name}",
        "type" to "info",
        "timestamp" to timestamp
    )

    ref.child(notificationId).setValue(data)
}
