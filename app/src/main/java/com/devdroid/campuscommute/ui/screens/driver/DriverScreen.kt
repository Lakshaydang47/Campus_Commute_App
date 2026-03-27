package com.devdroid.campuscommute.ui.screens.driver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

// ----------------------------------------
// 1. DRIVER BOTTOM NAVIGATION BAR
// ----------------------------------------

sealed class DriverBottomItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Dashboard : DriverBottomItem("driver_dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "Home")
    object Map : DriverBottomItem("tracker", Icons.Filled.Map, Icons.Outlined.Map, "Route")
    // ⚡ NEW ITEM ADDED ⚡
    object Location : DriverBottomItem("driver_set_location", Icons.Filled.LocationOn, Icons.Outlined.LocationOn, "Location")
    object Passengers : DriverBottomItem("passenger_count", Icons.Filled.Groups, Icons.Outlined.Groups, "Students")
    object Profile : DriverBottomItem("driver_profile", Icons.Filled.Person, Icons.Outlined.Person, "Profile")
}

@Composable
fun DriverBottomBar(
    navController: NavController,
    driverBusId: String, // 👈 MODIFIED: Requires the driver's current Bus ID
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DriverBottomItem.Dashboard,
        DriverBottomItem.Map,
        DriverBottomItem.Location, // 👈 ADDED to the list
        DriverBottomItem.Passengers,
        DriverBottomItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                // Check if the current route starts with the item's route (useful for parameterized routes)
                val isSelected = currentRoute == item.route ||
                        (item == DriverBottomItem.Location && currentRoute?.startsWith(item.route) == true)

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF266FEF) else Color(0xFF9CA3AF),
                    animationSpec = tween(300), label = "color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute != item.route) {

                                // ⚡ MODIFIED NAVIGATION LOGIC ⚡
                                val targetRoute = when (item) {
                                    DriverBottomItem.Location -> "${item.route}/$driverBusId" // Pass the required Bus ID
                                    else -> item.route // Standard routes
                                }

                                navController.navigate(targetRoute) {
                                    popUpTo("driver") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------
// 2. MAIN DRIVER SCREEN
// ----------------------------------------

@Composable
fun DriverScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // State
    var isSharing by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf(false) }

    // ⚡ DIRECT FIREBASE FETCH STATE
    var busId by remember { mutableStateOf("Loading...") }
    var driverName by remember { mutableStateOf("Driver") }

    // 1. Fetch Driver Details from Firestore
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        driverName = document.getString("name") ?: "Driver"
                        // Fetch Bus ID safely (handles Number or String types)
                        busId = document.get("busId")?.toString() ?: "Unknown"
                    } else {
                        busId = "Unknown"
                    }
                }
                .addOnFailureListener {
                    busId = "Error"
                    Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 2. Check Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    Scaffold(
        bottomBar = { DriverBottomBar(navController = navController,busId) },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $driverName 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Bus No: $busId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = {
                    auth.signOut()
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- Status Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isSharing)
                                Brush.horizontalGradient(listOf(Color(0xFF00C853), Color(0xFF69F0AE))) // Green
                            else
                                Brush.horizontalGradient(listOf(Color(0xFFD32F2F), Color(0xFFFF5252))) // Red
                        )
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSharing) Icons.Filled.SatelliteAlt else Icons.Filled.LocationOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isSharing) "YOU ARE ONLINE" else "YOU ARE OFFLINE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isSharing) "Sharing Location..." else "Tracking is disabled",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- Action Button (START / STOP) ---
            Button(
                onClick = {
                    if (busId == "Loading..." || busId == "Unknown" || busId == "Error") {
                        Toast.makeText(context, "Please wait for Bus details to load", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (!isSharing) {
                        // START SERVICE
                        val intent = Intent(context, LocationService::class.java).apply {
                            action = "ACTION_START"
                            putExtra("BUS_ID", busId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isSharing = true
                        Toast.makeText(context, "Trip Started 🚀", Toast.LENGTH_SHORT).show()
                    } else {
                        // STOP SERVICE
                        val intent = Intent(context, LocationService::class.java).apply {
                            action = "ACTION_STOP"
                        }
                        context.startService(intent)
                        isSharing = false
                        Toast.makeText(context, "Trip Ended 🏁", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(160.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (!isSharing) Icons.Filled.PlayArrow else Icons.Filled.Stop,
                        contentDescription = "Toggle",
                        tint = if (!isSharing) Color(0xFF266FEF) else Color.Red,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!isSharing) "START TRIP" else "END TRIP",
                        color = if (!isSharing) Color(0xFF266FEF) else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Report Delay Button ---
            OutlinedButton(
                onClick = { showDelayDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
            ) {
                Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report Traffic / Delay")
            }
        }

        // --- ALERT DIALOG ---
        if (showDelayDialog) {
            AlertDialog(
                onDismissRequest = { showDelayDialog = false },
                title = { Text("Report Delay") },
                text = { Text("Notify students that Bus $busId is running 10-15 mins late?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val ref = FirebaseDatabase.getInstance().getReference("notifications").push()
                            val alert = mapOf(
                                "id" to ref.key,
                                "title" to "Bus Delay ⚠️",
                                "message" to "Bus $busId is running late due to traffic.",
                                "timestamp" to System.currentTimeMillis(),
                                "type" to "delay",
                                "busId" to busId
                            )
                            ref.setValue(alert)
                            showDelayDialog = false
                            Toast.makeText(context, "Alert Sent!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Send Alert")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDelayDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}