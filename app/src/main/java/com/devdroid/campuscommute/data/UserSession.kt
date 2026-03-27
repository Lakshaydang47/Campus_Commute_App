package com.devdroid.campuscommute.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserSession {
    // Core User Details
    var userId: String? = null
    var userName: String? = null
    var userRole: String? = null
    var assignedBusId: String? = null

    // Route Cache (So StopRouteScreen doesn't need to fetch again)
    var cachedRouteStops: List<Stop> = emptyList()

    fun clear() {
        userId = null
        userName = null
        userRole = null
        assignedBusId = null
        cachedRouteStops = emptyList()
    }

    // Load data from Firebase into Memory
    fun loadSession(onResult: (Boolean) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onResult(false)
            return
        }

        FirebaseFirestore.getInstance().collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userId = currentUser.uid
                    userName = document.getString("name")
                    userRole = document.getString("role")
                    assignedBusId = document.get("busId")?.toString()
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener { onResult(false) }
    }
}