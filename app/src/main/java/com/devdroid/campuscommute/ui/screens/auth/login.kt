package com.devdroid.campuscommute.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.UserPreferences
import com.devdroid.campuscommute.utils.FcmTokenManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

val LoginBlue = Color(0xFF266FEF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(role: String, navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun performLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }

        isLoading = true
        errorMessage = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->

                val userId = authResult.user?.uid
                if (userId == null) {
                    errorMessage = "Login failed. User not found."
                    isLoading = false
                    return@addOnSuccessListener
                }

                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        isLoading = false

                        if (!document.exists()) {
                            errorMessage = "User profile not found."
                            return@addOnSuccessListener
                        }

                        val userRole = document.getString("role") ?: "student"
                        val status = document.getString("status") ?: "pending"

// 1. Wrong portal role
                        if (!userRole.equals(role, ignoreCase = true)) {
                            auth.signOut()
                            errorMessage =
                                "Access Denied: You are not a $role. You are a $userRole."
                            return@addOnSuccessListener
                        }

// 2. Admin bypass (NO approval needed)
                        if (role.lowercase() == "admin") {
                            Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()

                            UserPreferences.saveUserRole(context, "admin")

                            navController.navigate("admin") {
                                popUpTo(0) { inclusive = true } // Crash-proof pop
                            }

                            return@addOnSuccessListener
                        }

// 3. Other roles require approval
                        if (status == "approved") {
                            Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                            UserPreferences.saveUserRole(context, role.lowercase())
                            FcmTokenManager.updateToken()
                            val destination = when (role.lowercase()) {
                                "student" -> "student"
                                "driver" -> "driver"
                                "parent" -> "parent"
                                else -> "student"
                            }

                            // Simple clean navigation
                            navController.navigate(destination) {
                                popUpTo("login") { inclusive = true }    // remove login screen
                            }
                        } else {

// Pending or rejected → Approval screen
                            navController.navigate("approval_pending") {

                                popUpTo(0) { inclusive = true }

                            }

                        }

                    }

                    .addOnFailureListener {
                        isLoading = false
                        errorMessage = "Failed to verify profile: ${it.localizedMessage}"
                    }

            }

            .addOnFailureListener {
                isLoading = false
                errorMessage = it.localizedMessage ?: "Login failed"
            }

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Login as $role",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            placeholder = { Text("example@college.edu") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            isError = errorMessage != null
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { performLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LoginBlue),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(text = "Login", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account? ", color = Color.Gray)
            Text(
                text = "Register",
                color = LoginBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    navController.navigate("register/$role")
                }
            )
        }
    }
}