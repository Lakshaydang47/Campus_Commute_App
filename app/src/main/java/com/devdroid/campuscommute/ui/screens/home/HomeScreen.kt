package com.devdroid.campuscommute.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devdroid.campuscommute.R
import kotlinx.coroutines.launch

import com.devdroid.campuscommute.ui.components.AppDrawer
import com.devdroid.campuscommute.ui.components.StudentBottomBar
import com.devdroid.campuscommute.ui.components.TopAppBarComponent
import com.devdroid.campuscommute.ui.components.BottomBarItem
import com.devdroid.campuscommute.ui.components.DriverInfoSection
import com.devdroid.campuscommute.ui.components.SchoolLocationComponent
import com.devdroid.campuscommute.ui.screens.home.UserLocationDisplayCard
import com.devdroid.campuscommute.ui.screens.map.RealMapScreen
import com.devdroid.campuscommute.ui.screens.profile.ProfileScreen
import com.devdroid.campuscommute.ui.screens.tracker.TrackerScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController) {
    val bottomNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // --- State ---
    var userName by remember { mutableStateOf("Student") }
    var busId by remember { mutableStateOf("...") }

    val systemUiController = rememberSystemUiController()
    val blue = Color(0xFF266FEF)

    DisposableEffect(Unit) {
        // Set blue when entering screen
        systemUiController.setStatusBarColor(
            color = blue,
            darkIcons = false  // white icons
        )

        onDispose {
            // Reset back to default when leaving the screen
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = true
            )
        }
    }

    // --- Fetch Data ---
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("name") ?: "Student"
                    busId = doc.get("busId")?.toString() ?: "N/A"
                }
        }
    }

    // ✅ LOGIC FIX: Check if we are on the Home Tab
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isHomeTab = currentRoute == BottomBarItem.Home.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        // ✅ CRITICAL FIX: Disable swipe gestures if NOT on Home
        gesturesEnabled = isHomeTab,
        drawerContent = {
            AppDrawer(
                userName = userName,
                busId = busId,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    if (route == "logout") {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("role_selection") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        // Handle other drawer navs if needed
                    }
                },
                onCloseClick = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            bottomBar = { StudentBottomBar(navController = bottomNavController) }
        ) { innerPadding ->

            NavHost(
                navController = bottomNavController,
                startDestination = BottomBarItem.Home.route,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                // TAB 1: HOME (Has Drawer & Top Bar)
                composable(BottomBarItem.Home.route) {
                    HomeContent(
                        userName = userName,
                        busId = busId,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNotificationClick = { navController.navigate("notifications") },
                        modifier = Modifier.navigationBarsPadding(),
                        onQRClick = { navController.navigate("student_pass") }
                    )
                }


                // TAB 2: TRACKER (Full Screen, No Drawer Swipe)
                composable(BottomBarItem.Route.route) {
                    if (busId != "..." && busId != "N/A") {
                        TrackerScreen(
                            busId = busId,
                            onSeeRouteClick = { currentStopIndex ->
                                navController.navigate("routeScreen/$busId/$currentStopIndex")
                            },
                            onBackClick = { bottomNavController.popBackStack() }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if(busId == "...") CircularProgressIndicator() else Text("No Bus Assigned")
                        }
                    }
                }// OTHER TABS
                composable(BottomBarItem.Chat.route) { RealMapScreen(busId) }


                composable(BottomBarItem.Attendance.route) { AttendanceScreen() }

                composable(BottomBarItem.Profile.route) { ProfileScreen(navController = navController) }
            }
        }
    }
}

// ---------------------------------------------------------
// 2. HOME CONTENT
// ---------------------------------------------------------
@Composable
fun HomeContent(
    userName: String,
    busId: String,
    onOpenDrawer: () -> Unit,
    onNotificationClick: () -> Unit,
    onQRClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF9FAFB))
    ) {
        // ✅ Top Bar is ONLY here, so it won't show on Tracker/Profile
        TopAppBarComponent(
            userName = userName,
            busNumber = "Bus No. $busId",
            onMenuClick = onOpenDrawer,
            onNotificationClick = onNotificationClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
               BoardingPassCard(onClick = onQRClick)
            }

            item {
                Spacer(Modifier.height(16.dp))
                UserLocationDisplayCard(userBusId = busId)
                Spacer(Modifier.height(16.dp))
            }

            item {
                DriverInfoSection(busId = busId)
            }

            item {
                SchoolLocationComponent(
                    address = "70 Milestone, Grand Trunk Rd, Samalkha, Haryana 132102",
                    website = "www.piet.co.in",
                    phoneNumber = "+1800 120 6884"
                )
            }
        }
    }
}

// ... (Keep existing UI Helpers like QuickActionItem) ...
@Composable
fun QuickActionItem(icon: ImageVector, label: String, color: Long) { /*...*/ }
@Composable
fun NoticeItem(title: String, time: String) { /*...*/ }
@Composable
fun PlaceholderPage(title: String) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){Text(title)} }

@Composable
fun BoardingPassCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp), // Slightly taller for visibility
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF266FEF)), // Brand Blue background
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Boarding Pass",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to show QR Code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // QR Icon (Visual cue)
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.qr_code), // Make sure you have this icon
                        contentDescription = "QR Code",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

}