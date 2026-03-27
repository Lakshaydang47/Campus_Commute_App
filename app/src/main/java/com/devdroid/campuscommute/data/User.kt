package com.devdroid.campuscommute.data

import com.google.firebase.firestore.PropertyName

data class User(
    // The Document ID from Firebase Auth
    val userId: String = "",

    val name: String = "",
    val email: String = "",
    val phone: String = "",

    // Role: "student", "driver", "parent", "admin"
    val role: String = "student",

    // Nullable because not everyone has one immediately
    val fcmToken: String? = null,

    // Specific to Students (will be null for Drivers/Admins)
    val rollNumber: String? = null,

    // Timestamp of when they signed up
    val registeredAt: Long = System.currentTimeMillis()
)