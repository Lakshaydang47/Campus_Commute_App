package com.devdroid.campuscommute.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun RequestNotificationPermission(
    onGranted: () -> Unit
) {
    val context = LocalContext.current

    // For Android 13+
    val permission = Manifest.permission.POST_NOTIFICATIONS

    // If below Android 13 → auto-granted
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onGranted()
        return
    }

    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted()
        } else {
            showRationale = true
        }
    }

    LaunchedEffect(true) {
        val granted = ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            launcher.launch(permission)
        } else {
            onGranted()
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = { launcher.launch(permission) }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = {}) {
                    Text("Cancel")
                }
            },
            title = { Text("Notification Permission Required") },
            text = { Text("Please allow notifications to receive live bus updates.") }
        )
    }
}
