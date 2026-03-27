package com.devdroid.campuscommute.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.Stop
import com.google.firebase.auth.FirebaseAuth // 👈 Added Auth import
import com.google.firebase.firestore.FirebaseFirestore

// Helper for reading from Firestore
data class RouteWrapper(val stops: List<Stop> = emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRouteScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance() // 👈 Added Auth instance

    // --- State ---
    var busId by remember { mutableStateOf("") } // 👈 Starts empty
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) } // New state for error handling

    // Function to handle fetching the route once busId is known
    val fetchRoute = { fetchedBusId: String ->
        db.collection("routes").document(fetchedBusId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val wrapper = doc.toObject(RouteWrapper::class.java)
                    stops = wrapper?.stops?.sortedBy { it.sequence } ?: emptyList()
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                loadError = true
                Toast.makeText(context, "Failed to load route", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 1. Fetch Admin's Bus ID & Route ---
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // Step A: Get Admin Profile to find Bus ID
            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    val fetchedId = userDoc.get("busId")?.toString()

                    if (!fetchedId.isNullOrEmpty()) {
                        busId = fetchedId
                        // Step B: Fetch Existing Route
                        fetchRoute(fetchedId)
                    } else {
                        isLoading = false
                        loadError = true
                        Toast.makeText(context, "Error: No Bus Assigned to you", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    loadError = true
                    Toast.makeText(context, "Failed to fetch profile", Toast.LENGTH_SHORT).show()
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val titleText = if (busId.isNotEmpty()) "Edit Route (Bus $busId)" else "Loading Route..."
                    Text(titleText, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // SAVE BUTTON
                    TextButton(
                        onClick = {
                            if (busId.isNotEmpty()) {
                                isSaving = true
                                saveRouteToFirebase(db, busId, stops) { success ->
                                    isSaving = false
                                    if (success) Toast.makeText(context, "Route Saved Successfully! ✅", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "Save Failed ❌", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Cannot save: No Bus ID", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSaving && busId.isNotEmpty() && !isLoading
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("SAVE", fontWeight = FontWeight.Bold, color = Color(0xFF266FEF))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (busId.isNotEmpty() && !isLoading) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF266FEF),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
        }
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF266FEF))
            }
        } else if (loadError || busId.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Could not determine assigned bus.", color = Color.Red)
            }
        } else if (stops.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No stops defined for Bus $busId. Tap + to start.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(stops) { index, stop ->
                    StopItemCard(
                        stop = stop,
                        index = index + 1,
                        onDelete = {
                            val mutable = stops.toMutableList()
                            mutable.removeAt(index)
                            stops = mutable.mapIndexed { i, s -> s.copy(sequence = i + 1) }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // --- Dialogs ---
    if (showAddDialog) {
        AddStopDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newStop ->
                val nextSeq = stops.size + 1
                val stopWithSeq = newStop.copy(sequence = nextSeq)
                stops = stops + stopWithSeq
                showAddDialog = false
            }
        )
    }
}

// --- Logic Helper ---

fun saveRouteToFirebase(db: FirebaseFirestore, busId: String, stops: List<Stop>, onResult: (Boolean) -> Unit) {
    val data = mapOf(
        "busId" to busId,
        "stops" to stops,
        "lastUpdated" to System.currentTimeMillis()
    )

    db.collection("routes").document(busId).set(data)
        .addOnSuccessListener { onResult(true) }
        .addOnFailureListener { onResult(false) }
}

// --- UI Components ---

@Composable
fun StopItemCard(stop: Stop, index: Int, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number Badge
            Box(
                modifier = Modifier.size(32.dp).background(Color(0xFFE3F2FD), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(index.toString(), fontWeight = FontWeight.Bold, color = Color(0xFF266FEF))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Lat: ${stop.latitude}, Lng: ${stop.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Scheduled: ${stop.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00C853)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddStopDialog(onDismiss: () -> Unit, onAdd: (Stop) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Stop") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Stop Name (e.g. Karnal Gate)") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = lng,
                        onValueChange = { lng = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 08:30 AM)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && lat.isNotBlank() && lng.isNotBlank()) {
                        onAdd(
                            Stop(
                                id = System.currentTimeMillis().toString(),
                                name = name,
                                latitude = lat.toDoubleOrNull() ?: 0.0,
                                longitude = lng.toDoubleOrNull() ?: 0.0,
                                time = time
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF266FEF))
            ) {
                Text("Add Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}