package com.devdroid.campuscommute.ui.screens.tracker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdroid.campuscommute.data.Stop
import com.devdroid.campuscommute.data.UserSession
import com.devdroid.campuscommute.ui.components.DriverInfoSection
import com.devdroid.campuscommute.utils.haversineDistanceMeters
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper class for Firestore deserialization
data class RouteWrapper(val stops: List<Stop> = emptyList())

@Composable
fun TrackerScreen(
    busId: String,
    onSeeRouteClick: (Int) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val realtimeDb = remember { FirebaseDatabase.getInstance().getReference("locations/$busId") }
    val firestoreDb = remember { FirebaseFirestore.getInstance() }

    // --- State Variables ---
    var routeStops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isRouteLoading by remember { mutableStateOf(true) }

    var refreshKey by remember { mutableIntStateOf(0) }

    var busLocation by remember { mutableStateOf<LatLng?>(null) }
    var lastUpdateTxt by remember { mutableStateOf("Connecting...") }
    var driverStatus by remember { mutableStateOf("Checking...") }
    var currentSpeed by remember { mutableStateOf(0f) }

    // Default to 0 initially
    var currentStopIndex by remember { mutableIntStateOf(0) }

    // Determine Logic States
    val isOffline = driverStatus == "Offline" || busLocation == null

    // "Finished" means we have passed the very last stop
    val isFinished = routeStops.isNotEmpty() && currentStopIndex >= routeStops.size

    val mapProperties = remember { MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = false) }
    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = false, compassEnabled = true) }
    val cameraPositionState = rememberCameraPositionState()

    val checkLiveStatus: () -> Unit = remember {
        {
            refreshKey++
            busLocation = null
            driverStatus = "Checking..."
            lastUpdateTxt = "Connecting..."
            if (routeStops.isEmpty()) {
                isRouteLoading = true
            }
        }
    }

    // 1. FETCH ROUTE
    LaunchedEffect(busId) {
        isRouteLoading = true
        firestoreDb.collection("routes").document(busId).get()
            .addOnSuccessListener { doc ->
                try {
                    if (doc.exists()) {
                        val wrapper = doc.toObject(RouteWrapper::class.java)
                        val stops = wrapper?.stops?.sortedBy { it.sequence } ?: emptyList()
                        routeStops = stops
                        UserSession.cachedRouteStops = stops

                        val firstLocation = stops.getOrNull(0)?.location
                        if (firstLocation != null) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(firstLocation, 14f)
                        }
                    } else {
                        routeStops = emptyList()
                    }
                } catch (t: Throwable) {
                    Log.w("TrackerScreen", "Failed to parse route doc", t)
                    routeStops = emptyList()
                } finally {
                    isRouteLoading = false
                }
            }
            .addOnFailureListener { ex ->
                Log.w("TrackerScreen", "Failed to fetch route", ex)
                isRouteLoading = false
            }
    }

    // 2. LISTEN TO LIVE LOCATION
    DisposableEffect(busId, refreshKey) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val lat = snapshot.child("lat").getValue(Double::class.java)
                        ?: snapshot.child("lat").getValue(Float::class.java)?.toDouble()
                    val lng = snapshot.child("lng").getValue(Double::class.java)
                        ?: snapshot.child("lng").getValue(Float::class.java)?.toDouble()

                    var tsLong: Long? = snapshot.child("timestamp").getValue(Long::class.java)
                    if (tsLong == null) {
                        val tsDouble = snapshot.child("timestamp").getValue(Double::class.java)
                        if (tsDouble != null) tsLong = tsDouble.toLong()
                    }
                    if (tsLong != null && tsLong < 1000000000000L) {
                        tsLong *= 1000
                    }

                    val speedVal = snapshot.child("speed").getValue(Double::class.java)
                        ?: snapshot.child("speed").getValue(Float::class.java)?.toDouble()
                        ?: snapshot.child("speed").getValue(Long::class.java)?.toDouble()
                    val speed = speedVal?.toFloat() ?: 0f

                    if (lat != null && lng != null && tsLong != null) {
                        val newLoc = LatLng(lat, lng)
                        busLocation = newLoc
                        currentSpeed = speed

                        val timeDiff = System.currentTimeMillis() - tsLong
                        driverStatus = when {
                            timeDiff > 60000 * 5 -> "Offline"
                            speed < 1.0 -> "Stopped"
                            else -> "Moving"
                        }
                        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                        lastUpdateTxt = "Live: ${sdf.format(Date(tsLong))}"
                    } else {
                        driverStatus = "Offline"
                        lastUpdateTxt = "Offline"
                    }
                } catch (t: Throwable) {
                    Log.w("TrackerScreen", "RTDB value parsing error", t)
                    driverStatus = "Offline"
                    lastUpdateTxt = "Offline"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TrackerScreen", "Realtime DB listener cancelled: ${error.message}")
            }
        }
        realtimeDb.addValueEventListener(listener)
        onDispose { realtimeDb.removeEventListener(listener) }
    }

    // 3. LOGIC: Camera & Smart Stop Detection
    LaunchedEffect(busLocation, routeStops) {
        busLocation?.let { busLoc ->
            // Animate Camera
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(busLoc, 16f))
            } catch (t: Throwable) { /* Ignore animation error */ }

            if (routeStops.isNotEmpty()) {
                // Find nearest stop that is AHEAD of us (or the one we are currently at)
                // We use >= currentStopIndex so we don't jump backward to start.
                var minDistance = Float.MAX_VALUE
                var closestIndex = currentStopIndex

                routeStops.forEachIndexed { index, stop ->
                    // Only consider stops we haven't logically fully passed yet
                    if (index >= currentStopIndex) {
                        val distance = haversineDistanceMeters(
                            busLoc.latitude, busLoc.longitude,
                            stop.latitude, stop.longitude
                        )
                        // If this stop is closer than any other future stop, it's our target
                        if (distance < minDistance) {
                            minDistance = distance.toFloat()
                            closestIndex = index
                        }
                    }
                }

                // If we are extremely close (< 20m) to the last stop, mark finished?
                // For now, we just update the index to the nearest one.
                currentStopIndex = closestIndex
            }
        }
    }

    // 4. UI Layout
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

            if (isRouteLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF266FEF))
                }
            } else {

                // --- STATE SWITCHER ---
                if (!isOffline && !isFinished) {
                    // Show Map only if Active and Not Finished
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = mapUiSettings
                    ) {
                        if (routeStops.isNotEmpty()) {
                            // Polyline
                            val polyPoints = routeStops.mapNotNull { it.location }
                            if (polyPoints.size >= 2) {
                                Polyline(points = polyPoints, color = Color(0xFF266FEF), width = 12f)
                            }

                            // Markers Loop
                            routeStops.forEachIndexed { index, stop ->
                                val stopLocation = stop.location
                                if (stopLocation != null) {

                                    // LOGIC: COLOR BASED ON STATUS
                                    val (markerHue, stateTitle) = when {
                                        index < currentStopIndex -> {
                                            // 1. Reached/Passed
                                            Pair(BitmapDescriptorFactory.HUE_GREEN, "Reached")
                                        }
                                        index == currentStopIndex -> {
                                            // 2. Nearest/Arriving (Active)
                                            Pair(BitmapDescriptorFactory.HUE_AZURE, "Arriving Here")
                                        }
                                        else -> {
                                            // 3. Future
                                            Pair(BitmapDescriptorFactory.HUE_ORANGE, "Pending")
                                        }
                                    }

                                    Marker(
                                        state = MarkerState(stopLocation),
                                        title = "${stop.name} ($stateTitle)",
                                        snippet = if (index == currentStopIndex) "Bus is nearby" else null,
                                        icon = BitmapDescriptorFactory.defaultMarker(markerHue)
                                    )
                                }
                            }
                        }

                        busLocation?.let { loc ->
                            Marker(
                                state = MarkerState(loc),
                                title = "Bus $busId",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }
                } else {
                    // --- STATUS OVERLAYS ---
                    if (isOffline) {
                        BusOfflineScreen(onRefresh = checkLiveStatus)
                    } else if (isFinished) {
                        BusReachedScreen(destination = routeStops.lastOrNull()?.name ?: "Destination", onRefresh = checkLiveStatus)
                    }
                }
            }

            // --- TOP BAR ---
            Column(modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallFloatingActionButton(onClick = onBackClick, containerColor = Color.White) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    if (!isOffline && !isFinished) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dotColor = if (driverStatus.contains("Moving", ignoreCase = true)) Color.Green else Color.Red
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = driverStatus, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- BOTTOM CARD ---
            Box(modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
            ) {
                // Determine text: Use CURRENT index as the target
                val nextStopName = if (isFinished) "Route Completed"
                else routeStops.getOrNull(currentStopIndex)?.name ?: "Destination"

                TrackerBottomCard(
                    nextStopName = nextStopName,
                    lastUpdate = if (isOffline) "Offline" else lastUpdateTxt,
                    speed = currentSpeed,
                    onSeeRouteClick = { onSeeRouteClick(currentStopIndex) }
                )
            }
        }
    }
}



@Composable
fun BusStatusView(icon: ImageVector, title: String, subtitle: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(60.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, fontSize = 16.sp, color = Color.Gray)
    }
}

// --- Bottom Card Component ---
@Composable
fun TrackerBottomCard(
    nextStopName: String,
    lastUpdate: String,
    speed: Float,
    onSeeRouteClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // HEADER (Always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(bottom = if (isExpanded) 16.dp else 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        // If it's the current stop index, we are arriving THERE
                        text = "ARRIVING AT",
                        style = MaterialTheme.typography.labelSmall, color = Color.Gray
                    )
                    Text(nextStopName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Toggle",
                    tint = Color(0xFF266FEF),
                    modifier = Modifier.size(32.dp)
                )
            }

            // EXPANDED CONTENT
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                // Driver Info Section (Reused Component)
                DriverInfoSection()

                Divider(color = Color.LightGray.copy(alpha = 0.3f))
            }

            // FOOTER (Always visible)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = if (!isExpanded) 16.dp else 0.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(lastUpdate, fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.width(16.dp))
                if (speed > 1) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${speed.toInt()} km/h", fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onSeeRouteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF266FEF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Route", fontSize = 12.sp)
                }
            }
        }
    }
}