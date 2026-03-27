# 🚌 Campus Commute | Smart Bus Tracking

> **Stop Guessing. Start Knowing.** > Real-time bus tracking and commute management system designed specifically for modern college transportation ecosystems.

[![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)

🌐 **[View Live Product Demo Website](https://campus-commute-android.vercel.app/app.html)** 

---

## 📸 App Screenshots

*(Note: Create an `assets` folder in your repository root and add your images there with the matching filenames to display them here.)*

| Driver Dashboard | Live Tracking | User Roles |
| :---: | :---: | :---: |
| <img src="https://github.com/Lakshaydang47/Campus-Commute/blob/main/WhatsApp%20Image%202025-11-30%20at%2011.31.26%20PM%20(10).jpeg" width="250" alt="App Dashboard"> | <img src="https://github.com/Lakshaydang47/Campus-Commute/blob/main/WhatsApp%20Image%202025-11-30%20at%2011.31.26%20PM%20(1).jpeg" width="250" alt="Live Tracking"> | <img src="https://github.com/Lakshaydang47/Campus-Commute/blob/main/WhatsApp%20Image%202025-11-30%20at%2011.31.27%20PM.jpeg" width="250" alt="User Roles"> |

---

## ✨ Core Ecosystem Features

* 📍 **Live Bus Tracking:** Instant GPS streaming from driver devices to the Firebase Realtime Database with minimal latency.
* ⏱️ **Smart ETA:** Automated "3 Minutes Away" alerts and dynamic arrival predictions powered by the Google Directions API.
* 🚨 **Emergency SOS:** Instant notification of accidents, breakdowns, or delays to the Administration and Parents to ensure student safety.
* ☁️ **Cloud Native:** 100% real-time visibility with zero wait-time anxiety.

## 👥 Multi-Role Architecture

Campus Commute offers tailored interfaces and functionalities for 4 distinct user roles:

1.  🎓 **Student / Teacher:** View live bus locations, access digital boarding passes, and save favorite campus routes.
2.  🪪 **Driver:** Manage trip start/end times, report real-time traffic incidents, and manage passenger counting.
3.  👨‍👩‍👧 **Parent:** Monitor their child's daily commute safety and receive instant geographical arrival alerts.
4.  🛡️ **Admin:** Manage the entire bus fleet, optimize routes, oversee driver assignments, and view system analytics via a dedicated panel.

---

## 🛠️ Technology Stack

**Mobile Application:**
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose
* **Maps & Navigation:** Google Maps API & Google Directions API

**Backend & Infrastructure:**
* **Database:** Firebase Realtime Database & Cloud Firestore
* **Server/Admin Panel:** Node.js / MERN Stack

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (or latest)
* Node.js installed for the admin backend
* A Firebase Project with Realtime Database enabled
* Google Maps API Key

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/yourusername/campus-commute.git](https://github.com/yourusername/campus-commute.git)
    ```
2.  **Setup Firebase:**
    * Add your `google-services.json` file to the `app/` directory.
3.  **Setup Google Maps:**
    * Insert your API key in the `local.properties` or `AndroidManifest.xml` as configured in the project.
4.  **Build and Run:**
    * Sync Gradle files and run the application on an emulator or physical Android device.

---

## 👨‍💻 Developed By

**Lakshay Dang**
B.Tech CSE (Batch 2022-26)  
*Panipat Institute of Engineering and Technology (PIET)*

---
*© 2026 Campus Commute Project. All rights reserved.*
