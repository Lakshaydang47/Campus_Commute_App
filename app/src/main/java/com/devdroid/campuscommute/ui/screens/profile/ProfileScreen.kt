package com.devdroid.campuscommute.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Data structure to hold student details fetched from Firestore
data class StudentDetails(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val busId: String = "N/A",
    val rollNumber: String = "N/A",
    val department: String = "N/A",
    val emergencyContact: String = "N/A"
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // --- State ---
    var userData by remember { mutableStateOf(StudentDetails(email = auth.currentUser?.email ?: "N/A")) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Temporary states for editing
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editEmergencyContact by remember { mutableStateOf("") }

    // ⚡ FETCH STUDENT DATA ON LOAD
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val data = doc.data
                    if (data != null) {
                        userData = StudentDetails(
                            id = currentUser.uid,
                            name = doc.getString("name") ?: "Student",
                            email = currentUser.email ?: "N/A",
                            phone = doc.getString("phone") ?: "N/A",
                            busId = doc.get("busId")?.toString() ?: "N/A",
                            rollNumber = doc.getString("rollNumber") ?: "N/A",
                            department = doc.getString("department") ?: "N/A",
                            emergencyContact = doc.getString("emergencyContact") ?: "N/A"
                        )
                        // Initialize edit fields
                        editName = userData.name
                        editPhone = userData.phone
                        editEmergencyContact = userData.emergencyContact
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else { isLoading = false }
    }

    // --- ACTIONS ---
    val onLogout = {
        auth.signOut()
        UserPreferences.clearUserRole(context)
        navController.navigate("role_selection") { popUpTo(0) { inclusive = true } }
    }

    val onSave = {
        isSaving = true
        val updatedData = mutableMapOf<String, Any>()

        // Check for changes
        if (editName != userData.name) updatedData["name"] = editName
        if (editPhone != userData.phone) updatedData["phone"] = editPhone
        if (editEmergencyContact != userData.emergencyContact) updatedData["emergencyContact"] = editEmergencyContact

        if (updatedData.isNotEmpty()) {
            db.collection("users").document(userData.id).update(updatedData)
                .addOnSuccessListener {
                    // Update the main userData state and switch back to view mode
                    userData = userData.copy(
                        name = editName,
                        phone = editPhone,
                        emergencyContact = editEmergencyContact
                    )
                    isSaving = false
                    isEditing = false
                    Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    isSaving = false
                    Toast.makeText(context, "Update Failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } else {
            isSaving = false
            isEditing = false
            Toast.makeText(context, "No changes detected.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STUDENT Profile") },
                actions = {
                    if (!isLoading) {
                        if (isEditing) {
                            TextButton(onClick = onSave as () -> Unit, enabled = !isSaving) {
                                if (isSaving) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                else Text("SAVE", color = Color(0xFF266FEF), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF266FEF))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- HEADER ---
                ProfileHeader(userData.name, "STUDENT")

                Spacer(modifier = Modifier.height(32.dp))

                // --- DETAILS CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // COMMON FIELDS (Editable)
                        ProfileField(
                            icon = Icons.Default.Person, title = "Full Name",
                            value = editName, isEditing = isEditing,
                            onValueChange = { editName = it }
                        )
                        Divider(Modifier.padding(vertical = 12.dp))

                        // EMAIL (Read-only)
                        ProfileField(
                            icon = Icons.Default.Email, title = "Email Address",
                            value = userData.email, isEditing = false
                        )
                        Divider(Modifier.padding(vertical = 12.dp))

                        // PHONE (Editable)
                        ProfileField(
                            icon = Icons.Default.Phone, title = "Phone Number",
                            value = editPhone, isEditing = isEditing,
                            keyboardType = KeyboardType.Phone,
                            onValueChange = { editPhone = it }
                        )
                        Divider(Modifier.padding(vertical = 12.dp))

                        // BUS ID (Read-only)
                        ProfileField(
                            icon = Icons.Default.DirectionsBus, title = "Assigned Bus",
                            value = "Bus No. ${userData.busId}", isEditing = false
                        )
                        Divider(Modifier.padding(vertical = 12.dp))

                        // STUDENT SPECIFIC READ-ONLY FIELDS
                        ProfileField(icon = Icons.Default.Badge, title = "Roll Number", value = userData.rollNumber, isEditing = false)
                        Divider(Modifier.padding(vertical = 12.dp))
                        ProfileField(icon = Icons.Default.School, title = "Department", value = userData.department, isEditing = false)
                        Divider(Modifier.padding(vertical = 12.dp))

                        // STUDENT SPECIFIC EDITABLE FIELD
                        ProfileField(
                            icon = Icons.Default.ContactPhone, title = "Emergency Contact",
                            value = editEmergencyContact, isEditing = isEditing,
                            keyboardType = KeyboardType.Phone,
                            onValueChange = { editEmergencyContact = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- Logout Button ---
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- UI HELPERS ---

@Composable
fun ProfileHeader(name: String, role: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = Color(0xFFE0F2F1)) {
            Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(role.uppercase(), fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ProfileField(
    icon: ImageVector,
    title: String,
    value: String,
    isEditing: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Icon Badge
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFF266FEF), modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (isEditing) {
            // EDIT MODE
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                shape = RoundedCornerShape(12.dp),
                // ✅ FIX: Provide the title as the label
                label = { Text(title) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f),
                // This comment seems to be from a previous fix, you can keep or remove it.
                // ✅ USING SIMPLE DEFAULTS: This line is the solution to the compiler crash.
                // We rely on standard Material 3 theming.
                // The contentPadding parameter is not available here, it's typically set on the parent.
                // You can remove the below line as it might cause another error.
                // contentPadding = PaddingValues(16.dp)
            )
        } else {
            // VIEW MODE
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            }
        }
    }
}
