package com.devdroid.campuscommute.data

/**
 * Represents a single message in the group chat.
 */
data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "", // Used for coloring/badges (student, driver, admin)
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)