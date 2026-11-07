package com.music.privatechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlin.random.Random

private val Ink = Color(0xFF101022)
private val Purple = Color(0xFF8B5CF6)
private val Blue = Color(0xFF3987FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            MusicApp()
        }
    }
}

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val sentAt: Long = 0
)

@Composable
fun MusicApp() {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var language by remember { mutableStateOf("বাংলা") }
    var myRole by remember { mutableStateOf("ADMIN") }

    var pairId by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Firebase connecting...") }

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }

    var showPair by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }

    // Firebase Anonymous Login
    LaunchedEffect(Unit) {

        if (auth.currentUser == null) {

            auth.signInAnonymously()
                .addOnSuccessListener {
                    status = "Firebase connected / লগইন হয়েছে"
                }
                .addOnFailureListener {
                    status = "Login failed: ${it.localizedMessage}"
                }

        } else {
            status = "Firebase connected / লগইন হয়েছে"
        }
    }

    // Create a unique 6 digit pairing code
    fun createPair(attempt: Int = 0) {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            status = "Please wait for Firebase login"
            return
        }

        if (attempt >= 10) {
            status = "Could not generate code. Try again."
            return
        }

        val code = Random.nextInt(100000, 1000000).toString()
        val ref = db.collection("pairs").document(code)

        status = "Generating pairing code..."

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

            if (snapshot.exists()) {
                false
            } else {

                transaction.set(
                    ref,
                    mapOf(
                        "adminUid" to uid,
                        "memberUid" to "",
                        "status" to "WAITING",
                        "createdAt" to System.currentTimeMillis()
                    )
                )

                true
            }

        }.addOnSuccessListener { created ->

            if (created) {

                pairCode = code
                pairId = code
                connected = false

                status = "Code created! Waiting for Fatema..."

                showPair = false

            } else {
                createPair(attempt + 1)
            }

        }.addOnFailureListener {
            status = "Code creation failed: ${it.localizedMessage}"
        }
    }

    // Join using the 6 digit code
    fun joinPair(code: String) {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            status = "Please wait for Firebase login"
            return
        }

        if (code.length != 6) {
            status = "Enter a valid 6 digit code"
            return
        }

        val ref = db.collection("pairs").document(code)

        status = "Checking pairing code..."

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

            if (!snapshot.exists()) {
                throw IllegalStateException("Wrong pairing code!")
            }

            val adminUid = snapshot.getString("adminUid") ?: ""
            val memberUid = snapshot.getString("memberUid") ?: ""

            if (adminUid == uid) {
                throw IllegalStateException("You cannot join your own code!")
            }

            if (memberUid.isNotEmpty()) {
                throw IllegalStateException("This code is already used!")
            }

            transaction.update(
                ref,
                mapOf(
                    "memberUid" to uid,
                    "status" to "CONNECTED"
                )
            )

            true

        }.addOnSuccessListener {

            pairId = code
            pairCode = code
            connected = true

            status = "Connected successfully! ❤️"

            showPair = false

        }.addOnFailureListener {

            status = it.localizedMessage ?: "Pairing failed"
        }
    }

    // Listen for pairing status and chat messages
    DisposableEffect(pairId) {

        var pairListener: ListenerRegistration? = null
        var messageListener: ListenerRegistration? = null

        if (pairId.isNotBlank()) {

            val pairRef = db.collection("pairs").document(pairId)

            pairListener = pairRef.addSnapshotListener { snapshot, error ->

                if (error != null) {
                    status = error.localizedMessage ?: "Pair status error"
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {

                    val memberUid =
                        snapshot.getString("memberUid") ?: ""

                    connected = memberUid.isNotEmpty()

                    status = if (connected) {
                        "Connected ❤️ / ফোন যুক্ত হয়েছে"
                    } else {
                        "Waiting for Fatema... / অপেক্ষা চলছে"
                    }
                }
            }

            messageListener = pairRef.collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        status = error.localizedMessage ?: "Chat error"
                        return@addSnapshotListener
                    }

                    messages = snapshot?.documents?.map {

                        ChatMessage(
                            id = it.id,
                            sender = it.getString("sender") ?: "",
                            text = it.getString("text") ?: "",
                            sentAt = it.getLong("sentAt") ?: 0L
                        )

                    } ?: emptyList()
                }
        }

        onDispose {
            pairListener?.remove()
            messageListener?.remove()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            secondary = Blue,
            background = Ink,
            surface = Color(0xFF1B1B35)
        )
    ) {

        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF17132F), Ink)
                    )
                )
                .padding(18.dp)
        ) {

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Column {

                    Text(
                        "MUSIC",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Text(
                        "PRIVATE • JUST TWO",
                        color = Color(0xFFB9A9FF),
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                }

                TextButton(
                    onClick = {
                        language =
                            if (language == "বাংলা") "English" else "বাংলা"
                    }
                ) {
                    Text(language, color = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))

            // User Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF242044)
                ),
                shape = RoundedCornerShape(22.dp)
            ) {

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Avatar("S", Purple)

                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    ) {

                        Text(
                            "Shawon",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "ADMIN",
                            color = Color(0xFFB9A9FF),
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        "↔",
                        color = Color(0xFFB9A9FF),
                        fontSize = 22.sp
                    )

                    Column(Modifier.padding(horizontal = 10.dp)) {

                        Text(
                            "ফাতেমা",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "MEMBER",
                            color = Color(0xFF8AB8FF),
                            fontSize = 11.sp
                        )
                    }

                    Avatar("ফ", Blue)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Status
            Text(
                status,
                color = if (connected) Color(0xFF7CFFA8)
                else Color(0xFFB9A9FF),
                fontSize = 12.sp
            )

            // Show generated code
            if (pairCode.isNotBlank() && myRole == "ADMIN") {

                Spacer(Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF292449)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            "YOUR PAIRING CODE",
                            color = Color(0xFFB9A9FF),
                            fontSize = 11.sp
                        )

                        Text(
                            pairCode,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 5.sp
                        )

                        Text(
                            if (connected) "Fatema connected ❤️"
                            else "Share this code with Fatema",
                            color = Color(0xFF7CFFA8),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = { showPair = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Pair / কোড")
                }

                OutlinedButton(
                    onClick = {
                        status =
                            "Voice call UI ready; WebRTC calling is not connected yet."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, null)
                    Text(" Audio")
                }

                OutlinedButton(
                    onClick = {
                        status =
                            "Video call UI ready; WebRTC calling is not connected yet."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VideoCall, null)
                    Text(" Video")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Private chat",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "শুধু Shawon ও ফাতেমা",
                color = Color(0xFFAAA5C5),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            // Messages
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(messages) { item ->

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            if (item.sender == auth.currentUser?.uid)
                                Arrangement.End
                            else Arrangement.Start
                    ) {

                        Surface(
                            color =
                                if (item.sender == auth.currentUser?.uid)
                                    Color(0xFF49327C)
                                else Color(0xFF292449),
                            shape = RoundedCornerShape(16.dp)
                        ) {

                            Text(
                                item.text,
                                Modifier.padding(12.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Send Message
            Row(verticalAlignment = Alignment.CenterVertically) {

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("মেসেজ লিখুন… / Type a message")
                    },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    enabled = connected
                )

                IconButton(
                    enabled = connected && message.isNotBlank(),
                    onClick = {

                        val uid = auth.currentUser?.uid

                        if (uid == null) {
                            status = "Please wait for sign-in"
                        } else if (pairId.isBlank()) {
                            status = "Pair phones first"
                        } else if (message.isNotBlank()) {

                            val body = message.trim()

                            db.collection("pairs")
                                .document(pairId)
                                .collection("messages")
                                .add(
                                    mapOf(
                                        "sender" to uid,
                                        "text" to body,
                                        "sentAt" to System.currentTimeMillis()
                                    )
                                )
                                .addOnSuccessListener {
                                    message = ""
                                }
                                .addOnFailureListener {
                                    status = it.localizedMessage
                                        ?: "Send failed"
                                }
                        }
                    }
                ) {

                    Icon(
                        Icons.Default.Send,
                        "Send",
                        tint = Color(0xFFB9A9FF)
                    )
                }
            }
        }

        // Pairing Dialog
        if (showPair) {

            AlertDialog(

                onDismissRequest = { showPair = false },

                title = {
                    Text("Pair phones / ফোন যুক্ত করুন")
                },

                text = {

                    Column {

                        Text("Select your role / ভূমিকা নির্বাচন করুন")

                        Row {

                            TextButton(
                                onClick = { myRole = "ADMIN" }
                            ) {
                                Text("Shawon Admin")
                            }

                            TextButton(
                                onClick = { myRole = "MEMBER" }
                            ) {
                                Text("Fatema Member")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (myRole == "ADMIN") {

                            Text(
                                "Create a new 6 digit code.",
                                color = Color.Gray
                            )

                        } else {

                            OutlinedTextField(
                                value = pairCode,
                                onValueChange = {
                                    pairCode = it.filter(Char::isDigit).take(6)
                                },
                                label = {
                                    Text("6-digit pairing code")
                                },
                                singleLine = true
                            )
                        }
                    }
                },

                confirmButton = {

                    TextButton(
                        onClick = {

                            if (myRole == "ADMIN") {
                                createPair()
                            } else {
                                joinPair(pairCode)
                            }
                        }
                    ) {

                        Text(
                            if (myRole == "ADMIN")
                                "Generate Code"
                            else "OK / Connect"
                        )
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = { showPair = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun Avatar(letter: String, color: Color) {

    Box(
        Modifier
            .size(42.dp)
            .background(
                Brush.linearGradient(listOf(color, Blue)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            letter,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}n-i
