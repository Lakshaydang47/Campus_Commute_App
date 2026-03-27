package com.devdroid.campuscommute.utils

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object UsageMonitor {
    private const val SAFE_LIMIT = 900   // ~1 write per min whole day
    var writesToday by mutableStateOf(0)

    fun canWrite(): Boolean = writesToday < SAFE_LIMIT
    fun recordWrite() { writesToday++ }
}
