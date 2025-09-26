package world.wipedlifepotato.symbioandroidapp.ui.screens

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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.networkRequest

@Composable
fun TaskScreen(navController: NavHostController, token: String) {
    val coroutineScope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("all") }
    var page by remember { mutableStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }

    LaunchedEffect(status, page) {
        loading = true
        val query = mutableMapOf<String, String>()
        if (status != "all") query["status"] = status
        query["limit"] = "20"
        query["offset"] = ((page - 1) * 20).toString()
        val endpoint = "/api/tasks" + if (query.isNotEmpty()) "?" + query.entries.joinToString("&") { "${it.key}=${it.value}" } else ""

        val (success, data) = networkRequest(endpoint, emptyMap(), token, "GET")
        if (success && data != null && data is kotlinx.serialization.json.JsonObject && data.containsKey("tasks") && data["tasks"] !is JsonNull) {
            val tasksArray = data["tasks"]?.jsonArray
            tasks = tasksArray?.map { it.jsonObject } ?: emptyList()
            hasNext = tasks.size >= 20
        } else {
            errorMessage = "Failed to load tasks"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tasks", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Status filter
        Row {
            listOf("all", "open", "in_progress", "completed").forEach { s ->
                FilterChip(
                    selected = status == s,
                    onClick = { status = s; page = 1 },
                    label = { Text(s.replace("_", " ").capitalize()) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                items(tasks) { task ->
                    TaskItem(task, navController)
                }
                item {
                    if (hasNext) {
                        Button(onClick = { page++ }, modifier = Modifier.fillMaxWidth()) {
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
fun TaskItem(task: JsonObject, navController: NavHostController) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task["title"]?.jsonPrimitive?.content ?: "No title", style = MaterialTheme.typography.titleMedium)
            Text(task["description"]?.jsonPrimitive?.content ?: "No description")
            Text("Status: ${task["status"]?.jsonPrimitive?.content ?: "Unknown"}")
            Text("Budget: ${task["budget"]?.jsonPrimitive?.content ?: "0"} ${task["currency"]?.jsonPrimitive?.content ?: "BTC"}")
            Button(onClick = { /* Navigate to task details */ }) {
                Text("View Details")
            }
        }
    }
}
