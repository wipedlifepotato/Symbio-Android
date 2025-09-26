package world.wipedlifepotato.symbioandroidapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.ui.screens.*
import world.wipedlifepotato.symbioandroidapp.ui.theme.SymbioAndroidAppTheme
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

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
                            val (ownIdSuccess, ownIdData) = networkRequest("/api/ownID", emptyMap(), token)
                            Log.d("GetOwnID", ownIdData.toString())
                            if (ownIdSuccess && ownIdData != null && ownIdData.containsKey("user_id")) {
                                val userId = ownIdData["user_id"]?.jsonPrimitive?.int ?: throw Exception("No user_id")

                                // 2. /profile/by_id
                                val (profileSuccess, profileData) = networkRequest(
                                    "/profile/by_id?user_id=$userId",
                                    emptyMap(),
                                    token
                                )
                                Log.d("ProfileData", profileData.toString())
                                if (profileSuccess && profileData != null) {
                                    userProfile = profileData

                                    // 3. /api/wallet?currency=BTC
                                    val (walletSuccess, walletData) = networkRequest(
                                        "/api/wallet?currency=BTC",
                                        emptyMap(),
                                        token
                                    )
                                    Log.d("WalletData", walletData.toString())
                                    if (walletSuccess && walletData != null) {
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
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (token.isNotEmpty()) "dashboard" else "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") { HomeScreen(navController) }
                            composable("login") { LoginScreen(navController) { data ->
                                val newToken = data["token"]?.jsonPrimitive?.content ?: ""
                                if (newToken.isNotEmpty()) {
                                    token = newToken
                                    coroutineScope.launch { saveToken(token) }
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } }
                            composable("register") { RegisterScreen(navController) { data ->
                                val newToken = data["token"]?.jsonPrimitive?.content ?: ""
                                if (newToken.isNotEmpty()) {
                                    token = newToken
                                    coroutineScope.launch { saveToken(token) }
                                    navController.navigate("dashboard") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                }
                            } }
                            composable("restore") { RestoreScreen(navController) { data ->
                                val newToken = data["encrypted"]?.jsonPrimitive?.content ?: ""
                                if (newToken.isNotEmpty()) {
                                    token = newToken
                                    coroutineScope.launch { saveToken(token) }
                                    navController.navigate("dashboard") {
                                        popUpTo("restore") { inclusive = true }
                                    }
                                }
                            } }
                            composable("Tasks") {
                                TaskScreen(navController=navController)
                            }
                            composable("Profile") {
                                ProfileScreen(navController=navController)
                            }
                            composable("Profiles") {
                                ProfilesScreen(navController=navController)
                            }
                            composable("Disputes") {
                                DisputesScreen(navController=navController)
                            }
                            composable("Tickets") {
                                TicketScreen(navController=navController)
                            } // done
                            composable("Chats") {
                                ChatScreen(navController=navController)
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
