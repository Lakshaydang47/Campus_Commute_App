package com.devdroid.campuscommute.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.devdroid.campuscommute.data.model.LocalBusNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.notificationDataStore by preferencesDataStore("bus_notifications")

object NotificationStore {

    private val NOTIFICATION_LIST_KEY = stringPreferencesKey("notification_list")

    fun getNotifications(context: Context): Flow<List<LocalBusNotification>> {
        return context.notificationDataStore.data.map { prefs ->
            val json = prefs[NOTIFICATION_LIST_KEY] ?: "[]"
            Json.decodeFromString(json)
        }
    }

    suspend fun addNotification(context: Context, title: String, message: String) {
        context.notificationDataStore.edit { prefs ->
            val json = prefs[NOTIFICATION_LIST_KEY] ?: "[]"
            val list = Json.decodeFromString<List<LocalBusNotification>>(json).toMutableList()

            list.add(
                LocalBusNotification(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )

            prefs[NOTIFICATION_LIST_KEY] = Json.encodeToString(list)
        }
    }

    suspend fun deleteNotification(context: Context, id: String) {
        context.notificationDataStore.edit { prefs ->
            val json = prefs[NOTIFICATION_LIST_KEY] ?: "[]"
            val list = Json.decodeFromString<List<LocalBusNotification>>(json)
                .filter { it.id != id }

            prefs[NOTIFICATION_LIST_KEY] = Json.encodeToString(list)
        }
    }

    suspend fun markAsRead(context: Context, id: String) {
        context.notificationDataStore.edit { prefs ->
            val json = prefs[NOTIFICATION_LIST_KEY] ?: "[]"
            val list = Json.decodeFromString<List<LocalBusNotification>>(json)
                .map { if (it.id == id) it.copy(isRead = true) else it }

            prefs[NOTIFICATION_LIST_KEY] = Json.encodeToString(list)
        }
    }
}
