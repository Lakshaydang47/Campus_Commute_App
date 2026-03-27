package com.devdroid.campuscommute.ui.screens.driver

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.Stop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

// Helper for route fetching
data class RouteWrapper(val stops: List<Stop> = emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerCountScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val realtimeDb = FirebaseDatabase.getInstance()
    val auth = FirebaseAuth.getInstance()

    // --- State ---
    var busId by remember { mutableStateOf("") }
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var totalPassengers by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // --- QR Scanner State ---
    var showScanner by remember { mutableStateOf(false) }
    // Keep track of scanned IDs to prevent double counting
    val boardedStudents = remember { mutableStateListOf<String>() }

    // 1. Fetch Bus ID & Route
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    val fetchedId = userDoc.get("busId")?.toString()

                    if (!fetchedId.isNullOrEmpty()) {
                        busId = fetchedId
                        // Fetch Route
                        db.collection("routes").document(fetchedId).get()
                            .addOnSuccessListener { routeDoc ->
                                if (routeDoc.exists()) {
                                    val wrapper = routeDoc.toObject(RouteWrapper::class.java)
                                    stops = wrapper?.stops?.sortedBy { it.sequence } ?: emptyList()
                                }
                                isLoading = false
                            }
                            .addOnFailureListener { isLoading = false }
                    } else {
                        isLoading = false
                        Toast.makeText(context, "No Bus Assigned", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    // 2. Update Realtime Database
    fun updateOccupancy(newCount: Int) {
        if (busId.isNotEmpty()) {
            totalPassengers = newCount.coerceAtLeast(0)
            realtimeDb.getReference("buses/$busId/occupancy").setValue(totalPassengers)
            realtimeDb.getReference("buses/$busId/lastUpdated").setValue(System.currentTimeMillis())
        }
    }

    // 3. Handle QR Scan Result
    fun handleScan(studentId: String) {
        if (boardedStudents.contains(studentId)) {
            Toast.makeText(context, "Student already boarded!", Toast.LENGTH_SHORT).show()
        } else {
            boardedStudents.add(studentId)
            // Auto-increment count
            updateOccupancy(totalPassengers + 1)
            Toast.makeText(context, "Verified ✅", Toast.LENGTH_SHORT).show()

            // Close scanner after successful scan (Optional: keep open for batch scanning)
            showScanner = false
        }
    }

    // --- Main UI ---
    if (showScanner) {
        // 📸 Show Camera View
        DriverQRScanner(
            onScanSuccess = { id -> handleScan(id) },
            onClose = { showScanner = false }
        )
    } else {
        // 📋 Show List View
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Passenger Count (Bus $busId)", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showScanner = true },
                    containerColor = Color(0xFF266FEF),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.QrCodeScanner, "Scan")
                }
            },
            bottomBar = {
                // Total Count Footer
                Surface(
                    shadowElevation = 16.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Onboard", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                            Text("$totalPassengers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF266FEF))
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Count Synced!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF266FEF)),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Finish Updates")
                        }
                    }
                }
            },
            containerColor = Color(0xFFF9FAFB)
        ) { padding ->

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF266FEF))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(stops) { index, stop ->
                        PassengerStopCard(
                            stop = stop,
                            onAdd = { updateOccupancy(totalPassengers + 1) },
                            onRemove = { updateOccupancy(totalPassengers - 1) }
                        )
                    }
                    // Extra padding for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun PassengerStopCard(stop: Stop, onAdd: () -> Unit, onRemove: () -> Unit) {
    // Local counter for visual feedback per stop (optional)
    var stopCount by remember { mutableIntStateOf(0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Scheduled: ${stop.time}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onRemove(); stopCount-- },
                    modifier = Modifier.size(36.dp).background(Color(0xFFFFEBEE), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, null, tint = Color.Red)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Optional: Display stop-specific count if you track it
                // Text("$stopCount", fontWeight = FontWeight.Bold)
                // Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { onAdd(); stopCount++ },
                    modifier = Modifier.size(36.dp).background(Color(0xFFE8F5E9), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF00C853))
                }
            }
        }
    }
}