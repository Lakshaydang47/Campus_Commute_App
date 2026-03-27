package com.devdroid.campuscommute.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.ChatMessage
import com.devdroid.campuscommute.data.UserSession // Using the revived UserSession to get Bus ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    // ⚡ Fetch critical user data from memory
    val currentUserId = auth.currentUser?.uid ?: "anonymous"
    val userName = UserSession.userName ?: "Guest"
    val userRole = UserSession.userRole ?: "student"
    val busId = UserSession.assignedBusId ?: "56" // Default bus ID for chat room

    // Firebase Reference: chats/{busId}
    val chatRef = FirebaseDatabase.getInstance().getReference("chats/$busId")

    // State
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    val listState = rememberLazyListState()

    // 1. Listen for new messages
    DisposableEffect(busId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) {
                        list.add(msg)
                    }
                }
                messages = list.sortedBy { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        chatRef.addValueEventListener(listener)
        onDispose { chatRef.removeEventListener(listener) }
    }

    // 2. Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 3. Send message function
    val sendMessage = {
        if (messageText.isNotBlank()) {
            val newMessageRef = chatRef.push()
            val message = ChatMessage(
                id = newMessageRef.key ?: UUID.randomUUID().toString(),
                userId = currentUserId,
                userName = userName,
                userRole = userRole,
                text = messageText,
                timestamp = System.currentTimeMillis()
            )
            newMessageRef.setValue(message)
            messageText = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bus $busId Chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = sendMessage
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            state = listState
        ) {
            items(messages) { message ->
                MessageBubble(message, isMe = message.userId == currentUserId)
            }
        }
    }
}

// ----------------------------------------
// UI Components
// ----------------------------------------

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                label = { Text("Message") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onSend,
                containerColor = Color(0xFF266FEF),
                contentColor = Color.White,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean) {
    val bubbleColor = if (isMe) Color(0xFF266FEF) else Color(0xFFE0E0E0)
    val contentColor = if (isMe) Color.White else Color.Black
    val horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    val bubbleShape = if (isMe)
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    else
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    val roleColor = when(message.userRole.lowercase()) {
        "driver" -> Color(0xFF00C853)
        "admin" -> Color(0xFFFFA000)
        else -> Color.Gray
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Sender Info
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMe) {
                Text(
                    text = "${message.userName} (${message.userRole.replaceFirstChar { it.uppercase() }})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = roleColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Spacer(Modifier.height(2.dp))

        // Message Content Bubble
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                color = contentColor,
                modifier = Modifier.padding(10.dp)
            )
        }

        // Timestamp
        Text(
            text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}