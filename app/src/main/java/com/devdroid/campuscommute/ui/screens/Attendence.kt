package com.devdroid.campuscommute.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

// Helper data class for Firebase response
data class TripRecord(
    val timestamp: Long = 0L,
    val status: String = "" // e.g., "COMPLETED"
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var showReportDialog by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var isLoading by remember { mutableStateOf(true) }

    // Attendance data map: Key=Day of Month, Value=True(Present) / False(Absent)
    var attendanceData by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    val currentYearMonth = currentMonth // Capture for useEffect dependency

    // ⚡ FETCH LOGIC: Fetches trip data and maps it to the calendar grid
    LaunchedEffect(currentYearMonth) {
        isLoading = true
        val uid = auth.currentUser?.uid
        if (uid == null) { isLoading = false; return@LaunchedEffect }

        val startOfMonth = currentYearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonth = currentYearMonth.atEndOfMonth().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        db.collection("trip_history") // Assuming a collection named trip_history
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .whereLessThanOrEqualTo("timestamp", endOfMonth)
            .get()
            .addOnSuccessListener { snapshot ->
                val newAttendanceData = mutableMapOf<Int, Boolean>()
                val daysInMonth = currentYearMonth.lengthOfMonth()

                // Initialize all weekdays as ABSENT (False) first
                for (day in 1..daysInMonth) {
                    val date = currentYearMonth.atDay(day)
                    // Only mark weekdays as potentially required attendance days
                    if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                        newAttendanceData[day] = false
                    }
                }

                // Mark Present based on actual trips
                snapshot.documents.forEach { doc ->
                    val timestamp = doc.getLong("timestamp") ?: return@forEach
                    val tripDate = java.time.Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    if (tripDate.month == currentYearMonth.month && tripDate.year == currentYearMonth.year) {
                        // Mark the specific day as Present (True)
                        newAttendanceData[tripDate.dayOfMonth] = true
                    }
                }

                attendanceData = newAttendanceData.toMap()
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load history.", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.Email, contentDescription = "Download Report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9FAFB)) // Consistent soft gray background
                .padding(16.dp)
        ) {
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF4CAF50), label = "Present")
                Spacer(modifier = Modifier.width(24.dp))
                LegendItem(color = Color(0xFFFF5252), label = "Absence")
                Spacer(modifier = Modifier.width(24.dp))
                LegendItem(color = Color(0xFFBDBDBD), label = "Weekend/Holiday") // Grey
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Card (Using calculated data)
            val presentCount = attendanceData.count { it.value == true }
            val absenceCount = attendanceData.count { it.value == false }
            val totalWorkingDays = attendanceData.size // Only includes weekdays

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Present Days", value = presentCount.toString(), color = Color(0xFF4CAF50))
                        StatItem(label = "Absence Days", value = absenceCount.toString(), color = Color(0xFFFF5252))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Total Working Days", fontSize = 14.sp, color = Color.Gray)
                    Text(text = "$totalWorkingDays Days", fontSize = 18.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentYearMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row {
                            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                            }
                            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        CalendarView(
                            yearMonth = currentYearMonth,
                            attendanceData = attendanceData
                        )
                    }
                }
            }
        }

        if (showReportDialog) {
            AttendanceReportDialog(onDismiss = { showReportDialog = false })
        }
    }
}

// --- Helper Composable Functions ---

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 14.sp)
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarView(yearMonth: YearMonth, attendanceData: Map<Int, Boolean>) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek.value % 7 // 0=Sunday, 6=Saturday (For mapping)

    Column {
        // Day headers (Starting with Sunday)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (day == "S") Color(0xFFFF9800) else Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        var dayCounter = 1
        for (week in 0..5) {
            if (dayCounter > daysInMonth) break

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (dayOfWeek in 0..6) {
                    if ((week == 0 && dayOfWeek < firstDayOfMonth) || dayCounter > daysInMonth) {
                        // Empty box for spacing
                        Box(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        val date = yearMonth.atDay(dayCounter)
                        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

                        val isPresent = if (isWeekend) false else attendanceData[dayCounter] == true
                        val isAbsent = if (isWeekend) false else attendanceData[dayCounter] == false

                        CalendarDay(
                            day = dayCounter,
                            isPresent = isPresent,
                            isAbsent = isAbsent,
                            isWeekend = isWeekend, // Pass weekend flag
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDay(
    day: Int,
    isPresent: Boolean,
    isAbsent: Boolean,
    isWeekend: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isWeekend -> Color(0xFFBDBDBD) // Grey for weekends
        isAbsent -> Color(0xFFFF5252) // Red for Absence
        isPresent -> Color(0xFFA5D6A7) // Light Green for Present
        else -> Color.Transparent // Days not reached or without data
    }

    val textColor = when {
        isWeekend || isAbsent || isPresent -> Color.White
        else -> Color.DarkGray
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.toString(), color = textColor, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportDialog(onDismiss: () -> Unit) {
    var startDate by remember { mutableStateOf("01/08/2024") }
    var endDate by remember { mutableStateOf("01/09/2024") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // ... (Report Dialog UI remains the same) ...
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Attendance Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Start Date", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startDate, onValueChange = { startDate = it }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.DateRange, "Calendar") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("End Date", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDate, onValueChange = { endDate = it }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.DateRange, "Calendar") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Download Reports", fontSize = 16.sp)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showSystemUi = true)
@Composable
private fun fdfjkd() {
    AttendanceScreen()
}