package com.devdroid.campuscommute

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.devdroid.campuscommute.navigation.NavGraph
import com.devdroid.campuscommute.ui.theme.CampusCommuteTheme
import com.google.firebase.FirebaseApp
import androidx.compose.ui.platform.LocalContext
import com.devdroid.campuscommute.utils.FcmTokenManager
import com.pusher.pushnotifications.PushNotifications


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_CampusCommute)

        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)

        FcmTokenManager.updateToken()

        setContent {
            CampusCommuteTheme {
                val navController = rememberNavController()

                val context = LocalContext.current

                NavGraph(navController,context as MainActivity )

            }
        }
    }
}
