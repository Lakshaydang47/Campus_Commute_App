package com.devdroid.campuscommute.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppDrawer(
    userName: String = "Student",  // 👈 Dynamic Name
    busId: String = "N/A",         // 👈 Dynamic Bus ID
    onItemClick: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier.width(300.dp)
    ) {
        // --- HEADER SECTION ---
        DrawerHeader(userName, busId)

        Spacer(modifier = Modifier.height(16.dp))

        // --- NAVIGATION ITEMS ---
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {

            Text(
                "Menu",
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )

            DrawerItem(Icons.Default.Home, "Home") { onItemClick("home") }
            DrawerItem(Icons.Default.Person, "My Profile") { onItemClick("profile") }
            DrawerItem(Icons.Default.Map, "Map") {
                onItemClick("map_screen/${busId}")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

            DrawerItem(Icons.AutoMirrored.Filled.Help, "Help & Support") { /* Handle Help */ }

            Spacer(modifier = Modifier.weight(1f))

            DrawerItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Logout",
                textColor = Color.Red,
                iconColor = Color.Red,
                onClick = { onItemClick("logout") } // 👈 Handle Logout click
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DrawerHeader(name: String, busId: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                )
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Bus: $busId", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    textColor: Color = Color.Black,
    iconColor: Color = Color.Gray,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(text = label, color = textColor, fontWeight = FontWeight.Medium) },
        icon = { Icon(imageVector = icon, contentDescription = null, tint = iconColor) },
        selected = false,
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(vertical = 4.dp),
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}