package com.devdroid.campuscommute.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Data model for the list
data class ManagedUser(
    val userId: String,
    val name: String,
    val role: String,
    val phone: String,
    val email: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // --- State ---
    var allUsers by remember { mutableStateOf<List<ManagedUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var adminBusId by remember { mutableStateOf("") }

    // ⚡ Fetch Admin's Bus ID & Users
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // 1. Get Admin Bus ID
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                val busId = doc.get("busId")?.toString()
                if (!busId.isNullOrEmpty()) {
                    adminBusId = busId

                    // 2. Get All Approved Users for this Bus
                    // Note: We fetch ALL roles to manage everyone in one place
                    db.collection("users")
                        .whereEqualTo("busId", busId.toIntOrNull() ?: 0)
                        .whereEqualTo("status", "approved")
                        .get()
                        .addOnSuccessListener { result ->
                            val list = result.documents.map { userDoc ->
                                ManagedUser(
                                    userId = userDoc.id,
                                    name = userDoc.getString("name") ?: "Unknown",
                                    role = userDoc.getString("role") ?: "Student",
                                    phone = userDoc.getString("phone") ?: "",
                                    email = userDoc.getString("email") ?: ""
                                )
                            }
                            allUsers = list
                            isLoading = false
                        }
                        .addOnFailureListener { isLoading = false }
                } else {
                    isLoading = false
                }
            }
        }
    }

    // Filter Logic
    val filteredUsers = allUsers.filter {
        it.name.contains(searchText, ignoreCase = true) ||
                it.role.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bus $adminBusId Users", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search by Name or Role") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF266FEF))
                }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserListCard(user) { phoneNumber ->
                            // Call Action
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneNumber")
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListCard(user: ManagedUser, onCall: (String) -> Unit) {
    val (roleColor, roleBg) = when (user.role.lowercase()) {
        "driver" -> Color(0xFF00C853) to Color(0xFFE8F5E9)
        "parent" -> Color(0xFFFF6D00) to Color(0xFFFFF3E0)
        else -> Color(0xFF266FEF) to Color(0xFFE3F2FD)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar / Role Initial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(roleBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.role.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = roleColor,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${user.role.replaceFirstChar { it.uppercase() }} • ${user.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Call Button
            if (user.phone.isNotEmpty()) {
                IconButton(
                    onClick = { onCall(user.phone) },
                    modifier = Modifier
                        .background(Color(0xFFE0F2F1), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.Call, "Call", tint = Color(0xFF00695C))
                }
            }
        }
    }
}