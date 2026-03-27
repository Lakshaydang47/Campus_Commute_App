package com.devdroid.campuscommute.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

@Composable
fun SchoolLocationComponent(
    address: String,
    website: String,
    phoneNumber: String
) {
    val pietLocation = LatLng(28.8391, 76.8987)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pietLocation, 16f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "PIET Location",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // REAL GOOGLE MAP VIEW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = pietLocation),
                    title = "PIET College",
                    snippet = "Panipat Institute of Engineering & Technology"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = address,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Website", tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = website,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.clickable { /* open using intent */ }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, contentDescription = "Phone", tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = phoneNumber, fontSize = 14.sp, color = Color.Gray)
            }

            Box(
                modifier = Modifier
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .clickable { /* call intent */ }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Tap to call", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
