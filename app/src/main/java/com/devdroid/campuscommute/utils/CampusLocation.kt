package com.devdroid.campuscommute.utils

// Data class for a location on the static image map
data class CampusStopMapping(
    val name: String,
    // Normalized coordinates (0.0 to 1.0) for the static map image
    val normalizedX: Float,
    val normalizedY: Float
)

object CampusLocations {
    // --- MAPPED STOP NAMES TO STATIC IMAGE COORDINATES (0.0 to 1.0) ---
    // These coordinates determine where the bus icon appears on the collage_map.jpg image.
    val locations = listOf(
        // NOTE: Renamed stop descriptions for clarity in the map screen
        CampusStopMapping("Main Gate Entrance", 0.90f, 0.40f),
        CampusStopMapping("Admin Block", 0.65f, 0.55f),
        CampusStopMapping("Staring Point", 0.45f, 0.25f), // Corrected typo from "Staring" to "Staring Point"
        CampusStopMapping("Old Canteen", 0.25f, 0.60f),
        CampusStopMapping("Near A Block", 0.15f, 0.20f),
        CampusStopMapping("MBA Park", 0.85f, 0.20f),
        CampusStopMapping("Outside D Block", 0.95f, 0.15f),
        CampusStopMapping("APJ Auditorium", 0.35f, 0.85f),
        CampusStopMapping("E Block", 0.80f, 0.70f),
        CampusStopMapping("G Block", 0.50f, 0.15f),
        CampusStopMapping("B Block", 0.10f, 0.75f)
    ).associateBy { it.name }

    /**
     * Retrieves the normalized coordinates (X, Y) for a given location name.
     * This is used by CampusMapScreen to position the bus icon on the image.
     */
    fun getCoordsByName(name: String): Pair<Float, Float>? {
        val location = locations[name]
        return if (location != null) Pair(location.normalizedX, location.normalizedY) else null
    }
}