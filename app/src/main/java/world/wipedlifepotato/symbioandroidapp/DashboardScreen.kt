package world.wipedlifepotato.symbioandroidapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun DashboardScreen(profile: JsonObject?, wallet: JsonObject?, loading: Boolean, errorMessage: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (loading) {
            Text("Loading...")
        } else if (errorMessage.isNotEmpty()) {
            Text("Error: $errorMessage", color = Color.Red)
        } else {
            profile?.let {
                Text("Username: ${it["username"]?.jsonPrimitive?.content ?: ""}")
            }
            wallet?.let {
                Text("BTC Address: ${it.get("address")?.jsonPrimitive?.content ?: "no data"}")
                Text("BTC Balance: ${it.get("balance")?.jsonPrimitive?.content ?: "no data"}")
            }
        }
    }
}
