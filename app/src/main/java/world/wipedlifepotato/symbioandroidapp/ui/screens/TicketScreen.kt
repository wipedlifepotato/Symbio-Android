package world.wipedlifepotato.symbioandroidapp.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun TicketScreen(navController: NavHostController) {
    Text("Ticket screen")
    Button(onClick = {navController.navigate("dashboard")}) { }
}