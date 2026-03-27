package com.devdroid.campuscommute.data

import android.content.Context

object UserPreferences {
    private const val PREF_NAME = "AppPrefs"
    private const val KEY_ROLE = "user_role"

    // Save role when user logs in
    fun saveUserRole(context: Context, role: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    // Get role for Splash Screen
    fun getUserRole(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ROLE, null)
    }

    // Clear data on Logout
    fun clearUserRole(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ROLE).apply()
    }

    // In UserPreferences.kt

    // Add constants
    private const val KEY_LINK_STATUS = "link_status"
    private const val KEY_BUS_ID_CACHE = "bus_id_cache"

    // Add functions
    fun saveLinkStatus(context: Context, status: String, busId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LINK_STATUS, status)
            .putString(KEY_BUS_ID_CACHE, busId)
            .apply()
    }

    fun getLinkStatus(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LINK_STATUS, null)
    }

    fun getBusId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BUS_ID_CACHE, null)
    }
}