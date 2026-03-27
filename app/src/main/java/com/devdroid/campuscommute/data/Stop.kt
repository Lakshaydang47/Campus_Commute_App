package com.devdroid.campuscommute.data

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.PropertyName

/**
 * Represents a single bus stop.
 * Compatible with Firestore (requires default values).
 */
data class Stop(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val time: String = "",      // e.g., "08:30 AM"
    val sequence: Int = 0       // Order: 1, 2, 3...
) {
    // 🌍 Helper to convert to Google Maps object easily
    // @get:PropertyName ensures Firebase ignores this field when saving
    @get:PropertyName("ignore_location")
    val location: LatLng
        get() = LatLng(latitude, longitude)
}