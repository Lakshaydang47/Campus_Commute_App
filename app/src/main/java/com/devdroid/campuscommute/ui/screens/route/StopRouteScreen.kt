package com.devdroid.campuscommute.ui.screens.route

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdroid.campuscommute.data.Stop
import com.google.firebase.firestore.FirebaseFirestore

// Helper for deserialization
data class RouteWrapper(val stops: List<Stop> = emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopRouteScreen(
    busId: String,
    currentStopIndex: Int = 0, // Provided by TrackerScreen (5m logic applied there)
    onBackClick: () -> Unit = {}
) {
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Logic
    LaunchedEffect(busId) {
        if (busId.isNotEmpty() && busId != "N/A") {
            FirebaseFirestore.getInstance().collection("routes").document(busId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val wrapper = doc.toObject(RouteWrapper::class.java)
                        stops = wrapper?.stops?.sortedBy { it.sequence } ?: emptyList()
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Route Timeline", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Bus $busId", fontSize = 13.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF1A1A1A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF266FEF))
            }
        } else if (stops.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No route details found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize()
                    .background(Color(0xFFF8F9FA))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Progress Summary Card
                item {
                    // Logic: If current index is 2, we are AT stop 2 (3rd stop).
                    // Next stop is index + 1.
                    val nextStopName = stops.getOrNull(currentStopIndex + 1)?.name ?: "End of Route"

                    ProgressSummaryCard(
                        currentStop = currentStopIndex + 1, // 0-based to 1-based
                        totalStops = stops.size,
                        nextStopName = nextStopName
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                itemsIndexed(stops) { index, stop ->
                    // ⚡ REFINED LOGIC
                    val isArrived = index == currentStopIndex // Currently AT this stop (within 5m)
                    val isPassed = index < currentStopIndex   // Already passed this stop
                    val isNext = index == currentStopIndex + 1 // The immediate next destination
                    val isLast = index == stops.lastIndex

                    AnimatedStopItem(
                        stop = stop,
                        isPassed = isPassed,
                        isArrived = isArrived,
                        isNext = isNext,
                        isLast = isLast,
                        stopNumber = index + 1
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressSummaryCard(currentStop: Int, totalStops: Int, nextStopName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Progress", fontSize = 14.sp, color = Color.Gray)
                    Text("$currentStop of $totalStops stops", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.size(60.dp).background(
                        Brush.linearGradient(listOf(Color(0xFF266FEF), Color(0xFF4A90FF))), CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${(currentStop * 100 / totalStops)}%", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = currentStop.toFloat() / totalStops,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF266FEF),
                trackColor = Color(0xFFE3F2FD)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NearMe, null, modifier = Modifier.size(16.dp), tint = Color(0xFF266FEF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Next: $nextStopName", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AnimatedStopItem(
    stop: Stop,
    isPassed: Boolean,
    isArrived: Boolean,
    isNext: Boolean,
    isLast: Boolean,
    stopNumber: Int
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isArrived) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Row(modifier = Modifier.fillMaxWidth().scale(animatedScale)) {
        // Timeline Column
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            // Stop Circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = when {
                            isArrived -> Color(0xFF266FEF) // Current Location (Blue)
                            isPassed -> Color(0xFF4CAF50)  // Passed (Green)
                            isNext -> Color(0xFFFF9800)    // Next (Orange)
                            else -> Color(0xFFE0E0E0)      // Future (Gray)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isPassed -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    isArrived -> Icon(Icons.Default.DirectionsBus, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    isNext -> Text("$stopNumber", color = Color.White, fontWeight = FontWeight.Bold) // Next stop gets number
                    else -> Text("$stopNumber", color = Color.White, fontSize = 12.sp)
                }
            }

            // Connecting Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(70.dp)
                        .background(
                            if (isPassed) Color(0xFF4CAF50) // Green line for passed segments
                            else if (isArrived) Color(0xFF266FEF).copy(alpha = 0.5f) // Fading blue for current segment
                            else Color(0xFFE0E0E0)
                        )
                )
            }
        }

        // Stop Details Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, bottom = if (!isLast) 8.dp else 0.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isArrived -> Color(0xFFE3F2FD) // Light Blue Highlight
                    isNext -> Color(0xFFFFF3E0)    // Light Orange Highlight
                    isPassed -> Color(0xFFF1F8F4)  // Light Green
                    else -> Color.White
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isArrived) 4.dp else 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stop.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stop.time.ifEmpty { "--:--" }, fontSize = 13.sp, color = Color.Gray)
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isArrived -> Color(0xFF266FEF)
                        isPassed -> Color(0xFF4CAF50)
                        isNext -> Color(0xFFFF9800)
                        else -> Color(0xFFE0E0E0)
                    }
                ) {
                    Text(
                        text = when {
                            isArrived -> "Arrived"
                            isPassed -> "Passed"
                            isNext -> "Next Stop"
                            else -> "Upcoming"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isArrived || isPassed || isNext) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}