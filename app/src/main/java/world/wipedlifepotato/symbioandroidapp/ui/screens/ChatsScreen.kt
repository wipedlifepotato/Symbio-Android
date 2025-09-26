package world.wipedlifepotato.symbioandroidapp.ui.screens

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.getUsernameById
import world.wipedlifepotato.symbioandroidapp.networkRequest

@Composable
fun ChatScreen(navController: NavHostController, token: String) {
    val coroutineScope = rememberCoroutineScope()
    var chatRequests by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var chatRooms by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selectedChat by remember { mutableStateOf<JsonObject?>(null) }
    var messages by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var newMessage by remember { mutableStateOf("") }

    fun loadChatRequests() {
        coroutineScope.launch {
            val (success, data) = networkRequest("/api/chat/getChatRequests", emptyMap(), token, "GET")
            if (success && data != null && data is kotlinx.serialization.json.JsonArray) {
                chatRequests = data.jsonArray.map { it.jsonObject }
            }
        }
    }

    fun loadChatRooms() {
        coroutineScope.launch {
            val (success, data) = networkRequest("/api/chat/getChatRoomsForUser", emptyMap(), token, "GET")
            if (success && data != null && data is kotlinx.serialization.json.JsonArray) {
                chatRooms = data.jsonArray.map { it.jsonObject }
            } else {
                errorMessage = "Failed to load chats"
            }
        }
    }

    fun loadMessages(chatId: Int) {
        coroutineScope.launch {
            val (success, data) = networkRequest("/api/chat/getChatMessages?chat_room_id=$chatId", emptyMap(), token, "GET")
            if (success && data != null && data is kotlinx.serialization.json.JsonArray) {
                messages = data.jsonArray.map { it.jsonObject }
            } else {
                errorMessage = "Failed to load messages"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadChatRequests()
        loadChatRooms()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chats", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedChat != null) {
            Text("Chat: ${selectedChat!!["name"]?.jsonPrimitive?.content ?: "Unknown"}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(messages) { msg ->
                    ChatMessageItem(msg, token)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    if (newMessage.isNotEmpty()) {
                        coroutineScope.launch {
                            val chatId = selectedChat!!["id"]?.jsonPrimitive?.int ?: 0
                            val (success, _) = networkRequest("/api/chat/sendMessage?chat_room_id=$chatId", mapOf(
                                "message" to newMessage
                            ), token)
                            if (success) {
                                newMessage = ""
                                loadMessages(chatId)
                            } else {
                                errorMessage = "Failed to send message"
                            }
                        }
                    }
                }) {
                    Text("Send")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        val chatId = selectedChat!!["id"]?.jsonPrimitive?.int ?: 0
                        val (success, _) = networkRequest("/api/chat/exitFromChat?chat_room_id=$chatId", emptyMap(), token)
                        if (success) {
                            selectedChat = null
                            messages = emptyList()
                            loadChatRooms()
                        } else {
                            errorMessage = "Failed to exit chat"
                        }
                    }
                }) {
                    Text("Exit Chat")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { selectedChat = null; messages = emptyList() }) {
                    Text("Back")
                }
            }
        } else {
            Text("Chat Requests", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(0.5f)) {
                items(chatRequests) { request ->
                    ChatRequestItem(request, token, coroutineScope) {
                        loadChatRequests()
                        loadChatRooms()
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("My Chats", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(0.5f)) {
                items(chatRooms) { room ->
                    ChatRoomItem(room) {
                        selectedChat = room
                        loadMessages(room["id"]?.jsonPrimitive?.int ?: 0)
                    }
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("dashboard") }) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
fun ChatRequestItem(request: JsonObject, token: String, coroutineScope: kotlinx.coroutines.CoroutineScope, onAction: () -> Unit) {
    Log.d("RequestChatRequestItem", request.toString())
    val requested_id = request["requested_id"]?.jsonPrimitive?.int ?: 0
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Request from user $requested_id", modifier = Modifier.weight(1f))
            Button(onClick = {
                coroutineScope.launch {
                    networkRequest("/api/chat/acceptChatRequest?requester_id=$requested_id", emptyMap(), token, "GET")
                    onAction()
                }
            }) {
                Text("Accept")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = {
                coroutineScope.launch {
                    networkRequest("/api/chat/cancelChatRequest?requester_id=$requested_id", emptyMap(), token, "GET")
                    onAction()
                }
            }) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun ChatRoomItem(room: JsonObject, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(room["name"]?.jsonPrimitive?.content ?: "Chat", style = MaterialTheme.typography.titleSmall)
            Button(onClick = onClick) {
                Text("Open")
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun ChatMessageItem(msg: JsonObject, token: String) {
    Log.d("Msg", msg.toString())
    val sender = msg["sender_id"]?.jsonPrimitive?.content ?: "Unknown"
    val text = msg["message"]?.jsonPrimitive?.content ?: ""
    val isImage = isBase64Image(text)
    var senderName by remember { mutableStateOf("Loading...") }
    var decodedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(sender) {
        senderName = getUsernameById(sender.toInt(), token)
    }

    LaunchedEffect(text) {
        if (isImage) {
            try {
                val base64Data = if (text.startsWith("data:image")) {
                    text.substringAfter(",")
                } else {
                    text
                }
                val decodedBytes = Base64.decode(base64Data)
                decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(senderName, style = MaterialTheme.typography.bodySmall)
            if (isImage && decodedBitmap != null) {
                Image(
                    bitmap = decodedBitmap!!.asImageBitmap(),
                    contentDescription = "Image",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            } else {
                Text(text)
            }
        }
    }
}