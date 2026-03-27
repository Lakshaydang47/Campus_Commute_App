package com.devdroid.campuscommute.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.UserPreferences
import com.devdroid.campuscommute.utils.logoutUser // ✅ Uses your logout helper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ApprovalPendingScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Loading state for the refresh button
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Icon
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = "Pending",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFFFA000) // Amber/Orange color for "Waiting"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Title
        Text(
            text = "Approval Pending",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Explanation Text
        Text(
            text = "Your account is currently under review by the Bus Admin.\n\nYou cannot access the dashboard until your request is approved.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 4. REFRESH STATUS BUTTON
        Button(
            onClick = {
                isChecking = true
                val uid = auth.currentUser?.uid

                if (uid != null) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            isChecking = false
                            if (document.exists()) {
                                val status = document.getString("status") ?: "pending"
                                val role = document.getString("role") ?: "student"

                                if (status == "approved") {
                                    // ✅ APPROVED!
                                    Toast.makeText(context, "Account Approved!", Toast.LENGTH_SHORT).show()

                                    // 1. Save Role Locally
                                    UserPreferences.saveUserRole(context, role.lowercase())

                                    // 2. Navigate to Home
                                    // We use the exact strings from your NavGraph
                                    val destination = when (role.lowercase()) {
                                        "driver" -> "driver"    // Must match composable("driver")
                                        "student" -> "student"  // Must match composable("student")
                                        "parent" -> "parent"    // Must match composable("parent")
                                        "admin" -> "admin"      // Must match composable("admin")
                                        else -> "student"
                                    }

                                    navController.navigate(destination) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }

                                } else if (status == "rejected") {
                                    // ❌ REJECTED
                                    Toast.makeText(context, "Your request was rejected.", Toast.LENGTH_LONG).show()
                                } else {
                                    // ⏳ STILL PENDING
                                    Toast.makeText(context, "Still pending approval...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener {
                            isChecking = false
                            Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF266FEF)),
            enabled = !isChecking
        ) {
            if (isChecking) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Checking...")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Status")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. LOGOUT BUTTON
        // Important: If they registered with the wrong email, they need a way to leave.
        TextButton(onClick = { logoutUser(context, navController) }) {
            Text("Logout", color = Color.Red)
        }
    }
}