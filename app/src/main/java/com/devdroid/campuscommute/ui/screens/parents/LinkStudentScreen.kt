//package com.devdroid.campuscommute.ui.screens.parent
//
//import android.widget.Toast
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.devdroid.campuscommute.data.UserPreferences
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun LinkStudentScreen(navController: NavController) {
//
//    val context = LocalContext.current
//    val db = remember { FirebaseFirestore.getInstance() }
//    val auth = remember { FirebaseAuth.getInstance() }
//    val currentParentUID = auth.currentUser?.uid ?: ""
//
//    var studentIdInput by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(false) }
//    var linkStatus by remember { mutableStateOf("LOADING") }   // LOADING / NOT_LINKED / PENDING / APPROVED
//
//    // -------------------------------
//    // CHECK CURRENT STATUS
//    // -------------------------------
//    suspend fun checkCurrentStatus() {
//        if (currentParentUID.isEmpty()) {
//            linkStatus = "NOT_LINKED"
//            return
//        }
//
//        // 1. Quick cache check
//        val cachedStatus = UserPreferences.getLinkStatus(context)
//        val cachedBus = UserPreferences.getBusId(context)
//
//        if (cachedStatus == "APPROVED" && !cachedBus.isNullOrEmpty()) {
//            linkStatus = "APPROVED"
//            navController.navigate("parent") {
//                popUpTo("parent_link") { inclusive = true }
//            }
//            return
//        }
//
//        // 2. Firebase fallback
//        try {
//            val doc = db.collection("users").document(currentParentUID).get().await()
//            val firebaseStatus = doc.getString("linkStatus")
//            val busId = doc.get("busId")?.toString()
//
//            if (firebaseStatus == "approved" && !busId.isNullOrEmpty()) {
//                UserPreferences.saveLinkStatus(context, "APPROVED", busId)
//
//                linkStatus = "APPROVED"
//                navController.navigate("parent") {
//                    popUpTo("parent_link") { inclusive = true }
//                }
//                return
//            }
//
//            linkStatus = if (firebaseStatus == "pending") "PENDING" else "NOT_LINKED"
//
//        } catch (e: Exception) {
//            linkStatus = "NOT_LINKED"
//            Toast.makeText(context, "Error fetching status.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    LaunchedEffect(Unit) {
//        isLoading = true
//        checkCurrentStatus()
//        isLoading = false
//    }
//
//    // -------------------------------
//    // SUBMIT LINK REQUEST
//    // -------------------------------
//    fun submitLinkRequest() {
//        if (studentIdInput.isBlank()) {
//            Toast.makeText(context, "Enter child's Roll Number.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        isLoading = true
//
//        CoroutineScope(Dispatchers.Main).launch {
//            try {
//                // 1. Find student
//                val studentSnap = db.collection("users")
//                    .whereEqualTo("rollNumber", studentIdInput)
//                    .whereEqualTo("role", "student")
//                    .limit(1)
//                    .get().await()
//
//                if (studentSnap.isEmpty) {
//                    Toast.makeText(context, "Student not found.", Toast.LENGTH_SHORT).show()
//                    linkStatus = "NOT_LINKED"
//                    return@launch
//                }
//
//                val studentDoc = studentSnap.documents.first()
//                val busId = studentDoc.get("busId")?.toString()
//
//                if (busId.isNullOrEmpty()) {
//                    Toast.makeText(context, "Student is not assigned a bus yet.", Toast.LENGTH_SHORT).show()
//                    linkStatus = "NOT_LINKED"
//                    return@launch
//                }
//
//                // 2. Update parent's Firestore document
//                db.collection("users").document(currentParentUID).update(
//                    mapOf(
//                        "linkedStudentId" to studentIdInput,
//                        "linkedStudentUID" to studentDoc.id,
//                        "busId" to busId.toIntOrNull(),
//                        "linkStatus" to "approved"
//                    )
//                ).await()
//
//                // 3. Save cache
//                UserPreferences.saveLinkStatus(context, "APPROVED", busId)
//
//                Toast.makeText(context, "Linked successfully!", Toast.LENGTH_LONG).show()
//
//                // 4. Navigate
//                navController.navigate("parent") {
//                    popUpTo("parent_link") { inclusive = true }
//                }
//
//            } catch (e: Exception) {
//                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//            } finally {
//                isLoading = false
//            }
//        }
//    }
//
//    // -------------------------------
//    // UI
//    // -------------------------------
//    Scaffold(
//        topBar = { TopAppBar(title = { Text("Link Child") }) }
//    ) { padding ->
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .verticalScroll(rememberScrollState())
//                .padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            Text(
//                "Track Your Child’s Bus",
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(Modifier.height(8.dp))
//
//            Text(
//                "Enter your child's Roll Number to enable live tracking.",
//                color = Color.Gray
//            )
//
//            Spacer(Modifier.height(40.dp))
//
//            when (linkStatus) {
//
//                "LOADING" -> {
//                    CircularProgressIndicator()
//                    Spacer(Modifier.height(12.dp))
//                    Text("Checking status...")
//                }
//
//                "APPROVED" -> {
//                    StatusCard(
//                        icon = Icons.Default.CheckCircle,
//                        title = "Link Approved!",
//                        subtitle = "Redirecting...",
//                        color = Color(0xFF4CAF50)
//                    )
//                }
//
//                "PENDING" -> {
//                    StatusCard(
//                        icon = Icons.Default.HourglassEmpty,
//                        title = "Request Pending",
//                        subtitle = "Your request is under review.",
//                        color = Color(0xFFFFC107)
//                    )
//                }
//
//                else -> {   // NOT_LINKED
//                    LinkInputUI(
//                        studentIdInput = studentIdInput,
//                        onStudentIdChange = { studentIdInput = it },
//                        onSubmit = { submitLinkRequest() },
//                        isLoading = isLoading
//                    )
//                }
//            }
//        }
//    }
//}
//
//// ------------------ COMPONENTS ------------------
//
//@Composable
//fun StatusCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, color: Color) {
//    Card(
//        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(icon, null, tint = color, modifier = Modifier.size(40.dp))
//            Spacer(Modifier.height(12.dp))
//            Text(title, fontWeight = FontWeight.Bold)
//            Text(subtitle, fontSize = 14.sp, color = Color.Gray)
//        }
//    }
//}
//
//@Composable
//fun LinkInputUI(
//    studentIdInput: String,
//    onStudentIdChange: (String) -> Unit,
//    onSubmit: () -> Unit,
//    isLoading: Boolean
//) {
//    OutlinedTextField(
//        value = studentIdInput,
//        onValueChange = onStudentIdChange,
//        label = { Text("Child’s Roll Number") },
//        leadingIcon = { Icon(Icons.Default.Badge, null) },
//        modifier = Modifier.fillMaxWidth(),
//        singleLine = true,
//        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
//    )
//
//    Spacer(Modifier.height(24.dp))
//
//    Button(
//        onClick = onSubmit,
//        enabled = !isLoading,
//        modifier = Modifier.fillMaxWidth().height(50.dp),
//        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF266FEF))
//    ) {
//        if (isLoading) {
//            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
//        } else {
//            Text("Enable Tracking", fontSize = 18.sp)
//        }
//    }
//}
