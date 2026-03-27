package com.devdroid.campuscommute.utils

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth

fun logoutUser(context: Context, navController: NavController) {
    // 1. Sign out from Firebase
    FirebaseAuth.getInstance().signOut()

    // 2. Clear Local Cache (The Role)
    UserPreferences.clearUserRole(context)

    // 3. Show Message
    Toast.makeText(context, "Logged Out Successfully", Toast.LENGTH_SHORT).show()

    // 4. Navigate back to Role Selection & Clear History
    navController.navigate("role_selection") {
        popUpTo(0) { inclusive = true } // Clears the entire back stack
    }
}