package world.wipedlifepotato.symbioandroidapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
fun FreelancerReviewsScreen(
    navController: NavHostController,
    userId: String,
    token: String,
) {
    rememberCoroutineScope()
    var reviews by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var username by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        loading = true
        try {
            // Fetch user reviews
            val (reviewsSuccess, reviewsData) =
                networkRequest(
                    "/api/reviews/user?user_id=$userId",
                    emptyMap(),
                    token,
                    "GET",
                )
            if (reviewsSuccess && reviewsData != null && reviewsData is JsonObject &&
                reviewsData.containsKey(
                    "reviews",
                ) && reviewsData["reviews"] !is JsonNull
            ) {
                reviews = reviewsData["reviews"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
            }

            // Fetch username
            val (profileSuccess, profileData) =
                networkRequest(
                    "/profile/by_id?user_id=$userId",
                    emptyMap(),
                    token,
                    "GET",
                )
            if (profileSuccess && profileData != null && profileData is JsonObject &&
                profileData.containsKey(
                    "username",
                )
            ) {
                username = profileData["username"]?.jsonPrimitive?.content ?: "User $userId"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text("Reviews for $username", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                items(reviews) { review ->
                    ReviewCard(review)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}
