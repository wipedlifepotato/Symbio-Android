package world.wipedlifepotato.symbioandroidapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.networkRequest

@Composable
fun TaskDetailsScreen(
    navController: NavHostController,
    taskId: String,
    token: String,
) {
    val coroutineScope = rememberCoroutineScope()
    var task by remember { mutableStateOf<JsonObject?>(null) }
    var offers by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var canReview by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var showMakeOfferDialog by remember { mutableStateOf(false) }
    var offerPrice by remember { mutableStateOf("") }
    var offerMessage by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf(0) }
    var hasUserOffer by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        loading = true
        try {
            // Fetch task details
            val (taskSuccess, taskData) =
                networkRequest(
                    "/api/tasks/get?id=$taskId",
                    emptyMap(),
                    token,
                    "GET",
                )
            if (taskSuccess && taskData != null && taskData is JsonObject && taskData.containsKey("task")) {
                val taskObj = taskData["task"]?.jsonObject
                if (taskObj != null) {
                    // Check if own task
                    val (ownIdSuccess, ownIdData) =
                        networkRequest(
                            "/api/ownID",
                            emptyMap(),
                            token,
                            "GET",
                        )
                    if (ownIdSuccess && ownIdData != null && ownIdData is JsonObject &&
                        ownIdData.containsKey(
                            "user_id",
                        )
                    ) {
                        val userId = ownIdData["user_id"]?.jsonPrimitive?.int ?: 0
                        val clientId = taskObj["client_id"]?.jsonPrimitive?.int ?: -1
                        val mutableTask = taskObj.toMutableMap()
                        mutableTask["is_own"] = JsonPrimitive(userId == clientId)
                        task = JsonObject(mutableTask)
                    } else {
                        task = taskObj
                    }
                }
            } else {
                errorMessage = if (taskData is String) taskData else "Failed to load task details"
            }

            // Fetch offers
            val (offersSuccess, offersData) =
                networkRequest(
                    "/api/offers?task_id=$taskId",
                    emptyMap(),
                    token,
                    "GET",
                )
            if (offersSuccess && offersData != null && offersData is JsonObject &&
                offersData.containsKey(
                    "offers",
                ) && offersData["offers"] !is JsonNull
            ) {
                offers = offersData["offers"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
            }

            // Fetch reviews
            val (reviewsSuccess, reviewsData) =
                networkRequest(
                    "/api/reviews/task?task_id=$taskId",
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

            // Get user ID
            val (ownIdSuccess, ownIdData) = networkRequest("/api/ownID", emptyMap(), token, "GET")
            if (ownIdSuccess && ownIdData != null && ownIdData is JsonObject &&
                ownIdData.containsKey(
                    "user_id",
                )
            ) {
                userId = ownIdData["user_id"]?.jsonPrimitive?.int ?: 0
                hasUserOffer = offers.any { it["freelancer_id"]?.jsonPrimitive?.int == userId }

                // Check if can review
                if (task != null && task!!["status"]?.jsonPrimitive?.content == "completed") {
                    val clientId = task!!["client_id"]?.jsonPrimitive?.int ?: -1
                    if (clientId == userId) {
                        canReview = true
                    } else {
                        // Check if user has accepted offer
                        offers.forEach { offer ->
                            if (offer["accepted"]?.jsonPrimitive?.boolean == true &&
                                offer["freelancer_id"]?.jsonPrimitive?.int == userId
                            ) {
                                canReview = true
                            }
                        }
                    }
                }
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
        Text("Task Details", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else if (task != null) {
            LazyColumn {
                item {
                    TaskDetailCard(task!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text("Offers", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(offers) { offer ->
                    OfferCard(
                        offer,
                        token,
                        taskId.toInt(),
                        coroutineScope,
                        navController,
                        task!!["is_own"]?.jsonPrimitive?.boolean == true,
                    ) {
                        // Refresh data after accepting offer
                        coroutineScope.launch {
                            // Re-fetch offers
                            val (offersSuccess, offersData) =
                                networkRequest(
                                    "/api/offers?task_id=$taskId",
                                    emptyMap(),
                                    token,
                                    "GET",
                                )
                            if (offersSuccess && offersData != null && offersData is JsonObject &&
                                offersData.containsKey(
                                    "offers",
                                ) && offersData["offers"] !is JsonNull
                            ) {
                                offers = offersData["offers"]?.jsonArray?.map { it.jsonObject }
                                    ?: emptyList()
                            }
                        }
                    }
                }

                val isOwnTask = task!!["is_own"]?.jsonPrimitive?.boolean == true

                if (!isOwnTask && !hasUserOffer) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showMakeOfferDialog = true }) {
                            Text("Make Offer")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reviews", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(reviews) { review ->
                    ReviewCard(review)
                }

                if (canReview) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showReviewDialog = true }) {
                            Text("Leave Review")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("Tasks") }) {
            Text("Back to Tasks")
        }
    }

    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Leave a Review") },
            text = {
                Column {
                    Text("Rating (1-5):")
                    Slider(
                        value = reviewRating.toFloat(),
                        onValueChange = { reviewRating = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                    )
                    Text("$reviewRating")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Comment") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val (success, _) =
                            networkRequest(
                                "/api/reviews/create",
                                mapOf(
                                    "task_id" to taskId,
                                    "rating" to reviewRating.toString(),
                                    "comment" to reviewComment,
                                ),
                                token,
                                "POST",
                            )
                        if (success) {
                            showReviewDialog = false
                            reviewComment = ""
                            reviewRating = 5
                            // Refresh reviews
                            val (reviewsSuccess, reviewsData) =
                                networkRequest(
                                    "/api/reviews/task?task_id=$taskId",
                                    emptyMap(),
                                    token,
                                    "GET",
                                )
                            if (reviewsSuccess && reviewsData != null && reviewsData is JsonObject &&
                                reviewsData.containsKey(
                                    "reviews",
                                ) && reviewsData["reviews"] !is JsonNull
                            ) {
                                reviews = reviewsData["reviews"]?.jsonArray?.map { it.jsonObject }
                                    ?: emptyList()
                            }
                        }
                    }
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                Button(onClick = { showReviewDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showMakeOfferDialog) {
        AlertDialog(
            onDismissRequest = { showMakeOfferDialog = false },
            title = { Text("Make an Offer") },
            text = {
                Column {
                    OutlinedTextField(
                        value = offerPrice,
                        onValueChange = { offerPrice = it },
                        label = { Text("Price") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = offerMessage,
                        onValueChange = { offerMessage = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val (success, _) =
                            networkRequest(
                                "/api/offers/create",
                                mapOf(
                                    "task_id" to taskId.toInt(),
                                    "price" to offerPrice.toFloat(),
                                    "message" to offerMessage,
                                ),
                                token,
                                "POST",
                            )
                        if (success) {
                            showMakeOfferDialog = false
                            offerPrice = ""
                            offerMessage = ""
                            // Refresh offers
                            val (offersSuccess, offersData) =
                                networkRequest(
                                    "/api/offers?task_id=$taskId",
                                    emptyMap(),
                                    token,
                                    "GET",
                                )
                            if (offersSuccess && offersData != null && offersData is JsonObject &&
                                offersData.containsKey(
                                    "offers",
                                ) && offersData["offers"] !is JsonNull
                            ) {
                                offers = offersData["offers"]?.jsonArray?.map { it.jsonObject }
                                    ?: emptyList()
                            }
                        }
                    }
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                Button(onClick = { showMakeOfferDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun TaskDetailCard(task: JsonObject) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                task["title"]?.jsonPrimitive?.content ?: "No title",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(task["description"]?.jsonPrimitive?.content ?: "No description")
            Text("Status: ${task["status"]?.jsonPrimitive?.content ?: "Unknown"}")
            Text("Budget: ${task["budget"]?.jsonPrimitive?.content ?: "0"} ${task["currency"]?.jsonPrimitive?.content ?: "BTC"}")
            Text("Deadline: ${task["deadline"]?.jsonPrimitive?.content ?: "No deadline"}")
        }
    }
}

@Composable
fun OfferCard(
    offer: JsonObject,
    token: String,
    taskId: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    navController: NavHostController,
    isOwnTask: Boolean,
    onOfferAccepted: () -> Unit,
) {
    var acceptError by remember { mutableStateOf("") }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Freelancer: ${offer["freelancer"]?.jsonPrimitive?.content ?: "Unknown"}")
            Text("Price: ${offer["price"]?.jsonPrimitive?.content ?: "0"} ${offer["currency"]?.jsonPrimitive?.content ?: "BTC"}")
            Text("Message: ${offer["message"]?.jsonPrimitive?.content ?: "No message"}")
            Text("Accepted: ${if (offer["accepted"]?.jsonPrimitive?.boolean == true) "Yes" else "No"}")

            if (acceptError.isNotEmpty()) {
                Text(acceptError, color = MaterialTheme.colorScheme.error)
            }

            Row {
                if (isOwnTask && offer["accepted"]?.jsonPrimitive?.boolean != true) {
                    Button(onClick = {
                        coroutineScope.launch {
                            val offerId = offer["id"]?.jsonPrimitive?.int ?: 0
                            val (success, response) =
                                networkRequest(
                                    "/api/offers/accept",
                                    mapOf("offer_id" to offerId),
                                    token,
                                    "POST",
                                )
                            if (success && response != null && response is JsonObject) {
                                acceptError = ""
                                onOfferAccepted() // Refresh the data
                            } else {
                                acceptError =
                                    if (response is JsonPrimitive) response.content else "Failed to accept offer"
                            }
                        }
                    }) {
                        Text("Accept Offer")
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val freelancerId = offer["freelancer_id"]?.jsonPrimitive?.content ?: ""
                Button(onClick = {
                    navController.navigate("freelancer_reviews/$freelancerId")
                }) {
                    Text("View Reviews")
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: JsonObject) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Reviewer: ${review["reviewer_name"]?.jsonPrimitive?.content ?: "Unknown"}")
            Text("Rating: ${review["rating"]?.jsonPrimitive?.int ?: 0}/5")
            Text("Comment: ${review["comment"]?.jsonPrimitive?.content ?: "No comment"}")
        }
    }
}
