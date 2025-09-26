package world.wipedlifepotato.symbioandroidapp.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun ChatScreen(navController: NavHostController) {
    Text("Chat screen")
    Button(onClick = {navController.navigate("dashboard")}) { }
}