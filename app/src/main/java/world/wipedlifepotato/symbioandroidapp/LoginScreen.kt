package world.wipedlifepotato.symbioandroidapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun LoginScreen(navController: NavHostController, onSuccess: (JsonObject) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captchaBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var captchaId by remember { mutableStateOf("") }
    var captchaAnswer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val (id, bmp) = fetchCaptcha()
        captchaId = id
        captchaBitmap = bmp
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { navController.popBackStack() }) { Text("Back") }
        Spacer(Modifier.height(16.dp))
        TextField(username, { username = it }, label = { Text("Username") })
        TextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        captchaBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Captcha",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )
        }
        TextField(captchaAnswer, { captchaAnswer = it }, label = { Text("Captcha") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            scope.launch {
                val (success, data) = doLogin(username, password, captchaId, captchaAnswer)
                if (success && data != null) onSuccess(data) else error = "Login failed: "+data?.getValue("error")
                val (newId, newBmp) = fetchCaptcha()
                captchaId = newId
                captchaBitmap = newBmp
            }
        }) { Text("Login") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

suspend fun doLogin(username: String, password: String, captchaId: String, captchaAnswer: String): Pair<Boolean, JsonObject?> =
    networkRequest("/auth", mapOf(
        "username" to username,
        "password" to password,
        "captcha_id" to captchaId,
        "captcha_answer" to captchaAnswer
    ))