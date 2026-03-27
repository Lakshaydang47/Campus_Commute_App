package com.devdroid.campuscommute.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // Required for Firestore await

// --- Data Class (Updated to include linked student info) ---
data class PendingUser(
    val userId: String,
    val name: String,
    val role: String,
    val email: String,
    val linkedStudentRollNo: String? = null,
    val studentName: String? = null,        // 👈 For Display
    val studentDepartment: String? = null    // 👈 For Display
)

// ----------------------------------------
// 1. ADMIN BOTTOM NAVIGATION BAR
// ----------------------------------------

sealed class AdminBottomItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Dashboard : AdminBottomItem("admin", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "Home")
    object Bus : AdminBottomItem("manage_route", Icons.Filled.DirectionsBus, Icons.Outlined.DirectionsBus, "My Bus")
    object Tracking: AdminBottomItem("admin_tracker_redirect", Icons.Filled.Route, Icons.Outlined.Route, "Tracking")

    object Requests : AdminBottomItem("user_management", Icons.Filled.PersonAdd, Icons.Outlined.PersonAdd, "Requests")
    object Profile : AdminBottomItem("admin_profile", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "Profile")
}

@Composable
fun AdminBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        AdminBottomItem.Dashboard,
        AdminBottomItem.Bus,
        AdminBottomItem.Tracking,
        AdminBottomItem.Requests,
        AdminBottomItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = modifier.
            navigationBarsPadding()
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
                val isSelected = currentRoute == item.route

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
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
// 2. MAIN ADMIN DASHBOARD (Logic Refactored for Coroutines)
// ----------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    // --- YOUR EXACT LOGIC & STATE ---
    var pendingUsers by remember { mutableStateOf<List<PendingUser>>(emptyList()) }
    var adminBusId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var activeUsersCount by remember { mutableStateOf("-") } // For Active Users Stat

    // --- Primary Data Fetch (Runs once on composition) ---
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val busId = doc.getLong("busId")?.toInt()
                adminBusId = busId

                if (busId != null) {
                    // Fetch all required data concurrently
                    val usersResult = fetchAllUsersData(db, busId)
                    pendingUsers = usersResult.first
                    activeUsersCount = usersResult.second.toString()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading dashboard.", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // --- IMPROVED UI STRUCTURE ---
    Scaffold(
        bottomBar = { AdminBottomBar(navController = navController) },
        containerColor = Color(0xFFF5F7FA) // Light Grey Background
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF266FEF))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                // --- Header ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Hello, Admin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Managing Bus #${adminBusId ?: "..."}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    IconButton(onClick = {
                        navController.navigate("admin_announcements")
                    }) {
                        Icon(Icons.Default.Campaign, contentDescription = "Announcements", tint = Color.Gray)
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("role_selection") { popUpTo(0) { inclusive = true } }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Stats Cards Row ---
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatsCard(
                        title = "Pending",
                        count = pendingUsers.size.toString(),
                        color = Color(0xFFFFA000), // Amber
                        icon = Icons.Default.PendingActions,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatsCard(
                        title = "Active Users",
                        count = activeUsersCount, // Now showing fetched active user count
                        color = Color(0xFF266FEF), // Blue
                        icon = Icons.Default.Group,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // --- Pending Requests Section ---
                Text(
                    text = "New Requests (${pendingUsers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (pendingUsers.isEmpty()) {
                    EmptyStateView()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingUsers) { user ->
                            AdminUserRequestCard(
                                user = user,
                                onApprove = {
                                    updateStatus(db, user.userId, "approved") {
                                        // Optimistic update: Remove from list locally
                                        pendingUsers = pendingUsers.filter { it.userId != user.userId }
                                        Toast.makeText(context, "Approved ${user.name}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onReject = {
                                    updateStatus(db, user.userId, "rejected") {
                                        pendingUsers = pendingUsers.filter { it.userId != user.userId }
                                        Toast.makeText(context, "Rejected ${user.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// 3. HELPER FUNCTIONS (Logic Refactored)
// ----------------------------------------

/**
 * Fetches pending users and attempts to resolve linked student names.
 */
suspend fun fetchPendingUsers(db: FirebaseFirestore, busId: Int): List<PendingUser> {
    val pendingSnapshot = db.collection("users")
        .whereEqualTo("busId", busId)
        .whereEqualTo("status", "pending")
        .get().await()

    val pendingList = pendingSnapshot.documents.map { doc ->
        PendingUser(
            userId = doc.id,
            name = doc.getString("name") ?: "Unknown Parent",
            role = doc.getString("role") ?: "Unknown",
            email = doc.getString("email") ?: "",
            linkedStudentRollNo = doc.getString("linkedStudentId")
        )
    }.toMutableList()

    // Resolve student names for Parent requests (Simplified Coroutine approach)
    val updatedList = pendingList.map { parentRequest ->
        if (parentRequest.role.lowercase() == "parent" && !parentRequest.linkedStudentRollNo.isNullOrEmpty()) {
            val studentSnapshot = db.collection("users")
                .whereEqualTo("rollNumber", parentRequest.linkedStudentRollNo)
                .limit(1)
                .get().await()

            val studentDoc = studentSnapshot.documents.firstOrNull()
            if (studentDoc != null) {
                return@map parentRequest.copy(
                    studentName = studentDoc.getString("name"),
                    studentDepartment = studentDoc.getString("department")
                )
            }
        }
        parentRequest // Return original if not parent or student not found
    }

    return updatedList
}

/**
 * Bundles the data fetching into a single suspend function (called by LaunchedEffect).
 */
suspend fun fetchAllUsersData(db: FirebaseFirestore, busId: Int): Pair<List<PendingUser>, Int> {
    // 1. Fetch Pending Users (including linked student details)
    val pendingUsers = fetchPendingUsers(db, busId)

    // 2. Fetch Active Count
    val activeSnapshot = db.collection("users")
        .whereEqualTo("busId", busId)
        .whereEqualTo("status", "approved")
        .get().await()

    val activeCount = activeSnapshot.size()

    return Pair(pendingUsers, activeCount)
}

fun updateStatus(db: FirebaseFirestore, userId: String, newStatus: String, onSuccess: () -> Unit) {
    db.collection("users").document(userId)
        .update("status", newStatus)
        .addOnSuccessListener { onSuccess() }
}

// ----------------------------------------
// 4. UI COMPONENTS (VISUALS ONLY)
// ----------------------------------------

@Composable
fun StatsCard(title: String, count: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Column {
                Text(text = count, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                Text(text = title, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AdminUserRequestCard(user: PendingUser, onApprove: () -> Unit, onReject: () -> Unit) {
    val (roleColor, roleBg) = when (user.role.lowercase()) {
        "driver" -> Color(0xFF00C853) to Color(0xFFE8F5E9)
        "parent" -> Color(0xFFFF6D00) to Color(0xFFFFF3E0)
        else -> Color(0xFF266FEF) to Color(0xFFE3F2FD)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Role Badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(roleBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = user.role.take(1).uppercase(), color = roleColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                // ⚡ Display Linked Student Details
                if (user.role.lowercase() == "parent" && !user.linkedStudentRollNo.isNullOrEmpty()) {
                    Text(
                        "Linking: ${user.studentName ?: user.linkedStudentRollNo}", // Show Name or Roll No if name fetch failed
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00C853), // Green highlight
                        fontWeight = FontWeight.SemiBold
                    )
                    user.studentDepartment?.let { dept ->
                        Text(
                            "Dept: $dept",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }

                // Original Role/Email
                Text(
                    text = "${user.role} • ${user.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Buttons (Approve/Reject)
            IconButton(onClick = onReject, modifier = Modifier.size(40.dp).background(Color(0xFFFFEBEE), CircleShape)) {
                Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onApprove, modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), CircleShape)) {
                Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("All caught up!", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("No pending requests", fontSize = 12.sp, color = Color.LightGray)
    }
}