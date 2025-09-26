package world.wipedlifepotato.symbioandroidapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.networkRequest

@Composable
fun ProfilesScreen(
    navController: NavHostController,
    token: String,
) {
    val coroutineScope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var offset by remember { mutableStateOf(0) }
    val limit = 5
    var total by remember { mutableStateOf(0) }

    fun loadProfiles() {
        coroutineScope.launch {
            loading = true
            val (success, data) =
                networkRequest(
                    "/profiles?limit=$limit&offset=$offset",
                    emptyMap(),
                    token,
                    "GET",
                )
            if (success && data != null && data is JsonObject &&
                data.containsKey(
                    "profiles",
                ) && data["profiles"] !is JsonNull
            ) {
                val newProfiles = data["profiles"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
                profiles = if (offset == 0) newProfiles else profiles + newProfiles
                total = data["total"]?.jsonPrimitive?.int ?: 0
            } else {
                errorMessage = "Failed to load profiles"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProfiles()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text("Profiles", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading && profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                items(profiles) { profile ->
                    ProfileItem(profile, token, coroutineScope) { msg -> errorMessage = msg }
                }
                item {
                    if (profiles.size < total) {
                        Button(onClick = {
                            offset += limit
                            loadProfiles()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Load More")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("dashboard") }) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
fun ProfileItem(
    profile: JsonObject,
    token: String,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onMessage: (String) -> Unit,
) {
    val userId = profile["user_id"]?.jsonPrimitive?.int ?: 0
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                profile["username"]?.jsonPrimitive?.content ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(profile["full_name"]?.jsonPrimitive?.content ?: "")
            Text(profile["bio"]?.jsonPrimitive?.content ?: "")
            Button(onClick = {
                coroutineScope.launch {
                    val (success, _) =
                        networkRequest(
                            "/api/chat/createChatRequest?requested_id=$userId",
                            emptyMap(),
                            token,
                        )
                    onMessage(if (success) "Chat request sent" else "Failed to send request")
                }
            }) {
                Text("Send Chat Request")
            }
        }
    }
}
