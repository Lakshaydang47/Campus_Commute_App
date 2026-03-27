package com.devdroid.campuscommute.ui.screens.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

// Note: You must define the `onRefresh: () -> Unit` parameter in your parent TrackerScreen
// and pass the live location listener refresh function to this component.

@Composable
fun BusOfflineScreen(onRefresh: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(bottom = 100.dp), // Space for bottom card
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon/Illustration (Amber/Warning)
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFFFC107).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DirectionsBus, null, tint = Color(0xFFFFC107), modifier = Modifier.size(60.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Bus has yet to start!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = "The driver has not started the trip yet.", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ REFRESH BUTTON
        OutlinedButton(onClick = onRefresh, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF266FEF))) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            Spacer(Modifier.width(8.dp))
            Text("Check Driver Status")
        }
    }
}