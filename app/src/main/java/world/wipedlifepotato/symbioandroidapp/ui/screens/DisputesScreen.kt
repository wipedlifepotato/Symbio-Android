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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.networkRequest

@Composable
fun DisputesScreen(
    navController: NavHostController,
    token: String,
) {
    rememberCoroutineScope()
    var disputes by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        val (success, data) = networkRequest("/api/disputes/my", emptyMap(), token, "GET")
        if (success && data != null && data is JsonObject &&
            data.containsKey(
                "disputes",
            ) && data["disputes"] !is JsonNull
        ) {
            disputes = data["disputes"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
        } else {
            errorMessage = "Failed to load disputes"
        }
        loading = false
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text("My Disputes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                items(disputes) { dispute ->
                    DisputeItem(dispute, navController)
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
fun DisputeItem(
    dispute: JsonObject,
    navController: NavHostController,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Dispute ID: ${dispute["id"]?.jsonPrimitive?.content ?: "Unknown"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("Status: ${dispute["status"]?.jsonPrimitive?.content ?: "Unknown"}")
            Text("Opened by: ${dispute["opened_by_username"]?.jsonPrimitive?.content ?: "Unknown"}")
            Button(onClick = { /* Navigate to dispute details */ }) {
                Text("View Details")
            }
        }
    }
}
