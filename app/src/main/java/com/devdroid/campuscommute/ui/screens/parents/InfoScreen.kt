package com.devdroid.campuscommute.ui.screens.parents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdroid.campuscommute.R
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RouteInfoScreen() {
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 3 }
    )
    val scope = rememberCoroutineScope()
    val tabs = listOf("Driver Info", "Bus Incharge", "Bus Info")

    Scaffold(
        topBar = {
            Column {
                // Top App Bar
                TopAppBar(
                    title = { Text("Route Info") },
                    navigationIcon = {
                        IconButton(onClick = { /* Handle back */ }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Handle notification */ }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )

                // Tab Row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.White,
                    contentColor = Color(0xFF007AFF)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> DriverInfoContent()
                1 -> BusInchargeContent()
                2 -> BusInfoContent()
            }
        }
    }
}

@Composable
fun DriverInfoContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Basic Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.driver),
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RamLal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(20.dp))

                InfoItem(label = "Employee ID", value = "Piet Dv 385")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Email", value = "driver@gmail.com")
                Spacer(modifier = Modifier.height(16.dp))

                ContactSection(phoneNumber = "+91 965249683")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Driving Experience", value = "8 Years")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(
                    label = "Address",
                    value = "karnal,\nKnl 132001"
                )
            }
        }

        Text(
            text = "License Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(vertical = 20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                InfoItem(label = "License Number", value = "5395784338")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Expiry Date", value = "20 March 2034")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "License Class", value = "Class 5")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun BusInchargeContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Basic Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.incharge),
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Kunal uppal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(20.dp))

                InfoItem(label = "Employee ID", value = "PIET 1 VT 220")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Email", value = "admin@gmail.com")
                Spacer(modifier = Modifier.height(16.dp))

                ContactSection(phoneNumber = "+91 862537890")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Experience", value = "10 Years")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(
                    label = "Address",
                    value = "1847 Oak Ridge Drive\nCharlotte, NC 28202"
                )
            }
        }

        Text(
            text = "Professional Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(vertical = 20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                InfoItem(label = "Assigned Bus Number(s)", value = "15")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Assigned Route(s)", value = "Karnal Hansi Chowk")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Route Number(s)", value = "15")
                Spacer(modifier = Modifier.height(16.dp))

                InfoItem(label = "Emergency Contact", value = "102")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun BusInfoContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Basic Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Bus Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.busimage),
                        contentDescription = null,
                        modifier = Modifier.height(180.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoItem(label = "Color", value = "Yellow")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        InfoItem(label = "Plate#", value = "HR 65 D 5623")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoItem(label = "Capacity", value = "40")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        InfoItem(label = "Model", value = "2022")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ContactSection(phoneNumber: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Contact Number",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = phoneNumber,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Handle message */ },
                modifier = Modifier
                    .size(45.dp)
                    .background(Color(0xFFFFA500), CircleShape)
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Message",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = { /* Handle call */ },
                modifier = Modifier
                    .size(45.dp)
                    .background(Color(0xFF4CAF50), CircleShape)
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun eff() {
    RouteInfoScreen()
}