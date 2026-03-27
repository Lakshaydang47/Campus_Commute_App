package com.devdroid.campuscommute.ui.screens.route

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdroid.campuscommute.data.Stop

@Composable
fun StopRouteItem(
    stop: Stop,
    isReached: Boolean,
    isNext: Boolean,
    isLast: Boolean,
    statusText: String, // e.g., "Picked Up | June 03, 2023" or "ETA 05 mins"
    timeText: String,    // e.g., "30 mins ago" or "ETA 05 mins"
) {
    val timelineCircleColor = when {
        isNext -> Color(0xFF266FEF) // Blue for next stop
        isReached -> Color(0xFF266FEF) // Blue for reached stops
        else -> Color.Gray // Gray for future stops
    }
    val timelineLineColor = if (isReached) Color(0xFF266FEF) else Color.LightGray

    val stopTextColor = if (isNext) Color(0xFF266FEF) else Color.Black
    val statusInfoColor = Color.Gray // Adjust if needed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top // Align content to top for longer texts
    ) {
        // Timeline Indicator
        Column(
            modifier = Modifier.width(24.dp), // Fixed width for timeline column
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(10.dp)) { // Circle for the stop
                drawCircle(color = timelineCircleColor)
            }
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .height(50.dp) // Adjust height to match line length in image
                ) {
                    drawLine(
                        color = timelineLineColor,
                        start = Offset(x = center.x, y = 0f),
                        end = Offset(x = center.x, y = size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp)) // Space between timeline and text

        // Stop Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                fontSize = 16.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                color = stopTextColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusInfoColor
            )
        }

        // Time / ETA
        Text(
            text = timeText,
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp) // Add some padding
        )
    }
}

