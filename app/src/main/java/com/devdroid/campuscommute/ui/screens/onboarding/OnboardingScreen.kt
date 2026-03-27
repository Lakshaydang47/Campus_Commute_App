package com.devdroid.campuscommute.ui.screens.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi // Required for Pager in older Compose versions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.devdroid.campuscommute.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Theming Variables ---
val PrimaryBlue = Color(0xFF007AFF)
val TextDark = Color(0xFF1C1C1E)
val TextSubtle = Color(0xFF6C6C70)

@Composable
fun OnBoardingScreen(navController: NavController, context: Context) {

    val animations = listOf(
        R.raw.onboarding1,
        R.raw.onboardind2, // Ensure this spelling matches your file name (checked yours: onboardind2)
        R.raw.onboarding3,
    )

    val titles = listOf(
        "Track Your College Bus in Real-Time",
        "Never Miss Your Bus Again",
        "Get Notified About Bus Locations"
    )

    val description = listOf(
        "Effortlessly track your college bus's location in real-time, so you always know where it is and when it will arrive.",
        "Stay informed with live updates and notifications, ensuring you never miss your bus and are always on time for your classes.",
        "Receive timely alerts about your bus's location, arrival times, and any unexpected delays, keeping you in the loop."
    )

    // FIXED: Uses lambda syntax for pageCount (AndroidX standard)
    val pagerState = rememberPagerState(pageCount = { animations.size })

    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

                // FIXED: Official HorizontalPager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { currentPage ->
                    OnBoardingPage(
                        animationResId = animations[currentPage],
                        title = titles[currentPage],
                        description = description[currentPage],
                        pagerState = pagerState
                    )
                }

                // --- TOP LEFT BACK BUTTON ---
                if (pagerState.currentPage > 0) {
                    Icon(
                        painter = painterResource(id = R.drawable.back),
                        contentDescription = "Back",
                        tint = TextDark,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(horizontal = 20.dp, vertical = 48.dp)
                            .size(16.dp)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                    )
                }

                // --- TOP RIGHT SKIP BUTTON ---
                if (pagerState.currentPage != pagerState.pageCount - 1) {
                    Text(
                        text = "Skip",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 20.dp, vertical = 40.dp)
                            .clickable {
                                onBoardingIsfinished(context = context)
                                navController.popBackStack()
                                navController.navigate("role_selection")
                            },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSubtle
                    )
                }
            }

            // Indicator and Buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    pageCount = animations.size,
                    currentPage = pagerState.currentPage,
                )
                Spacer(modifier = Modifier.height(32.dp))
                ButtonSelection(
                    pagerState = pagerState,
                    navController = navController,
                    context = context
                )
            }
        }
    }
}

@Composable
fun OnBoardingPage(
    animationResId: Int,
    title: String,
    description: String,
    pagerState: PagerState
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationResId))
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pagerState.pageCount - 1

    LaunchedEffect(key1 = pagerState.currentPage) {
        if (!isLastPage) {
            delay(5000L)
            if (pagerState.currentPage < pagerState.pageCount - 1) {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 8.dp)
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.Center)
            )
        }

        Text(
            text = title,
            textAlign = TextAlign.Justify,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
            lineHeight = 36.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            textAlign = TextAlign.Justify,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = TextSubtle,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
fun ButtonSelection(pagerState: PagerState, navController: NavController, context: Context) {

    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pagerState.pageCount - 1

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isLastPage) Arrangement.Center else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLastPage) {
            // Circular NEXT Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.forward),
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // GET STARTED Button
            Button(
                onClick = {
                    onBoardingIsfinished(context = context)
                    navController.popBackStack()
                    navController.navigate("role_selection")
                },
                modifier = Modifier
                    .width(220.dp)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "GET STARTED",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}


@Composable
fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) {
            IndicatorSingleDot(isSelected = it == currentPage)
        }
    }
}

@Composable
fun IndicatorSingleDot(isSelected: Boolean) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        label = "Indicator Width Animation"
    )
    val color by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.6f),
        label = "Indicator Color Animation"
    )

    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(color)
    )
}

// FIXED: Changed parameter to Context (broader than MainActivity)
private fun onBoardingIsfinished(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("isFinished", true)
    editor.apply()
    return sharedPreferences.getBoolean("isFinished", false)
}