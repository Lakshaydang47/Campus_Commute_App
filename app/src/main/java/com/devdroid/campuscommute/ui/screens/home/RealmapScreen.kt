package com.devdroid.campuscommute.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devdroid.campuscommute.data.CamStop
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// --- 🛑 ESSENTIAL HELPER DATA CLASSES 🛑 ---

data class CurrentBusStop(
    val busId: String,
    val locationName: String,
    val lat: Double,
    val lng: Double
)

// NOTE: This must be defined or imported from your `data` package.
// I've included a placeholder structure for compilation.
data class CampusStop(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

val CAMPUS_STOP_POINTS = listOf(
    // --- Primary Access Points ---
    CamStop("main_gate", "Main Gate Entrance", "The security check point at the college entrance", 29.211358, 77.017815),
    CamStop("admin_bldg", "Admin Block", "Parking area near the Principal's office", 29.211107, 77.017321),

    // --- Academic/Departmental Buildings ---
    CamStop("cse_dept", "Staring", "Near the Computer Science Department building", 29.210777, 77.017313),
    CamStop("mech_bldg", "Old Canteen", "Drop-off point by the Engineering workshops", 29.210367, 77.017213),
    CamStop("lib_drop", "Near A Block", "Designated spot closest to the library entrance", 29.210363, 77.016562),

    // --- Residential/Hostel Areas ---
    CamStop("boys_hstl", "MBA Park", "Main parking and drop-off zone for Boys Hostel", 29.210369, 77.016231),
    CamStop("girls_hstl", "Outside D Block", "Designated stop outside the Girls Hostel complex", 29.210594, 77.016022),

    // --- Amenities ---
    CamStop("canteen", "APJ Auditorium", "Parking area near the main food service building", 29.210917, 77.016029),
    CamStop("sports", "E Block", "Near the stadium/indoor sports facility entrance", 29.211238, 77.016034),
    CamStop("parking_lot", "G Block", "The main general parking field", 29.211402, 77.016170),

    // --- Emergency/Specific Stops ---
    CamStop("dispensary", "B Block", "Near the medical dispensary building", 29.211398, 77.016544)
)

// --------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealMapScreen(userBusId: String) {
    val db = FirebaseFirestore.getInstance()
    var parkedLocation by remember { mutableStateOf<CurrentBusStop?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLocationsList by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.HYBRID) }

    // Default camera position (center of campus)
    val defaultPosition = LatLng(29.3520, 77.0135) // Center of your campus
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 17f)
    }

    // Firestore Listener
    DisposableEffect(userBusId) {
        isLoading = true
        val docRef = db.collection("bus_status").document(userBusId)
        var listener: ListenerRegistration? = null

        listener = docRef.addSnapshotListener { snapshot, e ->
            isLoading = false
            if (e != null) return@addSnapshotListener

            parkedLocation = if (snapshot != null && snapshot.exists()) {
                CurrentBusStop(
                    busId = snapshot.getString("busId") ?: userBusId,
                    locationName = snapshot.getString("locationName") ?: "Unknown",
                    lat = snapshot.getDouble("lat") ?: 0.0,
                    lng = snapshot.getDouble("lng") ?: 0.0
                )
            } else {
                null
            }
        }
        onDispose { listener?.remove() }
    }

    // Auto-zoom to bus location when available
    LaunchedEffect(parkedLocation) {
        parkedLocation?.let { location ->
            // Only auto-zoom if the coordinates are valid (not 0,0)
            if (location.lat != 0.0 && location.lng != 0.0) {
                val position = LatLng(location.lat, location.lng)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(position, 18f),
                    1000 // 1 second animation
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Bus $userBusId",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLoading && parkedLocation != null) {
                            Text(
                                "Live Tracking",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Toggle Map Type
                    IconButton(
                        onClick = {
                            mapType = when (mapType) {
                                MapType.HYBRID -> MapType.NORMAL
                                MapType.NORMAL -> MapType.SATELLITE
                                MapType.SATELLITE -> MapType.TERRAIN
                                else -> MapType.HYBRID
                            }
                        }
                    ) {
                        Icon(Icons.Default.Layers, "Change Map Type")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Center on Bus Location
                if (parkedLocation != null) {
                    FloatingActionButton(
                        onClick = {
                            parkedLocation?.let { location ->
                                // Only move if coordinates are valid
                                if (location.lat != 0.0 && location.lng != 0.0) {
                                    val position = LatLng(location.lat, location.lng)
                                    cameraPositionState.move(
                                        CameraUpdateFactory.newLatLngZoom(position, 18f)
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.MyLocation, "Center on Bus")
                    }
                }

                // Locations List Button
                SmallFloatingActionButton(
                    onClick = { showLocationsList = !showLocationsList },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        if (showLocationsList) Icons.Default.Close else Icons.Default.List,
                        "Show Locations"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = mapType,
                    isMyLocationEnabled = false,
                    isBuildingEnabled = true
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    zoomGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = true,
                    rotationGesturesEnabled = true,
                    compassEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // Campus Locations Markers (Static Stops - AZURE)
                CAMPUS_STOP_POINTS.forEach { stop ->
                    val isCurrentLocation = stop.name == parkedLocation?.locationName
                    val stopPosition = LatLng(stop.latitude, stop.longitude)

                    Marker(
                        state = MarkerState(position = stopPosition),
                        title = stop.name,
                        snippet = stop.description,
                        // Static stops are Azure Blue
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        alpha = if (isCurrentLocation) 1f else 0.7f,
                        zIndex = if (isCurrentLocation) 1f else 0f
                    )
                }

                // Bus Location Marker (LIVE - GREEN)
                parkedLocation?.let { location ->
                    // Only show bus marker if coordinates are valid
                    if (location.lat != 0.0 && location.lng != 0.0) {
                        val busPosition = LatLng(location.lat, location.lng)
                        Marker(
                            state = MarkerState(position = busPosition),
                            title = "Bus $userBusId - LIVE",
                            snippet = location.locationName,
                            // Live bus is distinct GREEN
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                            zIndex = 2f // Bus marker is always on top layer
                        )
                    }
                }
            }

            // Status Card at Top
            AnimatedVisibility(
                visible = !isLoading || parkedLocation != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                "Locating bus...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Current Location",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    parkedLocation?.locationName ?: "Offline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Map Type Indicator
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        when (mapType) {
                            MapType.HYBRID -> "Satellite View"
                            MapType.SATELLITE -> "Satellite Only"
                            MapType.TERRAIN -> "Terrain View"
                            else -> "Normal View"
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Locations List Drawer
            AnimatedVisibility(
                visible = showLocationsList,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Campus Stops",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showLocationsList = false }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        CAMPUS_STOP_POINTS.forEach { stop ->
                            val isCurrentLocation = stop.name == parkedLocation?.locationName

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentLocation)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                onClick = {
                                    // Zoom to selected location
                                    cameraPositionState.move(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(stop.latitude, stop.longitude),
                                            18f
                                        )
                                    )
                                    showLocationsList = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (isCurrentLocation) Icons.Default.GpsFixed else Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (isCurrentLocation)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stop.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrentLocation) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            stop.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}