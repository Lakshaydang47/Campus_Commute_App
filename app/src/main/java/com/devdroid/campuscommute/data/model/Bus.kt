package com.devdroid.campuscommute.data.model

/**
 * Represents a physical bus in the fleet.
 */
data class Bus(
    val busNumber: String = "",       // e.g., "56" (This is the ID)
    val licensePlate: String = "",    // e.g., "HR-05-AB-1234"
    val driverId: String? = null,     // UID of the assigned driver
    val adminId: String = "",         // UID of the Admin managing this bus
    val status: String = "Inactive"   // "Active", "Inactive", "Maintenance"
)