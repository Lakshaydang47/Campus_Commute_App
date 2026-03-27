package com.devdroid.campuscommute.ui.screens.admin

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

// Data model for Admin
data class AdminDetails(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val busId: String = "N/A" // The bus this admin manages
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // --- State ---
    var adminData by remember { mutableStateOf(AdminDetails(email = auth.currentUser?.email ?: "N/A")) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Temporary edit states
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    // ⚡ FETCH ADMIN DATA
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        adminData = AdminDetails(
                            id = uid,
                            name = doc.getString("name") ?: "Admin",
                            email = doc.getString("email") ?: "N/A",
                            phone = doc.getString("phone") ?: "N/A",
                            busId = doc.get("busId")?.toString() ?: "N/A"
                        )
                        // Init edit fields
                        editName = adminData.name
                        editPhone = adminData.phone
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            isLoading = false
        }
    }

    // --- ACTIONS ---
    val onLogout = {
        auth.signOut()
        UserPreferences.clearUserRole(context)
        navController.navigate("role_selection") {
            popUpTo(0) { inclusive = true }
        }
    }

    val onSave = {
        isSaving = true
        val updates = mutableMapOf<String, Any>()

        if (editName != adminData.name) updates["name"] = editName
        if (editPhone != adminData.phone) updates["phone"] = editPhone

        if (updates.isNotEmpty()) {
            db.collection("users").document(adminData.id).update(updates)
                .addOnSuccessListener {
                    adminData = adminData.copy(name = editName, phone = editPhone)
                    isSaving = false
                    isEditing = false
                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    isSaving = false
                    Toast.makeText(context, "Update Failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            isSaving = false
            isEditing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    if (!isLoading) {
                        if (isEditing) {
                            TextButton(onClick = onSave as () -> Unit, enabled = !isSaving) {
                                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp))
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF266FEF))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                AdminProfileHeader(adminData.name, "Bus Manager")

                Spacer(modifier = Modifier.height(32.dp))

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Name (Editable)
                        AdminProfileField(
                            icon = Icons.Default.Person, title = "Full Name",
                            value = editName, isEditing = isEditing,
                            onValueChange = { editName = it }
                        )
                        Divider(Modifier.padding(vertical = 12.dp))

                        // Email (Read-only)
                        AdminProfileField(
                            icon = Icons.Default.Email, title = "Email",
                            value = adminData.email, isEditing = false
                        )
                        Divider(Modifier.padding(vertical = 12.dp))

                        // Phone (Editable)
                        AdminProfileField(
                            icon = Icons.Default.Phone, title = "Phone",
                            value = editPhone, isEditing = isEditing,
                            keyboardType = KeyboardType.Phone,
                            onValueChange = { editPhone = it }
                        )
                        Divider(Modifier.padding(vertical = 12.dp))

                        // Bus ID (Read-only)
                        AdminProfileField(
                            icon = Icons.Default.DirectionsBus, title = "Managing Bus",
                            value = "Bus No. ${adminData.busId}", isEditing = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Logout
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- UI Helpers ---

@Composable
fun AdminProfileHeader(name: String, role: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = Color(0xFFE3F2FD)) {
            Icon(Icons.Default.AdminPanelSettings, null, tint = Color(0xFF266FEF), modifier = Modifier.padding(20.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(role, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AdminProfileField(
    icon: ImageVector,
    title: String,
    value: String,
    isEditing: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))

        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(title) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF266FEF),
                    unfocusedIndicatorColor = Color.LightGray
                )
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            }
        }
    }
}