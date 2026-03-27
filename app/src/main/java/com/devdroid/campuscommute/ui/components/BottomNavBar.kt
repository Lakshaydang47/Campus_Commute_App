package com.devdroid.campuscommute.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.More
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

// 1. Define Items with Icons matching your image
sealed class BottomBarItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomBarItem("home", Icons.Filled.Home, Icons.Outlined.Home)

    // Route Icon (Timeline looks like the 'S' path in your image)
    object Route : BottomBarItem("tracker", Icons.Filled.Timeline, Icons.Outlined.Timeline)

    // Chat Icon
    object Chat : BottomBarItem("map_screen", Icons.AutoMirrored.Filled.Accessible, Icons.AutoMirrored.Outlined.Accessible)

    // Attendance (Calendar Icon)
    object Attendance : BottomBarItem("attendance", Icons.Filled.DateRange, Icons.Outlined.DateRange)

    // Profile Icon
    object Profile : BottomBarItem("profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun StudentBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomBarItem.Home,
        BottomBarItem.Route,
        BottomBarItem.Chat,
        BottomBarItem.Attendance,
        BottomBarItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Container with shadow and rounded top corners
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp) // Slightly taller for better touch targets
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // Evenly space the 5 icons
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                // Color Animation: Blue if selected, Gray if not
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF2563EB) else Color(0xFF9CA3AF), // Royal Blue vs Gray
                    animationSpec = tween(300),
                    label = "color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f) // Ensures equal spacing
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Removes the ripple click effect for a cleaner look
                        ) {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.route,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp) // Icon size
                    )
                }
            }
        }
    }
}