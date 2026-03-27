package com.devdroid.campuscommute.ui.screens.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.campuscommute.data.UserSession
import com.devdroid.campuscommute.utils.QRCodeUtils
import com.google.firebase.auth.FirebaseAuth

@Composable
fun StudentBoardingPassScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()

    // Use User ID or Roll Number as the unique token
    val uniqueToken = auth.currentUser?.uid ?: "UNKNOWN"
    val studentName = UserSession.userName ?: "Student"

    // Generate QR
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uniqueToken) {
        qrBitmap = QRCodeUtils.generateQRCode(uniqueToken)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF266FEF)) // Blue Background
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Boarding Pass",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    CircularProgressIndicator()
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(studentName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Scan this to board", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF266FEF))
        ) {
            Text("Close")
        }
    }
}