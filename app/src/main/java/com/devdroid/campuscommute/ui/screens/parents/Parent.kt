package com.devdroid.campuscommute.ui.screens.parent

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.devdroid.campuscommute.ui.components.AppDrawer
import com.devdroid.campuscommute.ui.components.DriverInfoSection
import com.devdroid.campuscommute.ui.components.TopAppBarComponent
import com.devdroid.campuscommute.ui.screens.home.UserLocationDisplayCard
import com.devdroid.campuscommute.ui.screens.notifications.NotificationScreen
import com.devdroid.campuscommute.ui.screens.parents.RouteInfoScreen
import com.devdroid.campuscommute.ui.screens.profile.ProfileScreen
import com.devdroid.campuscommute.ui.screens.tracker.TrackerScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// --------------------------------------------------------
// PARENT HOME SCREEN (Drawer + Bottom Navigation + Tabs)
// --------------------------------------------------------
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ParentHomeScreen(navController: NavController) {

    val bottomNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var parentName by remember { mutableStateOf("Parent") }
    var childName by remember { mutableStateOf("Child") }
    var busId by remember { mutableStateOf("...") }

    // Fetch parent + child info
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(uid).get()
                .addOnSuccessListener { doc ->
                    parentName = doc.getString("name") ?: "Parent"
                    childName = doc.getString("linkedStudentName") ?: "Child"
                    busId = doc.get("busId")?.toString() ?: "N/A"
                }
        }
    }

    // Detect active bottom tab
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isHomeTab = currentRoute == ParentBottomBarItem.Home.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isHomeTab,
        drawerContent = {
            AppDrawer(
                userName = parentName,
                busId = busId,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }

                    if (route == "logout") {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("role_selection") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onCloseClick = { scope.launch { drawerState.close() } }
            )
        }
    ) {

        Scaffold(
            bottomBar = { ParentBottomBar(bottomNavController) }
        ) { innerPadding ->

            NavHost(
                navController = bottomNavController,
                startDestination = ParentBottomBarItem.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {

                // HOME TAB
                composable(ParentBottomBarItem.Home.route) {
                    ParentHomeContent(
                        parentName = parentName,
                        childName = childName,
                        busId = busId,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNotificationClick = { navController.navigate("notifications") }
                    )
                }

                composable(ParentBottomBarItem.Announcements.route){
                    NotificationScreen(navController)
                }

                // TRACK TAB (Live Tracking)
                composable(ParentBottomBarItem.Track.route) {
                    if (busId != "..." && busId != "N/A") {
                        TrackerScreen(
                            busId = busId,
                            onSeeRouteClick = {},
                            onBackClick = { bottomNavController.popBackStack() }
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (busId == "...") CircularProgressIndicator()
                            else Text("Child is not assigned a bus yet")
                        }
                    }
                }

                // PROFILE TAB
                composable(ParentBottomBarItem.Profile.route) {
                    ProfileScreen(navController)
                }

                composable(ParentBottomBarItem.Info.route) {
                    RouteInfoScreen()
                }
            }
        }
    }
}

// --------------------------------------------------------
// BOTTOM BAR ITEMS FOR PARENT
// --------------------------------------------------------
sealed class ParentBottomBarItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : ParentBottomBarItem("parent_home", "Home", Icons.Default.Home)
    object Announcements : ParentBottomBarItem("notifications", "Announcements", Icons.Default.Campaign)
    object Info : ParentBottomBarItem("info", "Info", Icons.Default.Info)
    object Track : ParentBottomBarItem("parent_track", "Track", Icons.Default.LocationOn)
    object Profile : ParentBottomBarItem("parent_profile", "Profile", Icons.Default.Person)
}

@Composable
fun ParentBottomBar(navController: NavController) {

    val items = listOf(
        ParentBottomBarItem.Home,
        ParentBottomBarItem.Announcements,
        ParentBottomBarItem.Info,
        ParentBottomBarItem.Track,
        ParentBottomBarItem.Profile
    )

    NavigationBar(containerColor = Color.White) {

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

// --------------------------------------------------------
// HOME TAB CONTENT (UI)
// --------------------------------------------------------
@Composable
fun ParentHomeContent(
    parentName: String,
    childName: String,
    busId: String,
    onOpenDrawer: () -> Unit,
    onNotificationClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {

        // TOP APP BAR (Only appears on Home)
        TopAppBarComponent(
            userName = parentName,
            busNumber = "Child Bus: $busId",
            onMenuClick = onOpenDrawer,
            onNotificationClick = onNotificationClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // CHILD CARD
            item {
                UserLocationDisplayCard(
                    busId
                )
            }

            item {
                DriverInfoSection(
                    busId
                )
            }

            // Quick Actions
            item {
                Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(Icons.Default.LocationOn, "Tracker")
                    QuickActionItem(Icons.Default.History, "History")
                    QuickActionItem(Icons.Default.Warning, "Alerts")
                }
            }

            item { Spacer(Modifier.height(100.dp)) }

        }
    }
}

// --------------------------------------------------------
// Quick Action Item
// --------------------------------------------------------
@Composable
fun QuickActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(Color.White),
            modifier = Modifier.size(70.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(30.dp))
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(label)
    }
}
