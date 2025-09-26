package world.wipedlifepotato.symbioandroidapp.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import world.wipedlifepotato.symbioandroidapp.fetchCaptcha
import world.wipedlifepotato.symbioandroidapp.networkRequest
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.serialization.json.jsonPrimitive

suspend fun doRegister(
    username: String,
    password: String,
    captchaId: String,
    captchaAnswer: String
): Pair<Boolean, kotlinx.serialization.json.JsonObject?> =
    networkRequest("/register", mapOf(
        "username" to username,
        "password" to password,
        "captcha_id" to captchaId,
        "captcha_answer" to captchaAnswer
    ))

@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: (JsonObject) -> Unit
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var isWrongPassword by remember { mutableStateOf(false) }
    var captchaId by remember { mutableStateOf("") }
    var captchaAnswer by remember { mutableStateOf("") }
    var captchaBitmap: Bitmap? by remember { mutableStateOf<Bitmap?>(null) }

    var serverMessage by remember { mutableStateOf<String?>(null) }
    var mnemonic by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Загрузка капчи при старте
    LaunchedEffect(Unit) {
        val (id, bmp) = fetchCaptcha()
        captchaId = id
        captchaBitmap = bmp
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

        if (isWrongPassword) {
            Text("WRONG REPEAT PASSWORD", color = Color.Red)
        }

        TextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Login") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = repeatPassword,
            onValueChange = {
                repeatPassword = it
                isWrongPassword = repeatPassword != password
            },
            label = { Text("Repeat Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // Капча
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

        TextField(
            value = captchaAnswer,
            onValueChange = { captchaAnswer = it },
            label = { Text("Captcha Answer") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (!isWrongPassword) {
                    coroutineScope.launch {
                        val (success, data) = doRegister(login, password, captchaId, captchaAnswer)
                        if (success && data != null) {
                            serverMessage = data["message"]?.jsonPrimitive?.content
                            mnemonic = data["encrypted"]?.jsonPrimitive?.content
                            onRegisterSuccess(data)
                        } else {
                            serverMessage = data?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
                            val (newId, newBmp) = fetchCaptcha()
                            captchaId = newId
                            captchaBitmap = newBmp
                        }
                    }
                }
            },
            enabled = !isWrongPassword
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(8.dp))

        serverMessage?.let { msg ->
            Text(msg, color = if (mnemonic != null) Color.Green else Color.Red)
        }

        mnemonic?.let { phrase ->
            Spacer(modifier = Modifier.height(4.dp))
            Text("Mnemonic: $phrase")
            Button(onClick = {
                clipboardManager.setText(AnnotatedString(phrase))
            }) {
                Text("Copy to clipboard")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val (newId, newBmp) = fetchCaptcha()
                    captchaId = newId
                    captchaBitmap = newBmp
                }
            }
        ) {
            Text("Refresh Captcha")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}



