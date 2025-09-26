package world.wipedlifepotato.symbioandroidapp.ui.screens

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.getUsernameById
import world.wipedlifepotato.symbioandroidapp.networkRequest
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun TicketScreen(navController: NavHostController, token: String) {
    val coroutineScope = rememberCoroutineScope()
    var myTickets by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selectedTicket by remember { mutableStateOf<JsonObject?>(null) }
    var ticketMessages by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var newMessage by remember { mutableStateOf("") }

    fun loadMyTickets() {
        coroutineScope.launch {
            val (success, data) = networkRequest("/api/ticket/my", emptyMap(), token, "GET")
            if (success && data != null && data is kotlinx.serialization.json.JsonArray) {
                myTickets = data.jsonArray.map { it.jsonObject }
            } else {
                errorMessage = "Failed to load tickets"
            }
        }
    }

    fun loadMessages(ticketId: Int) {
        coroutineScope.launch {
            val (success, data) = networkRequest("/api/ticket/messages?ticket_id=$ticketId", emptyMap(), token, "GET")
            if (success && data != null && data is kotlinx.serialization.json.JsonArray) {
                ticketMessages = data.jsonArray.map { it.jsonObject }
            } else {
                errorMessage = "Failed to load messages"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMyTickets()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Support Tickets", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Create new ticket
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (subject.isNotEmpty() && message.isNotEmpty()) {
                coroutineScope.launch {
                    val (success, _) = networkRequest("/api/ticket/createTicket", mapOf(
                        "subject" to subject,
                        "message" to message
                    ), token)
                    if (success) {
                        subject = ""
                        message = ""
                        loadMyTickets()
                        errorMessage = "Ticket created"
                    } else {
                        errorMessage = "Failed to create ticket"
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Create Ticket")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTicket != null) {
            Text("Ticket: ${selectedTicket!!["subject"]?.jsonPrimitive?.content}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(ticketMessages) { msg ->
                    MessageItem(msg, token)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                label = { Text("New Message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    if (newMessage.isNotEmpty()) {
                        coroutineScope.launch {
                            val ticketId = selectedTicket!!["id"]?.jsonPrimitive?.int ?: 0
                            val (success, _) = networkRequest("/api/ticket/write", mapOf(
                                "ticket_id" to ticketId.toInt(),
                                "message" to newMessage
                            ), token)
                            if (success) {
                                newMessage = ""
                                loadMessages(ticketId)
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
                        val ticketId = selectedTicket!!["id"]?.jsonPrimitive?.int ?: 0
                        val (success, _) = networkRequest("/api/ticket/close", mapOf(
                            "ticket_id" to ticketId.toInt()
                        ), token)
                        if (success) {
                            selectedTicket = null
                            ticketMessages = emptyList()
                            loadMyTickets()
                        } else {
                            errorMessage = "Failed to close ticket"
                        }
                    }
                }) {
                    Text("Close Ticket")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { selectedTicket = null; ticketMessages = emptyList() }) {
                    Text("Back")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(myTickets) { ticket ->
                    TicketItem(ticket) {
                        selectedTicket = ticket
                        loadMessages(ticket["id"]?.jsonPrimitive?.int ?: 0)
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
fun TicketItem(ticket: JsonObject, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(ticket["subject"]?.jsonPrimitive?.content ?: "No subject", style = MaterialTheme.typography.titleMedium)
            Text("Status: ${ticket["status"]?.jsonPrimitive?.content ?: "Unknown"}")
            Button(onClick = onClick) {
                Text("View")
            }
        }
    }
}
@OptIn(ExperimentalEncodingApi::class)
fun isBase64Image(text: String): Boolean {
    return try {
        // Handle data URL format: data:image/...;base64,
        val base64Data = if (text.startsWith("data:image")) {
            text.substringAfter(",")
        } else {
            text
        }
        val data = Base64.decode(base64Data)
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        bitmap != null
    } catch (e: Exception) {
        false
    }
}
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun MessageItem(msg: JsonObject, token: String) {
    val coroutineScope = rememberCoroutineScope()
    var senderName by remember { mutableStateOf("Loading...") }
    var decodedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val senderId = msg["SenderID"]?.jsonPrimitive?.int ?: 0
    val message = msg["Message"]?.jsonPrimitive?.content ?: ""
    val isImage = message.startsWith("data:image") || message.length > 1000 // simple check

    LaunchedEffect(senderId) {
        senderName = getUsernameById(senderId, token)
    }

    LaunchedEffect(message) {
        if (isImage) {
            try {
                val base64Data = if (message.startsWith("data:image")) {
                    message.substringAfter(",")
                } else {
                    message
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
                Text(message)
            }
        }
    }
}