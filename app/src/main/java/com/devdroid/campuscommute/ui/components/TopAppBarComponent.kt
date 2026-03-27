package com.devdroid.campuscommute.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopAppBarComponent(
    modifier: Modifier = Modifier,
    userName: String = "Student",
    busNumber: String = "No Bus Assigned", // Note: This should receive "Bus No. 56" or similar
    notificationCount: Int = 0,
    onMenuClick: () -> Unit, // 👈 Triggers Drawer Opening
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit
) {
    // 🌅 Greeting logic
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
    val currentDate = dateFormat.format(calendar.time)

    // 🎨 Gradient for top bar
    val gradientBrush = if (isSystemInDarkTheme()) {
        Brush.horizontalGradient(listOf(Color(0xFF1A3E8B), Color(0xFF264B9E)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF266FEF), Color(0xFF3B82F6)))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .background(
                brush = gradientBrush,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            // Adjusted padding to respect status bar space (top 48dp)
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 24.dp)
    ) {
        // --- ROW 1: Menu, Greeting, Notifications ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Menu Icon & Greeting Container
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Menu Icon (Triggers Drawer)
                IconButton(onClick = onMenuClick, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Greeting Text
                Column {
                    Text("$greeting 👋", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(currentDate, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // 2. Notification Bell (Triggers Navigation)
            BadgedBox(
                badge = {
                    if (notificationCount > 0) {
                        Badge(containerColor = Color.Red, contentColor = Color.White) {
                            Text(text = notificationCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onNotificationClick() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ROW 2: Profile Image & User Info ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(12.dp)
                .clickable { onProfileClick() }
        ) {
            // Profile Image (Icon placeholder)
            Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, "Profile", tint = Color(0xFF2563EB), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = busNumber,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}