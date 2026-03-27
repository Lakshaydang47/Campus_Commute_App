plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.devdroid.campuscommute"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.devdroid.campuscommute"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    // --- Core Android ---
    implementation(libs.androidx.core.ktx) // Removed the 3 duplicates of this
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // --- Compose UI ---
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.5.4") // For the extra icons

    // --- Navigation ---
    implementation("androidx.navigation:navigation-compose:2.8.0") // Updated to newer stable version

    // --- Firebase (BOM manages versions) ---
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // --- Auth Credentials ---
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- Google Maps & Location ---
    implementation(libs.play.services.location)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.0") // Updated for better performance

    // --- Lottie Animation ---
    implementation("com.airbnb.android:lottie-compose:6.3.0") // Updated to latest stable

    // --- System UI Controller ---
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //animated nav bar
    implementation("androidx.compose.animation:animation-graphics:1.6.0")

    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
// ZXing for QR Code generation and parsing
    implementation("com.google.zxing:core:3.5.1")

    implementation("com.google.guava:guava:31.1-android")

    implementation("com.pusher:pusher-java-client:2.4.4")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.firebase:firebase-messaging:21.1.0")
    implementation("com.pusher:push-notifications-android:1.9.0")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core:1.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation("androidx.compose.material3:material3:<M3_VERSION>")

    // Recommended for adapting UI to different screen sizes (tablets, foldables)
    implementation("androidx.compose.material3:material3-window-size-class:<M3_VERSION>")

    implementation("com.google.maps.android:android-maps-utils:2.4.0")
// Use the latest stable version


}