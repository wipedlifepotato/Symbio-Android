package world.wipedlifepotato.symbioandroidapp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreelanceMenu(navController: NavController) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Freelance System",
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            MenuColumn(navController, listOf("Tasks" to "Tasks", "Tickets" to "Tickets"))
            MenuColumn(navController, listOf("Disputes" to "Disputes", "Chats" to "Chats"))
            MenuColumn(navController, listOf("Profiles" to "Profiles", "Profile" to "Profile"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuColumn(
    navController: NavController,
    items: List<Pair<String, String>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (label, route) ->
            Button(
                onClick = { navController.navigate(route) },
                modifier =
                    Modifier
                        .width(140.dp)
                        .height(56.dp),
                shape = MaterialTheme.shapes.medium, // красивые скругления
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    profile: JsonObject?,
    wallet: JsonObject?,
    loading: Boolean,
    errorMessage: String,
    token: String,
    onLogout: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var sendLoading by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var sendSuccess by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                // Profile Card
                profile?.let {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Welcome, ${it["username"]?.jsonPrimitive?.content ?: "User"}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Wallet Card
                wallet?.let {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Wallet,
                                    contentDescription = "Wallet",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Bitcoin Wallet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val address = it["address"]?.jsonPrimitive?.content ?: "N/A"
                            Text(
                                "Address: $address",
                                modifier =
                                    Modifier.clickable {
                                        if (address != "N/A" && address.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(address))
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Address copied",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        } else {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "No address available",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }
                                    },
                            )
                            Text("Balance: ${it["balance"]?.jsonPrimitive?.content ?: "0"} BTC")
                        }
                    }
                }

                // Send Money Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Send Bitcoin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = recipient,
                            onValueChange = { recipient = it },
                            label = { Text("Recipient Address") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount (BTC)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (recipient.isNotBlank() && amount.isNotBlank()) {
                                    coroutineScope.launch {
                                        sendLoading = true
                                        sendError = null
                                        sendSuccess = null
                                        val (success, response) =
                                            sendBitcoin(
                                                recipient,
                                                amount,
                                                token,
                                            )
                                        sendLoading = false
                                        if (success) {
                                            sendSuccess = "Transaction sent successfully!"
                                            recipient = ""
                                            amount = ""
                                        } else {
                                            Log.d("ErrorToSend", response.toString())
                                            sendError =
                                                when (response) {
                                                    is JsonObject ->
                                                        response["error"]?.jsonPrimitive?.content
                                                            ?: "Send failed"

                                                    is String -> response
                                                    else -> "Send failed"
                                                }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !sendLoading && recipient.isNotBlank() && amount.isNotBlank(),
                        ) {
                            if (sendLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Text("Send")
                            }
                        }
                        sendError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        sendSuccess?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } // End Send Card
                FreelanceMenu(navController)
            }
        }
    }
}
