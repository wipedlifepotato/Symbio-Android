package world.wipedlifepotato.symbioandroidapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { navController.navigate("login") }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Login") }
        Button(onClick = { navController.navigate("register") }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Register") }
        Button(onClick = { navController.navigate("restore") }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Restore Account") }
    }
}
