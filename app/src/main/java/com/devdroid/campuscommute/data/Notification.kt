package com.devdroid.campuscommute.data

data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val type: String = "info", // Used for color/icon (e.g., "info", "delay")
    val busId: String = "" // Used for filtering ("ALL" or specific bus ID like "15")
)