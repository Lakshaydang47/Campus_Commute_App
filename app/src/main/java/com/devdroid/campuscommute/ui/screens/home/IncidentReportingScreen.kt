package com.devdroid.campuscommute.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape

data class IncidentType(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IncidentReportingScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    // --- Data/User Info (Placeholders) ---
    val busId = "15"
    val studentName = "Student"
    val currentUserId = auth.currentUser?.uid ?: ""

    // States
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<IncidentType?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val incidentTypes = listOf(
        IncidentType("Bus Delay", Icons.Default.Schedule),
        IncidentType("Rash Driving", Icons.Default.Speed),
        IncidentType("Safety Concern", Icons.Default.Warning),
        IncidentType("Overcrowding", Icons.Default.People),
        IncidentType("Bus Breakdown", Icons.Default.Build)
    )

    // ⚡ DEFINITIVE FIX: Define a function that performs the asynchronous task
    // and returns Unit, ensuring the Button onClick is happy.
    val submitReportAction: () -> Unit = submitReportAction@{
        if (selectedType == null || description.isBlank()) {
            Toast.makeText(context, "Please select type and add description.", Toast.LENGTH_SHORT).show()
            return@submitReportAction
        }

        isSubmitting = true

        val report = hashMapOf(
            "busId" to busId,
            "reporterName" to studentName,
            "reporterId" to currentUserId,
            "type" to selectedType!!.label,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "status" to "New"
        )

        db.collection("incidents").add(report)
            .addOnSuccessListener {
                isSubmitting = false
                Toast.makeText(context, "Report Submitted! Thank you.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
            .addOnFailureListener {
                isSubmitting = false
                Toast.makeText(context, "Failed to submit report: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }


    // --- REPLACEMENT FOR SCAFFOLD (Full Screen Layout) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {

        // 1. TOP APP BAR (Manual Hoisting + Status Bar Padding)
        CenterAlignedTopAppBar(
            title = { Text("Report Incident", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
            modifier = Modifier.statusBarsPadding()
        )

        // 2. SCROLLABLE CONTENT (Weight 1f)
        Column(
            modifier = Modifier
                .weight(1f) // Fills remaining space
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text("Select Incident Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            // Type Selection Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = spacedBy(8.dp),
                verticalArrangement = spacedBy(8.dp)
            ) {
                incidentTypes.forEach { type ->
                    ChipItem(
                        type = type,
                        isSelected = selectedType == type,
                        onClick = { selectedType = if (selectedType == type) null else type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe the issue (e.g. Bus 5 min late, driver was speeding)") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(32.dp))

        }

        // 3. SUBMIT BUTTON (Pinned to the Bottom)
        Button(
            onClick = submitReportAction, // ✅ FIXED: Calls the function returning Unit
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(50.dp),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Submit Report", fontSize = 18.sp)
            }
        }
    }
}

// --- UI Helpers ---

@Composable
fun ChipItem(type: IncidentType, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color(0xFF266FEF) else Color.White,
        contentColor = if (isSelected) Color.White else Color.Black,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF266FEF) else Color.LightGray),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(type.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(type.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}