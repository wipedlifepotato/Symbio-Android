package world.wipedlifepotato.symbioandroidapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.networkRequest

@Composable
fun ProfileScreen(navController: NavHostController, token: String) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<JsonObject?>(null) }
    var username by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var avatarBase64 by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        val (success, data) = networkRequest("/profile", emptyMap(), token, "GET")
        if (success && data != null && data is kotlinx.serialization.json.JsonObject) {
            username = data["username"]?.jsonPrimitive?.content ?: ""
            profile = data["profile"]?.jsonObject
            profile?.let {
                fullName = it["full_name"]?.jsonPrimitive?.content ?: ""
                bio = it["bio"]?.jsonPrimitive?.content ?: ""
                skills = (it["skills"]?.jsonArray?.map { s -> s.jsonPrimitive.content } ?: emptyList()).joinToString(", ")
                avatarBase64 = it["avatar"]?.jsonPrimitive?.content ?: ""
            }
        } else {
            errorMessage = "Failed to load profile"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = skills,
                onValueChange = { skills = it },
                label = { Text("Skills (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                coroutineScope.launch {
                    val skillsArray = skills.split(",").map { it.trim() }
                    val updateData = mapOf(
                        "full_name" to fullName,
                        "bio" to bio,
                        "skills" to skillsArray,
                        "avatar" to avatarBase64
                    )
                    val (success, _) = networkRequest("/profile", updateData, token)
                    if (success) {
                        errorMessage = "Profile updated successfully"
                    } else {
                        errorMessage = "Failed to update profile"
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Update Profile")
            }
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = if (errorMessage.contains("successfully")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("dashboard") }) {
            Text("Back to Dashboard")
        }
    }
}