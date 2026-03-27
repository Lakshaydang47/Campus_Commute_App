package com.devdroid.campuscommute.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LocalBusNotification(
    val id: String,              // unique UUID
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
