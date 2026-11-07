package com.music.privatechat

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.security.SecureRandom

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
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences("music_pair", Context.MODE_PRIVATE)
    }

    var language by remember { mutableStateOf("বাংলা") }
    var myRole by remember { mutableStateOf("ADMIN") }
    var pairId by remember { mutableStateOf(prefs.getString("pairId", "") ?: "") }
    var pairCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Firebase connecting...") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var showPair by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun savePair(code: String) {
        pairId = code
        prefs.edit().putString("pairId", code).apply()
        showPair = false
        status = "Phone connected successfully! / ফোন যুক্ত হয়েছে!"
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    status = "Signed in / লগইন হয়েছে"
                }
                .addOnFailureListener {
                    status = "Login failed: ${it.localizedMessage}"
                }
        } else {
            status = "Signed in / লগইন হয়েছে"
        }
    }

    DisposableEffect(pairId) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null

        if (pairId.isNotBlank()) {
            registration = db.collection("pairs")
                .document(pairId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        status = error.localizedMessage ?: "Chat error"
                        return@addSnapshotListener
                    }

                    messages = snap?.documents?.map {
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
            registration?.remove()
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

            Text(
                status,
                color = Color(0xFFB9A9FF),
                fontSize = 11.sp
            )

            if (pairId.isNotBlank()) {
                Text(
                    "Connected code: $pairId",
                    color = Color(0xFF8AB8FF),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(10.dp))

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
                        status = "Voice calling is not implemented yet."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, null)
                    Text(" Audio")
                }

                OutlinedButton(
                    onClick = {
                        status = "Video calling is not implemented yet."
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

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { item ->
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("মেসেজ লিখুন… / Type a message")
                    },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        val uid = auth.currentUser?.uid

                        if (uid == null) {
                            status = "Please wait for login"
                        } else if (pairId.isBlank()) {
                            status = "Pair phones first"
                        } else if (message.isNotBlank()) {
                            val body = message

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
                                    status = it.localizedMessage ?: "Send failed"
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

        if (showPair) {
            AlertDialog(
                onDismissRequest = {
                    if (!busy) showPair = false
                },
                title = {
                    Text("Pair phones / ফোন যুক্ত করুন")
                },
                text = {
                    Column {
                        Text("Role / ভূমিকা")

                        Row {
                            TextButton(
                                onClick = { myRole = "ADMIN" },
                                enabled = !busy
                            ) {
                                Text("Shawon Admin")
                            }

                            TextButton(
                                onClick = { myRole = "MEMBER" },
                                enabled = !busy
                            ) {
                                Text("Fatema Member")
                            }
                        }

                        OutlinedTextField(
                            value = pairCode,
                            onValueChange = {
                                pairCode = it.filter(Char::isDigit).take(6)
                            },
                            label = {
                                Text("6-digit pairing code")
                            },
                            enabled = !busy
                        )

                        Text(
                            if (myRole == "ADMIN")
                                "Shawon: Create a new code."
                            else
                                "Fatema: Enter Shawon's code."
                        )

                        if (busy) {
                            Spacer(Modifier.height(10.dp))
                            CircularProgressIndicator()
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            val uid = auth.currentUser?.uid

                            if (uid == null) {
                                status = "Please wait for login"
                                return@TextButton
                            }

                            busy = true

                            if (myRole == "ADMIN") {

                                val code = SecureRandom()
                                    .nextInt(900000) + 100000

                                val newCode = code.toString()

                                db.collection("pairs")
                                    .document(newCode)
                                    .get()
                                    .addOnSuccessListener { existing ->
                                        if (existing.exists()) {
                                            busy = false
                                            status = "Try again"
                                        } else {
                                            db.collection("pairs")
                                                .document(newCode)
                                                .set(
                                                    mapOf(
                                                        "adminUid" to uid,
                                                        "memberUid" to "",
                                                        "status" to "WAITING"
                                                    )
                                                )
                                                .addOnSuccessListener {
                                                    pairCode = newCode
                                                    savePair(newCode)
                                                    busy = false
                                                }
                                                .addOnFailureListener {
                                                    busy = false
                                                    status =
                                                        it.localizedMessage
                                                            ?: "Create failed"
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        busy = false
                                        status =
                                            it.localizedMessage ?: "Error"
                                    }

                            } else {

                                if (pairCode.length != 6) {
                                    busy = false
                                    status = "Enter a 6-digit code"
                                    return@TextButton
                                }

                                db.collection("pairs")
                                    .document(pairCode)
                                    .get()
                                    .addOnSuccessListener { document ->

                                        if (!document.exists()) {
                                            busy = false
                                            status = "Code not found"
                                            return@addOnSuccessListener
                                        }

                                        val currentStatus =
                                            document.getString("status")

                                        val adminUid =
                                            document.getString("adminUid")

                                        val memberUid =
                                            document.getString("memberUid")

                                        if (
                                            currentStatus != "WAITING" ||
                                            adminUid == uid ||
                                            !memberUid.isNullOrEmpty()
                                        ) {
                                            busy = false
                                            status = "Code already used or invalid"
                                            return@addOnSuccessListener
                                        }

                                        db.collection("pairs")
                                            .document(pairCode)
                                            .update(
                                                mapOf(
                                                    "memberUid" to uid,
                                                    "status" to "CONNECTED"
                                                )
                                            )
                                            .addOnSuccessListener {
                                                savePair(pairCode)
                                                busy = false
                                            }
                                            .addOnFailureListener {
                                                busy = false
                                                status =
                                                    it.localizedMessage
                                                        ?: "Pair failed"
                                            }
                                    }
                                    .addOnFailureListener {
                                        busy = false
                                        status =
                                            it.localizedMessage ?: "Join failed"
                                    }
                            }
                        }
                    ) {
                        Text(
                            if (myRole == "ADMIN") "Create code" else "Join"
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPair = false },
                        enabled = !busy
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
}n-iFF
