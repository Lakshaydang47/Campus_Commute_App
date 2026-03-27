package com.devdroid.campuscommute.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
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
import com.devdroid.campuscommute.data.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// NOTE: Ensure your NotificationModel class is defined like this in your data package:
/*
data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val type: String = "info",
    // busId is an Int (e.g., 15), stored as a String in the model for Firebase safety
    val busId: String = ""
)
*/


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val realtimeDb = FirebaseDatabase.getInstance()

    // --- State Management ---
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    // Stores the admin's busId as a String (e.g., "15") for use in the database path
    var adminBusId by remember { mutableStateOf<String?>(null) }

    // History State
    var sentHistory by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(true) }

    // 1. Fetch Admin Bus ID (From Firestore)
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                // Fetch the busId (which is an Int, e.g., 15)
                val doc = db.collection("users").document(uid).get().await()
                // Convert the Int to String for consistency in Firebase keys/data types
                adminBusId = doc.getLong("busId")?.toString()
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 2. Listen for History (From Realtime DB)
    DisposableEffect(adminBusId) {
        if (adminBusId != null) {
            val ref = realtimeDb.getReference("notifications")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<NotificationModel>()
                    for (child in snapshot.children) {
                        try {
                            val notif = child.getValue(NotificationModel::class.java)
                            // Filter: Show only notifications for THIS bus
                            if (notif != null && notif.busId == adminBusId) {
                                list.add(notif)
                            }
                        } catch (e: Exception) {
                            // Safely ignore or log bad data structures
                        }
                    }
                    // Sort newest first
                    sentHistory = list.sortedByDescending { it.timestamp }
                    isLoadingHistory = false
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoadingHistory = false
                    Toast.makeText(context, "History loading failed.", Toast.LENGTH_SHORT).show()
                }
            }
            ref.addValueEventListener(listener)
            onDispose { ref.removeEventListener(listener) }
        } else {
            onDispose { }
        }
    }

    // 3. Send Action (Write to Realtime DB)
    val onSend = {
        if (title.isNotBlank() && message.isNotBlank() && adminBusId != null) {
            isSending = true

            // Generate a new key
            val ref = realtimeDb.getReference("notifications").push()

            val newNotif = NotificationModel(
                id = ref.key ?: "",
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                type = "info",
                // ⚡ Crucial: Send the admin's busId (e.g., "15") ⚡
                busId = adminBusId!!
            )

            ref.setValue(newNotif).addOnCompleteListener {
                isSending = false
                if (it.isSuccessful) {
                    Toast.makeText(context, "Announcement Sent! 📢", Toast.LENGTH_SHORT).show()
                    // Clear fields
                    title = ""
                    message = ""
                } else {
                    Toast.makeText(context, "Failed to send: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (adminBusId == null) {
            Toast.makeText(context, "Bus ID not loaded yet. Please wait.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Announcements", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            // --- NEW MESSAGE CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("New Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g. Bus Delayed)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onSend as () -> Unit,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isSending && title.isNotBlank() && message.isNotBlank() && adminBusId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF266FEF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Broadcast Alert")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- HISTORY SECTION ---
            Text(
                "Sent History (Bus: ${adminBusId ?: "Loading..."})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(Modifier.height(12.dp))

            if (isLoadingHistory) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF266FEF))
                }
            } else if (sentHistory.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No announcements sent yet.", color = Color.LightGray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(sentHistory) { item ->
                        HistoryItem(item)
                    }
                }
            }
        }
    }
}

// --- HistoryItem Composable ---
@Composable
fun HistoryItem(item: NotificationModel) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Announcement", tint = Color(0xFF266FEF), modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.message, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
            }
        }
    }
}