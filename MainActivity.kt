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
import com.google.firebase.firestore.Query

private val Ink = Color(0xFF101022)
private val Purple = Color(0xFF8B5CF6)
private val Blue = Color(0xFF3987FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent { MusicApp() }
    }
}

data class ChatMessage(val id: String = "", val sender: String = "", val text: String = "", val sentAt: Long = 0)

@Composable
fun MusicApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var language by remember { mutableStateOf("বাংলা") }
    var myRole by remember { mutableStateOf("ADMIN") }
    var pairId by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Firebase setup required / Firebase সেটআপ প্রয়োজন") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var showPair by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnSuccessListener { status = "Signed in / লগইন হয়েছে" }
                .addOnFailureListener { status = "Sign-in failed: ${it.localizedMessage}" }
        } else status = "Signed in / লগইন হয়েছে"
    }

    LaunchedEffect(pairId) {
        if (pairId.isNotBlank()) {
            db.collection("pairs").document(pairId).collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snap, error ->
                    if (error != null) { status = error.localizedMessage ?: "Chat error"; return@addSnapshotListener }
                    messages = snap?.documents?.map {
                        ChatMessage(it.id, it.getString("sender") ?: "", it.getString("text") ?: "", it.getLong("sentAt") ?: 0L)
                    } ?: emptyList()
                }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, secondary = Blue, background = Ink, surface = Color(0xFF1B1B35))) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF17132F), Ink))).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("MUSIC", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("PRIVATE • JUST TWO", color = Color(0xFFB9A9FF), fontSize = 11.sp, letterSpacing = 2.sp)
                }
                TextButton(onClick = { language = if (language == "বাংলা") "English" else "বাংলা" }) { Text(language, color = Color.White) }
            }
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF242044)), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar("S", Purple)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("Shawon", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("ADMIN", color = Color(0xFFB9A9FF), fontSize = 11.sp)
                    }
                    Text("↔", color = Color(0xFFB9A9FF), fontSize = 22.sp)
                    Column(Modifier.padding(horizontal = 10.dp)) {
                        Text("ফাতেমা", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("MEMBER", color = Color(0xFF8AB8FF), fontSize = 11.sp)
                    }
                    Avatar("ফ", Blue)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(status, color = Color(0xFFB9A9FF), fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { showPair = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Pair / কোড") }
                OutlinedButton(onClick = { status = "Voice call signaling scaffold ready; WebRTC call UI/service still needs wiring." }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Call, null); Text(" Audio")
                }
                OutlinedButton(onClick = { status = "Video call signaling scaffold ready; camera permission and WebRTC flow still need wiring." }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.VideoCall, null); Text(" Video")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Private chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("শুধু Shawon ও ফাতেমা", color = Color(0xFFAAA5C5), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (item.sender == auth.currentUser?.uid) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (item.sender == auth.currentUser?.uid) Color(0xFF49327C) else Color(0xFF292449), shape = RoundedCornerShape(16.dp)) {
                            Text(item.text, Modifier.padding(12.dp), color = Color.White)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("মেসেজ লিখুন… / Type a message") }, shape = RoundedCornerShape(18.dp), singleLine = true)
                IconButton(onClick = {
                    val uid = auth.currentUser?.uid
                    if (uid == null) status = "Please wait for sign-in"
                    else if (pairId.isBlank()) status = "Pair phones first / আগে ফোন যুক্ত করুন"
                    else if (message.isNotBlank()) {
                        val body = message
                        db.collection("pairs").document(pairId).collection("messages").add(
                            mapOf("sender" to uid, "text" to body, "sentAt" to System.currentTimeMillis())
                        ).addOnSuccessListener { message = "" }
                            .addOnFailureListener { status = it.localizedMessage ?: "Send failed" }
                    }
                }) { Icon(Icons.Default.Send, "Send", tint = Color(0xFFB9A9FF)) }
            }
        }
        if (showPair) {
            AlertDialog(onDismissRequest = { showPair = false },
                title = { Text("Pair phones / ফোন যুক্ত করুন") },
                text = {
                    Column {
                        Text("Role / ভূমিকা")
                        Row {
                            TextButton(onClick = { myRole = "ADMIN" }) { Text("Shawon Admin") }
                            TextButton(onClick = { myRole = "MEMBER" }) { Text("Fatema Member") }
                        }
                        OutlinedTextField(value = pairCode, onValueChange = { pairCode = it.filter(Char::isDigit).take(6) },
                            label = { Text("6-digit pairing code") })
                        Text("Pairing must be completed through the secure Firebase callable functions included in the backend folder.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPair = false; status = "Backend pairing function must be deployed and connected before pairing." }) { Text("OK") }
                })
        }
    }
}

@Composable
fun Avatar(letter: String, color: Color) {
    Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(color, Blue)), CircleShape), contentAlignment = Alignment.Center) {
        Text(letter, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
