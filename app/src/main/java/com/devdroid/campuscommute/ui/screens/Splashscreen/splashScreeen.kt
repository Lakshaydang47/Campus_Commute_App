package com.devdroid.campuscommute.ui.screens.Splashscreen

import android.content.Context
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.devdroid.campuscommute.MainActivity
import com.devdroid.campuscommute.R
import com.devdroid.campuscommute.data.UserPreferences
import com.devdroid.campuscommute.data.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    context: MainActivity
) {

    LaunchedEffect(key1 = true) {

        delay(1000L) // Wait for animation

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // 2. Check Login Status
        if (currentUser != null) {

            // ⚡ KEY FIX: Load Session Data into Memory
            UserSession.loadSession { success ->

                val role = UserSession.userRole ?: UserPreferences.getUserRole(context) ?: "student"
                val cleanedRole = role.lowercase()

                // Save if missing
                if (UserPreferences.getUserRole(context) == null) {
                    UserPreferences.saveUserRole(context, cleanedRole)
                }

                // Navigate
                navController.navigate(cleanedRole) {
                    popUpTo("splash") { inclusive = true }
                }
            }

        } else {
            // 3. Not Logged In
            val prefs = context.getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
            val isFinished = prefs.getBoolean("isFinished", false)

            if (isFinished) {
                navController.navigate("role_selection") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    // Premium Gradient Background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF42A5F5),  // Accent Blue (left side)
                        Color(0xFF1E88E5),  // Mid Blue
                        Color(0xFF0D5EFC)   // Brand Blue (right side)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            // App Logo - Larger & Scales Well
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(140.dp) // bigger, scales well
                    .padding(12.dp),
                alignment = Alignment.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Name - Modern, Clean Typography
            Text(
                text = "Campus Commute",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Wide Horizontal Lottie Animation
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.loading)
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(60.dp)) // balanced spacing
        }
    }
}


private fun onBoardingIsfinished(context: MainActivity):Boolean {
    val sharedPreferences =context.getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("isFinished", false)
}

