package world.wipedlifepotato.symbioandroidapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun DisputesScreen(navController: NavHostController) {
    Column {
        Text("Disputes screen")
        Button(onClick = { navController.navigate("dashboard") }) { }
    }
}