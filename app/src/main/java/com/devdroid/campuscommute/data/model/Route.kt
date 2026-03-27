package com.devdroid.campuscommute.data.model

import com.devdroid.campuscommute.data.Stop

/**
 * Represents the entire route for a specific Bus.
 * Firestore Document ID should be the 'busId' (e.g., "56").
 */
data class Route(
    val busId: String = "",
    val routeName: String = "",       // e.g., "Karnal Morning Route"
    val stops: List<Stop> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)