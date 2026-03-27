//package com.devdroid.campuscommute.ui.screens.tracker
//
//import android.util.Log
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.PinDrop
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//// Assuming CamStop is accessible or defined here if necessary
//// Since CamStop definition isn't provided, we'll redefine it based on Stop structure for this file's context.
//// In a real project, you would import com.devdroid.campuscommute.data.CamStop
//data class CamStop( // Re-defining CamStop locally for context, assuming structure mirrors Stop
//    val id: String,
//    val name: String,
//    val sequence: Int,
//    val latitude: Double,
//    val longitude: Double
//)
//
//data class RouteWrapper(val stops: List<CamStop> = emptyList()) // Adjusted to CamStop
//
//// Helper function to simulate fetching all possible campus stops (same as driver screen)
//private fun getCollegeStopsMock(): List<CamStop> {
//    return listOf(
//        CamStop("S_01", "Main Gate Parking", 1, 12.971, 77.594),
//        CamStop("S_02", "Library Block Entrance", 2, 12.972, 77.595),
//        CamStop("S_03", "Sports Complex Drop-off", 3, 12.973, 77.596),
//        CamStop("S_04", "Canteen Service Area", 4, 12.974, 77.597),
//        CamStop("S_05", "Admin Block Parking", 5, 12.975, 77.598)
//    )
//}
//
//data class BusManualStatus(
//    val stopName: String,
//    val timestamp: Long
//)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun BusStatusCardScreen(
//    busId: String,
//    onBackClick: () -> Unit = {}
//) {
//    // We need all possible stops to resolve the ID to a Name
//    val allStops = remember { getCollegeStopsMock() }
//    val realtimeDb = remember { FirebaseDatabase.getInstance().getReference("locations/$busId") }
//
//    var status by remember { mutableStateOf<BusManualStatus?>(null) }
//    var isLoading by remember { mutableStateOf(true) }
//    var isManualMode by remember { mutableStateOf(false) }
//
//    // Listener for Realtime DB
//    DisposableEffect(busId) {
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                isLoading = false
//
//                val statusType = snapshot.child("statusType").getValue(String::class.java)
//                val currentStopId = snapshot.child("currentStopId").getValue(String::class.java)
//                var tsLong: Long? = snapshot.child("timestamp").getValue(Long::class.java)
//
//                if (tsLong == null) {
//                    val tsDouble = snapshot.child("timestamp").getValue(Double::class.java)
//                    if (tsDouble != null) tsLong = tsDouble.toLong()
//                }
//                if (tsLong != null && tsLong < 1000000000000L) {
//                    tsLong *= 1000
//                }
//
//                if (statusType == "MANUAL_STOP" && currentStopId != null && tsLong != null) {
//                    // RESOLVE ID using allStops (now List<CamStop>)
//                    val manualStopName = allStops.find { it.id == currentStopId }?.name ?: "Unknown Campus Stop"
//                    status = BusManualStatus(manualStopName, tsLong)
//                    isManualMode = true
//                } else {
//                    status = null
//                    isManualMode = false
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("BusStatusCardScreen", "DB Error: ${error.message}")
//                isLoading = false
//                isManualMode = false
//                status = null
//            }
//        }
//        realtimeDb.addValueEventListener(listener)
//        onDispose { realtimeDb.removeEventListener(listener) }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Bus $busId Live Status") },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        contentWindowInsets = WindowInsets(0, 0, 0, 0)
//    ) { padding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .background(Color(0xFFF0F4F8)), // Light background for contrast
//            contentAlignment = Alignment.Center
//        ) {
//            when {
//                isLoading -> {
//                    CircularProgressIndicator(color = Color(0xFF266FEF))
//                }
//                isManualMode && status != null -> {
//                    ManualBusStatusCard(status = status!!)
//                }
//                else -> {
//                    // Fallback when bus is running on GPS or is offline
//                    Text(
//                        "Bus is currently running on the route or is offline. Check the main tracker map for live GPS updates.",
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.padding(32.dp),
//                        color = Color.Gray
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun ManualBusStatusCard(status: BusManualStatus) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth(0.9f)
//            .padding(16.dp),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(8.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .padding(24.dp)
//                .fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                imageVector = Icons.Default.PinDrop,
//                contentDescription = "Bus Standing",
//                tint = Color(0xFF16A34A), // Green/Success Color
//                modifier = Modifier.size(48.dp)
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                "BUS IS STANDING NEAR",
//                style = MaterialTheme.typography.titleSmall,
//                color = Color.Gray,
//                fontWeight = FontWeight.SemiBold
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                status.stopName,
//                style = MaterialTheme.typography.headlineSmall,
//                color = Color.Black,
//                fontWeight = FontWeight.ExtraBold,
//                textAlign = TextAlign.Center
//            )
//
//            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
//
//            val timeFormatted = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(
//                Date(
//                    status.timestamp
//                )
//            )
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(Icons.Default.Info, contentDescription = "Manual Update", tint = Color.LightGray, modifier = Modifier.size(16.dp))
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    "Manual Check-in: $timeFormatted",
//                    fontSize = 12.sp,
//                    color = Color.Gray
//                )
//            }
//        }
//    }
//}