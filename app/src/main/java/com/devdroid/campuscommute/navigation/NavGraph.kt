package com.devdroid.campuscommute.navigation

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devdroid.campuscommute.MainActivity
import com.devdroid.campuscommute.ui.screens.AttendanceScreen
import com.devdroid.campuscommute.ui.screens.HomeScreen
import com.devdroid.campuscommute.ui.screens.Splashscreen.SplashScreen
import com.devdroid.campuscommute.ui.screens.admin.AdminAnnouncementScreen
import com.devdroid.campuscommute.ui.screens.admin.AdminDashboardScreen
import com.devdroid.campuscommute.ui.screens.admin.AdminProfileScreen
import com.devdroid.campuscommute.ui.screens.admin.ManageRouteScreen
import com.devdroid.campuscommute.ui.screens.admin.UserManagementScreen
import com.devdroid.campuscommute.ui.screens.auth.ApprovalPendingScreen
import com.devdroid.campuscommute.ui.screens.auth.LoginScreen
import com.devdroid.campuscommute.ui.screens.auth.RegisterScreen
import com.devdroid.campuscommute.ui.screens.chat.ChatScreen
import com.devdroid.campuscommute.ui.screens.driver.DriverParkedLocationScreen // 👈 NEW FEATURE IMPORT
import com.devdroid.campuscommute.ui.screens.driver.DriverScreen
import com.devdroid.campuscommute.ui.screens.driver.PassengerCountScreen
import com.devdroid.campuscommute.ui.screens.home.IncidentReportingScreen
import com.devdroid.campuscommute.ui.screens.home.StudentBoardingPassScreen
import com.devdroid.campuscommute.ui.screens.map.RealMapScreen
import com.devdroid.campuscommute.ui.screens.notifications.NotificationScreen
import com.devdroid.campuscommute.ui.screens.onboarding.OnBoardingScreen
import com.devdroid.campuscommute.ui.screens.parent.ParentHomeScreen
import com.devdroid.campuscommute.ui.screens.parents.RouteInfoScreen
import com.devdroid.campuscommute.ui.screens.profile.ProfileScreen
import com.devdroid.campuscommute.ui.screens.route.StopRouteScreen
import com.devdroid.campuscommute.ui.screens.tracker.BusOfflineScreen
import com.devdroid.campuscommute.ui.screens.tracker.BusReachedScreen
import com.devdroid.campuscommute.ui.screens.tracker.TrackerScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(navController: NavHostController, context: MainActivity) {
    NavHost(navController = navController, startDestination = "splash") {

        // 1. Splash Screen
        composable("splash") {
            SplashScreen(navController = navController, context = context)
        }

        // 2. Onboarding Screen
        composable("onboarding") {
            OnBoardingScreen(navController = navController, context = context)
        }

        // 3. Role Selection
        composable("role_selection") {
            RoleSelectionScreen(navController = navController)
        }

        // 4. Login Screen (Dynamic Argument)
        composable(
            route = "login/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val selectedRole = backStackEntry.arguments?.getString("role") ?: "student"
            LoginScreen(role = selectedRole, navController = navController)
        }

        // 5. Register Screen (Dynamic Argument)
        composable(
            route = "register/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val selectedRole = backStackEntry.arguments?.getString("role") ?: "student"
            RegisterScreen(role = selectedRole, navController = navController)
        }

        composable("approval_pending") {
            ApprovalPendingScreen(navController = navController)
        }

        // --- ✅ ROLE SPECIFIC DESTINATIONS ---

        composable("student") {
            HomeScreen(navController) // **HomeScreen will load busId for the display card**
        }

        composable(
            route = "map_screen/{busId}",
            arguments = listOf(navArgument("busId") { type = NavType.StringType })
        ) { backStackEntry ->
            val busId = backStackEntry.arguments?.getString("busId") ?: ""
            // Call the map screen with the ID retrieved from the navigation argument
            RealMapScreen(userBusId = busId)
        }

        composable("admin") {
            AdminDashboardScreen(navController)
        }

        composable("driver") {
            navController.navigate("driver_dashboard") {
                popUpTo("driver") { inclusive = true }
            }
        }

        // ----------------------------------------------------
        // 🚌 NEW BUS LOCATION ROUTES 🚌
        // ----------------------------------------------------

        // 1. Driver Location Selector (NEW SCREEN)
        composable(
            route = "driver_set_location/{busId}",
            arguments = listOf(navArgument("busId") { type = NavType.StringType })
        ) { backStackEntry ->
            val busId = backStackEntry.arguments?.getString("busId") ?: ""
            DriverParkedLocationScreen(navController = navController, driverBusId = busId)
        }

        // 2. (Note: UserLocationDisplayCard is now integrated directly into HomeScreen.kt)

        // ----------------------------------------------------
        // --- OTHER SCREENS ---
        // ----------------------------------------------------

        composable("notifications") {
            NotificationScreen(navController)
        }


        composable("profile") { ProfileScreen(navController) }

        composable("info") {
            RouteInfoScreen()
        }

        // ✅ UPDATED TRACKER ROUTE
        composable(
            route = "tracker/{busId}",
            arguments = listOf(navArgument("busId") { type = NavType.StringType })
        ) { backStackEntry ->
            val busId = backStackEntry.arguments?.getString("busId") ?: "bus56"

            TrackerScreen(
                busId = busId,
                onSeeRouteClick = { currentStopIndex ->
                    // Pass busId here too
                    navController.navigate("routeScreen/$busId/$currentStopIndex")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "routeScreen/{busId}/{stopIndex}",
            arguments = listOf(
                navArgument("busId") { type = NavType.StringType },
                navArgument("stopIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val busId = backStackEntry.arguments?.getString("busId") ?: ""
            val index = backStackEntry.arguments?.getInt("stopIndex") ?: 0

            StopRouteScreen(
                busId = busId,
                currentStopIndex = index,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("manage_route") {
            ManageRouteScreen(navController)
        }

        composable("chat"){
            ChatScreen(navController)
        }
        composable("driver_dashboard") { DriverScreen(navController) }
        composable("driver_map") { ChatScreen(navController) } // Placeholder
        composable("driver_passengers") { AttendanceScreen() }
        composable("driver_profile") { ProfileScreen(navController) }

        composable("offline"){
            BusOfflineScreen( onRefresh = {})
        }

        composable("reached") {
            BusReachedScreen(
                destination = "College Campus",
                onRefresh = { /* This callback should trigger navigation or data check */ }
            )
        }

        composable("incident_report") {
            IncidentReportingScreen(navController)
        }

        composable("parent_link") {  }
        composable("parent") { ParentHomeScreen(navController) }

        composable("user_management") {
            UserManagementScreen(navController)
        }

        composable("admin_announcements") {
            AdminAnnouncementScreen(navController)
        }

        composable("admin_profile") {
            AdminProfileScreen(navController)
        }

        composable("admin_tracker_redirect") {
            // Logic: Fetch Admin's Bus ID and redirect to Tracker
            val context = LocalContext.current
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            // Loading UI while fetching ID
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            LaunchedEffect(Unit) {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                        val busId = doc.get("busId")?.toString()
                        if (!busId.isNullOrEmpty()) {
                            navController.navigate("tracker/$busId") {
                                popUpTo("admin") // Clean up the backstack
                            }
                        } else {
                            Toast.makeText(context, "No Bus Assigned to Track", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                }
            }
        }

        composable("passenger_count") {
            PassengerCountScreen(navController)
        }
        composable("student_pass") { StudentBoardingPassScreen(navController) }


    }
}