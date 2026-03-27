package com.devdroid.campuscommute.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdroid.campuscommute.R

@Composable
fun BusInfoCard(
    busNumber: String = "03",
    licensePlate: String = "MAH 12 AR 4567",
    status: String = "Boarded"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // LEFT SIDE → icon + text
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Rounded icon background
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F1F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.busimage),
                        contentDescription = "Bus Icon",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Bus $busNumber",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = licensePlate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }

            // RIGHT SIDE → status text
            val statusColor = when (status.lowercase()) {
                "boarded" -> Color(0xFF00B894)
                "reached" -> Color(0xFF0984E3)
                "delayed" -> Color(0xFFFFA000)
                "missed" -> Color(0xFFD63031)
                else -> Color.Gray
            }

            Text(
                text = status,
                color = statusColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


