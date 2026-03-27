package com.devdroid.campuscommute.data

// In a data file (e.g., CampusData.kt)
data class CamStop(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

val CAMPUS_STOP_POINTS = listOf(
    // --- Primary Access Points ---
    CamStop("main_gate", "Main Gate Entrance", "The security check point at the college entrance", 29.211358, 77.017815),
    CamStop("admin_bldg", "Admin Block", "Parking area near the Principal's office", 29.211107, 77.017321),

    // --- Academic/Departmental Buildings ---
    CamStop("cse_dept", "Staring", "Near the Computer Science Department building", 29.210777, 77.017313),
    CamStop("mech_bldg", "Old Canteen", "Drop-off point by the Engineering workshops", 29.210367, 77.017213),
    CamStop("lib_drop", "Near A Block", "Designated spot closest to the library entrance", 29.210363, 77.016562),

    // --- Residential/Hostel Areas ---
    CamStop("boys_hstl", "MBA Park", "Main parking and drop-off zone for Boys Hostel", 29.210369, 77.016231),
    CamStop("girls_hstl", "Outside D Block", "Designated stop outside the Girls Hostel complex", 29.210594, 77.016022),

    // --- Amenities ---
    CamStop("canteen", "APJ Auditorium", "Parking area near the main food service building", 29.210917, 77.016029),
    CamStop("sports", "E Block", "Near the stadium/indoor sports facility entrance", 29.211238, 77.016034),
    CamStop("parking_lot", "G Block", "The main general parking field", 29.211402, 77.016170),

    // --- Emergency/Specific Stops ---
    CamStop("dispensary", "B Block", "Near the medical dispensary building", 29.211398, 77.016544)
)