package com.devdroid.campuscommute.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdroid.campuscommute.R
import com.devdroid.campuscommute.data.UserSession
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DriverInfoSection(
    busId: String = UserSession.assignedBusId ?: "15", // Default to session if not passed
    onSeeRouteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // --- State Variables ---
    var driverName by remember { mutableStateOf("Loading...") }
    var driverPhone by remember { mutableStateOf("") }

    var adminName by remember { mutableStateOf("Loading...") }
    var adminPhone by remember { mutableStateOf("") }

    // ⚡ Fetch Driver & Admin details for this Bus
    LaunchedEffect(busId) {
        if (busId != "N/A") {
            val busIdInt = busId.toIntOrNull() ?: 0

            // 1. Fetch Driver
            db.collection("users")
                .whereEqualTo("busId", busIdInt)
                .whereEqualTo("role", "driver")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        driverName = doc.getString("name") ?: "Unknown Driver"
                        driverPhone = doc.getString("phone") ?: ""
                    } else {
                        driverName = "No Driver Assigned"
                    }
                }

            // 2. Fetch Admin (Incharge)
            db.collection("users")
                .whereEqualTo("busId", busIdInt)
                .whereEqualTo("role", "admin")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        adminName = doc.getString("name") ?: "Unknown Admin"
                        adminPhone = doc.getString("phone") ?: ""
                    } else {
                        adminName = "No Admin Assigned"
                    }
                }
        }
    }

    // --- Helper to make call ---
    fun makeCall(phoneNumber: String) {
        if (phoneNumber.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 5.dp)
            .background(Color.White)
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        // DRIVER CARD
        PersonCard(
            name = driverName,
            role = "Bus Driver",
            phone = if (driverPhone.isNotEmpty()) driverPhone else "No Contact Info",
            avatar = R.drawable.driver, // Ensure you have a placeholder drawable
            onCallClick = { makeCall(driverPhone) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ADMIN CARD
        PersonCard(
            name = adminName,
            role = "Bus Incharge",
            phone = if (adminPhone.isNotEmpty()) adminPhone else "No Contact Info",
            avatar = R.drawable.incharge, // Ensure you have a placeholder drawable
            onCallClick = { makeCall(adminPhone) }
        )

        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
fun PersonCard(
    name: String,
    role: String,
    phone: String,
    avatar: Int,
    onCallClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // PROFILE IMAGE
        // Note: For a real app, use Coil/Glide to load 'avatar' if it was a URL.
        // Here we stick to the resource ID as per your request.
        Image(
            painter = painterResource(id = avatar),
            contentDescription = null,
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {

            // NAME + VERIFIED
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Verified Icon
                Image(
                    painter = painterResource(id = R.drawable.verify),
                    contentDescription = "Verified",
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = role,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = phone,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // CALL BUTTON
        Box(
            modifier = Modifier
                .size(width = 70.dp, height = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF5F5F5)) // Light gray bg for button
                .clickable { onCallClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.phone_call),
                contentDescription = "Call",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DriverInfoSectionPreview() {
    DriverInfoSection(busId = "56")
}