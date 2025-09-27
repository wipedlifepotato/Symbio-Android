package world.wipedlifepotato.symbioandroidapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.ui.screens.ChatScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.DashboardScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.DisputesScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.FreelancerReviewsScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.HomeScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.LoginScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.ProfileScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.ProfilesScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.RegisterScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.RestoreScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.TaskDetailsScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.TaskScreen
import world.wipedlifepotato.symbioandroidapp.ui.screens.TicketScreen
import world.wipedlifepotato.symbioandroidapp.ui.theme.SymbioAndroidAppTheme

// DataStore
val ComponentActivity.dataStore by preferencesDataStore("user_prefs")
val TOKEN_KEY = stringPreferencesKey("token")

suspend fun ComponentActivity.saveToken(token: String) {
    dataStore.edit { prefs -> prefs[TOKEN_KEY] = token }
}

suspend fun ComponentActivity.loadToken(): String {
    val prefs = dataStore.data.first()
    return prefs[TOKEN_KEY] ?: ""
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SymbioAndroidAppTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()

                var token by remember { mutableStateOf("") }
                var userProfile by remember { mutableStateOf<JsonObject?>(null) }
                var wallet by remember { mutableStateOf<JsonObject?>(null) }
                var loading by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }
                var initializing by remember { mutableStateOf(true) } // флаг инициализации

                LaunchedEffect(Unit) {
                    val loadedToken = loadToken()
                    token = loadedToken
                }

                LaunchedEffect(token) {
                    if (token.isNotEmpty()) {
                        loading = true
                        try {
                            // 1. /ownID
                            val (ownIdSuccess, ownIdData) =
                                networkRequest(
                                    "/api/ownID",
                                    emptyMap(),
                                    token,
                                    "GET",
                                )
                            Log.d("GetOwnID", ownIdData.toString())
                            if (ownIdSuccess && ownIdData != null && ownIdData is JsonObject &&
                                ownIdData.containsKey(
                                    "user_id",
                                )
                            ) {
                                val userId =
                                    ownIdData["user_id"]?.jsonPrimitive?.int
                                        ?: throw Exception("No user_id")

                                // 2. /profile/by_id
                                val (profileSuccess, profileData) =
                                    networkRequest(
                                        "/profile/by_id?user_id=$userId",
                                        emptyMap(),
                                        token,
                                        "GET",
                                    )
                                Log.d("ProfileData", profileData.toString())
                                if (profileSuccess && profileData != null && profileData is JsonObject) {
                                    userProfile = profileData

                                    // 3. /api/wallet?currency=BTC
                                    val (walletSuccess, walletData) =
                                        networkRequest(
                                            "/api/wallet?currency=BTC",
                                            emptyMap(),
                                            token,
                                            "GET",
                                        )
                                    Log.d("WalletData", walletData.toString())
                                    if (walletSuccess && walletData != null && walletData is JsonObject) {
                                        wallet = walletData
                                    } else {
                                        errorMessage = "Failed to fetch wallet"
                                    }
                                } else {
                                    errorMessage = "Failed to fetch profile"
                                }
                            } else {
                                errorMessage = "Failed to fetch ownID"
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorMessage = e.message ?: "Unknown error"
                        } finally {
                            loading = false
                        }
                    }
                    initializing = false
                }

                // UI
                if (initializing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (token.isNotEmpty()) "dashboard" else "home",
                            modifier = Modifier.padding(innerPadding),
                        ) {
                            composable("home") { HomeScreen(navController) }
                            composable("login") {
                                LoginScreen(navController) { data ->
                                    val jsonObj = data as? JsonObject
                                    val newToken =
                                        jsonObj?.get("token")?.jsonPrimitive?.content ?: ""
                                    if (newToken.isNotEmpty()) {
                                        token = newToken
                                        coroutineScope.launch { saveToken(token) }
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                }
                            }
                            composable("register") {
                                RegisterScreen(navController) { data ->
                                    val jsonObj = data as? JsonObject
                                    val newToken =
                                        jsonObj?.get("token")?.jsonPrimitive?.content ?: ""
                                    if (newToken.isNotEmpty()) {
                                        token = newToken
                                        coroutineScope.launch { saveToken(token) }
                                        navController.navigate("dashboard") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    }
                                }
                            }
                            composable("restore") {
                                RestoreScreen(navController) { data ->
                                    val jsonObj = data as? JsonObject
                                    val newToken =
                                        jsonObj?.get("encrypted")?.jsonPrimitive?.content ?: ""
                                    if (newToken.isNotEmpty()) {
                                        token = newToken
                                        coroutineScope.launch { saveToken(token) }
                                        navController.navigate("dashboard") {
                                            popUpTo("restore") { inclusive = true }
                                        }
                                    }
                                }
                            }
                            composable("Tasks") {
                                TaskScreen(navController = navController, token = token)
                            }
                            composable("task_details/{taskId}") { backStackEntry ->
                                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                                TaskDetailsScreen(
                                    navController = navController,
                                    taskId = taskId,
                                    token = token,
                                )
                            }
                            composable("freelancer_reviews/{userId}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                FreelancerReviewsScreen(
                                    navController = navController,
                                    userId = userId,
                                    token = token,
                                )
                            }
                            composable("Profile") {
                                ProfileScreen(navController = navController, token = token)
                            }
                            composable("Profiles") {
                                ProfilesScreen(navController = navController, token = token)
                            }
                            composable("Disputes") {
                                DisputesScreen(navController = navController, token = token)
                            }
                            composable("Tickets") {
                                TicketScreen(navController = navController, token = token)
                            } // done
                            composable("Chats") {
                                ChatScreen(navController = navController, token = token)
                            }
                            composable("dashboard") {
                                DashboardScreen(
                                    navController = navController,
                                    profile = userProfile,
                                    wallet = wallet,
                                    loading = loading,
                                    errorMessage = errorMessage,
                                    token = token,
                                    onLogout = {
                                        token = ""
                                        coroutineScope.launch { saveToken("") }
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
