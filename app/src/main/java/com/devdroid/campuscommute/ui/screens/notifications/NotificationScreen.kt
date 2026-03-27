package com.devdroid.campuscommute.ui.screens.notifications

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
// ... (All other imports remain the same)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
// ... (Other imports)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
// ... (Other imports)
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
// ... (Firebase imports)
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

// --------------------------------------------------------------------------
// Helper Data Classes (Unchanged)
// --------------------------------------------------------------------------

data class CurrentBusStop(
    val busId: String,
    val locationName: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double
)

// --------------------------------------------------------------------------
// Notification Screen Composable (MODIFIED)
// --------------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    val dbRef = FirebaseDatabase.getInstance().getReference("notifications")
    val firestore = FirebaseFirestore.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 1. LOCAL MEMORY SIMULATION (Cache for historical announcements)
    val localNotifications = remember { mutableStateListOf<NotificationModel>() }
    var isLoading by remember { mutableStateOf(true) }
    var userBusId by remember { mutableStateOf<String?>(null) }

    // ⭐ NEW STATE: Tracks the timestamp of the last time user clicked "Mark All As Read"
    var lastReadTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // 2. REAL-TIME STATE FOR CURRENT BUS LOCATION
    var currentBusStatus by remember { mutableStateOf<CurrentBusStop?>(null) }
    var lastUpdateTimestamp by remember { mutableStateOf(0L) }

    // --- Data Fetching Logic (Unchanged) ---
    // A. Initial Load: Fetch User ID and Historical Announcements
    LaunchedEffect(Unit) {
        // ... (Existing LaunchedEffect logic remains the same)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                // Fetch user bus ID
                val doc = firestore.collection("users").document(uid).get().await()
                userBusId = doc.get("busId")?.toString()

                // Fetch historical announcements ONCE
                val snapshot = dbRef.get().await()
                val list = mutableListOf<NotificationModel>()
                for (child in snapshot.children) {
                    val item = child.getValue(NotificationModel::class.java)
                    // Filter: ALL messages + messages specific to the user's busId
                    if (item != null && (item.busId == "ALL" || item.busId == userBusId)) {
                        list.add(item)
                    }
                }
                localNotifications.addAll(list.sortedByDescending { it.timestamp })

            } catch (e: Exception) {
                // Handle errors
            } finally {
                isLoading = false
            }
        }
    }

    // B. Real-Time Listener for Current Bus Status (Unchanged)
    DisposableEffect(userBusId) {
        // ... (Existing DisposableEffect logic remains the same)
        if (userBusId.isNullOrEmpty()) return@DisposableEffect onDispose {}

        val busId = userBusId!!
        val docRef = firestore.collection("bus_status").document(busId)
        var listener: ListenerRegistration? = null

        listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val newStatus = CurrentBusStop(
                busId = snapshot.getString("busId") ?: busId,
                locationName = snapshot.getString("locationName") ?: "Bus is not in campus",
                timestamp = snapshot.getLong("timestamp") ?: 0L,
                lat = snapshot.getDouble("lat") ?: 0.0,
                lng = snapshot.getDouble("lng") ?: 0.0
            )

            if (newStatus.timestamp > lastUpdateTimestamp) {
                lastUpdateTimestamp = newStatus.timestamp
                currentBusStatus = newStatus

                scope.launch {
                    val location = newStatus.locationName
                    val message = if (location == "Bus is not in campus") {
                        "Bus status updated: Not currently parked on campus."
                    } else {
                        "New location set: ${newStatus.locationName}!"
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "VIEW",
                        duration = SnackbarDuration.Short
                    )
                }
            } else if (currentBusStatus == null) {
                currentBusStatus = newStatus
                lastUpdateTimestamp = newStatus.timestamp
            }
        }
        onDispose { listener?.remove() }
    }

    // --- UI and Deletion Logic ---

    val deleteNotification: (NotificationModel) -> Unit = { notifToDelete ->
        localNotifications.remove(notifToDelete)
    }

    val finalDisplayList = remember(localNotifications, currentBusStatus) {
        val list = mutableListOf<Any>()
        currentBusStatus?.let { status ->
            list.add(status)
        }
        list.addAll(localNotifications)
        list.distinctBy {
            when (it) {
                is NotificationModel -> it.id
                is CurrentBusStop -> "STATUS_CARD"
                else -> Any()
            }
        }
    }

    // ⭐ NEW LOGIC: Calculate unread count and Define Mark All action
    val unreadCount = remember(localNotifications, lastReadTimestamp) {
        localNotifications.count { it.timestamp > lastReadTimestamp }
    }

    val markAllAsRead: () -> Unit = {
        lastReadTimestamp = System.currentTimeMillis()
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "All ${localNotifications.size} announcements marked as read.",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        // ⭐ NEW UI: Unread Count Badge
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                containerColor = Color(0xFFC62828),
                                modifier = Modifier.size(24.dp),
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                // ⭐ NEW UI: Mark All As Read Button
                actions = {
                    if (localNotifications.isNotEmpty()) {
                        TextButton(onClick = markAllAsRead) {
                            Text("Mark All Read", color = Color(0xFF266FEF))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        if (isLoading && finalDisplayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF266FEF))
            }
        } else if (finalDisplayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No updates yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(finalDisplayList, key = { _, item ->
                    when (item) {
                        is NotificationModel -> item.id
                        is CurrentBusStop -> "STATUS_CARD"
                        else -> Any()
                    }
                }) { _, item ->
                    when (item) {
                        is CurrentBusStop -> {
                            // ⭐ MODIFIED: Pass lastUpdateTimestamp for time elapsed calculation
                            CurrentStatusNotificationCard(
                                status = item,
                                lastReadTimestamp = lastReadTimestamp
                            )
                        }
                        is NotificationModel -> {
                            // ⭐ MODIFIED: Pass lastReadTimestamp to highlight unread messages
                            NotificationItem(
                                item = item,
                                onDelete = { deleteNotification(item) },
                                isUnread = item.timestamp > lastReadTimestamp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. Historical Notification Item (MODIFIED)
// ----------------------------------------------------

@Composable
fun NotificationItem(
    item: NotificationModel,
    onDelete: () -> Unit,
    isUnread: Boolean // ⭐ NEW PARAMETER
) {
    val (icon, color, bg) = when (item.type) {
        "delay" -> Triple(Icons.Default.Warning, Color(0xFFC62828), Color(0xFFFFEBEE))
        "info" -> Triple(Icons.Default.Info, Color(0xFF1565C0), Color(0xFFE3F2FD))
        else -> Triple(Icons.Default.Notifications, Color(0xFFE65100), Color(0xFFFFF3E0))
    }

    val dateStr = try {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
    } catch (e: Exception) { "" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        // ⭐ NEW: Highlight the card if it's unread
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) Color(0xFFE8F5E9) else Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 4.dp else 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge (Unchanged)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content Area (Unchanged)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isUnread) Color.Black else Color.Black.copy(alpha = 0.85f) // ⭐ Make title bolder if unread
                    )
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))

                Text(
                    item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )

                if (item.busId != "ALL") {
                    Text(
                        "Target: Bus #${item.busId}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // DELETE BUTTON (Re-inserted as requested to maintain the original structure without swipe)
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = Color.Gray
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3. Current Status Notification Card (MODIFIED)
// ----------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CurrentStatusNotificationCard(status: CurrentBusStop, lastReadTimestamp: Long) {
    // ⭐ NEW LOGIC: Calculate time elapsed since update
    val timeElapsed = remember(status.timestamp) {
        val now = System.currentTimeMillis()
        val difference = now - status.timestamp
        formatTimeElapsed(difference)
    }

    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(status.timestamp))
    val isStatusSet = status.locationName != "Bus is not in campus"
    // ⭐ NEW: Mark as unread if the status update is newer than the last time the user read notifications
    val isNewUpdate = status.timestamp > lastReadTimestamp && status.timestamp > (System.currentTimeMillis() - 600000) // Only consider if within last 10 minutes

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // ⭐ NEW: Use a highlight color if it's a new update
            containerColor = when {
                isNewUpdate -> Color(0xFFE3F2FD) // Light Blue highlight for recent status
                isStatusSet -> Color(0xFFF9FBE7)
                else -> Color(0xFFFFEBEE)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNewUpdate) 8.dp else 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge (Unchanged)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isStatusSet) Color(0xFFDCEDC8) else Color(0xFFFFCDD2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isStatusSet) Icons.Default.LocationOn else Icons.Default.Cancel,
                    null,
                    tint = if (isStatusSet) Color(0xFF558B2F) else Color(0xFFC62828),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content Area
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Current Bus Parking Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.Black.copy(alpha = 0.85f)
                    )
                    // ⭐ NEW: Time Elapsed Display
                    Text(
                        timeElapsed,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    status.locationName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isStatusSet) Color(0xFF558B2F) else Color(0xFFC62828)
                )

                Text(
                    "Reported at: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// 4. Time Formatting Helper Function (NEW)
// ----------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
fun formatTimeElapsed(milliseconds: Long): String {
    val duration = milliseconds.milliseconds.toJavaDuration()
    val seconds = duration.seconds

    return when {
        seconds < 60 -> "Just now"
        seconds < 3600 -> {
            val minutes = seconds / 60
            "$minutes min${if (minutes > 1) "s" else ""} ago"
        }
        seconds < 86400 -> {
            val hours = seconds / 3600
            "$hours hr${if (hours > 1) "s" else ""} ago"
        }
        else -> {
            val days = seconds / 86400
            "$days day${if (days > 1) "s" else ""} ago"
        }
    }
}