package com.devdroid.campuscommute.data.model

data class StopPoint(
    val id: String = "",             // Unique ID
    val name: String = "",           // "Namaste Chowk"
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val sequence: Int = 0,           // 1, 2, 3 (Order in the path)
    val arrivalTime: String = ""     // "08:30 AM"
)