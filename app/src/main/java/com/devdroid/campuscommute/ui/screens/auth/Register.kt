package com.devdroid.campuscommute.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(role: String, navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // --- 1. Common Fields ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // This variable holds the TEXT input for bus number
    var busIdInput by remember { mutableStateOf("") }

    // --- 2. Role Specific Fields ---
    var rollNo by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var linkedStudentId by remember { mutableStateOf("") }
    var adminCode by remember { mutableStateOf("") }
    val SECRET_ADMIN_CODE = "ADMIN2025"

    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = "Register as $role", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

        // --- COMMON INPUTS ---
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // -------------------------------------------
        //          ROLE SPECIFIC FIELDS
        // -------------------------------------------

        // --- STUDENT ---
        if (role.lowercase() == "student") {
            OutlinedTextField(
                value = busIdInput,
                onValueChange = { newValue ->
                    // Only allow numbers
                    if (newValue.all { it.isDigit() }) busIdInput = newValue
                },
                label = { Text("Bus Number (e.g. 101)") },
                leadingIcon = { Icon(Icons.Default.DirectionsBus, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = rollNo, onValueChange = { rollNo = it },
                label = { Text("Roll Number") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = department, onValueChange = { department = it },
                label = { Text("Department") },
                leadingIcon = { Icon(Icons.Default.School, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = emergencyContact, onValueChange = { emergencyContact = it },
                label = { Text("Emergency Contact") },
                leadingIcon = { Icon(Icons.Default.ContactPhone, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }

        // --- DRIVER ---
        if (role.lowercase() == "driver") {
            OutlinedTextField(
                value = busIdInput,
                onValueChange = { newValue -> if (newValue.all { it.isDigit() }) busIdInput = newValue },
                label = { Text("Assigned Bus Number") },
                leadingIcon = { Icon(Icons.Default.DirectionsBus, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = licenseNumber, onValueChange = { licenseNumber = it },
                label = { Text("License Number") },
                leadingIcon = { Icon(Icons.Default.CardMembership, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }

        // --- ADMIN ---
        if (role.lowercase() == "admin") {
            OutlinedTextField(
                value = busIdInput,
                onValueChange = { newValue -> if (newValue.all { it.isDigit() }) busIdInput = newValue },
                label = { Text("Managing Bus Number") },
                leadingIcon = { Icon(Icons.Default.DirectionsBus, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = adminCode, onValueChange = { adminCode = it },
                label = { Text("Admin Code") },
                leadingIcon = { Icon(Icons.Default.Security, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        // --- PARENT ---
        if (role.lowercase() == "parent") {
            // ✅ ADDED: Bus Number input for Parent
            OutlinedTextField(
                value = busIdInput,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) busIdInput = newValue
                },
                label = { Text("Child's Bus Number") },
                leadingIcon = { Icon(Icons.Default.DirectionsBus, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = linkedStudentId, onValueChange = { linkedStudentId = it },
                label = { Text("Linked Student Roll No") },
                leadingIcon = { Icon(Icons.Default.ChildCare, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- PASSWORD ---
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Toggle")
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SUBMIT BUTTON ---
        Button(
            onClick = {
                // 1. Validate Common Fields
                if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
                    Toast.makeText(context, "Please fill common fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // 2. Validate Bus Number (For Student/Driver/Admin/Parent)
                val finalBusIdInt: Int? = busIdInput.toIntOrNull()

                // ✅ UPDATED VALIDATION LIST: Includes 'parent'
                if (role.lowercase() in listOf("student", "driver", "admin", "parent")) {
                    if (finalBusIdInt == null) {
                        Toast.makeText(context, "Please enter a valid Bus Number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                }

                // 3. Validate Admin Code
                if (role.lowercase() == "admin" && adminCode != SECRET_ADMIN_CODE) {
                    Toast.makeText(context, "Invalid Admin Code", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user!!.uid

                        val userData = hashMapOf(
                            "userId" to uid,
                            "name" to name,
                            "email" to email,
                            "phone" to phone,
                            "role" to role.lowercase(),
                            "registeredAt" to System.currentTimeMillis(),
                            "status" to if (role.lowercase() == "admin") "approved" else "pending"
                        )

                        // ✅ SAVE BUS ID TO MAP (Done for all roles requiring it)
                        if (finalBusIdInt != null) {
                            userData["busId"] = finalBusIdInt
                        }

                        // Add Specific Fields
                        when (role.lowercase()) {
                            "student" -> {
                                userData["rollNumber"] = rollNo
                                userData["department"] = department
                                userData["emergencyContact"] = emergencyContact
                            }
                            "driver" -> {
                                userData["licenseNumber"] = licenseNumber
                            }
                            "parent" -> {
                                userData["linkedStudentId"] = linkedStudentId
                            }
                            "admin" -> {
                                userData["adminCode"] = "VERIFIED"
                            }
                        }

                        db.collection("users").document(uid).set(userData)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Account Created!", Toast.LENGTH_SHORT).show()
                                navController.navigate("login/$role") {
                                    popUpTo("register/$role") { inclusive = true }
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "DB Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Reg Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF266FEF)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Register", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account? ", color = Color.Gray)
            Text(text = "Login", color = Color(0xFF266FEF), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { navController.navigate("login/$role") })
        }
        Spacer(modifier = Modifier.height(50.dp))
    }
}