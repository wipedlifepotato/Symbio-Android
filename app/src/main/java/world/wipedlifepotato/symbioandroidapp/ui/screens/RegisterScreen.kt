package world.wipedlifepotato.symbioandroidapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import world.wipedlifepotato.symbioandroidapp.fetchCaptcha
import world.wipedlifepotato.symbioandroidapp.networkRequest

suspend fun doRegister(
    username: String,
    password: String,
    captchaId: String,
    captchaAnswer: String,
): Pair<Boolean, JsonElement?> =
    networkRequest(
        "/register",
        mapOf(
            "username" to username,
            "password" to password,
            "captcha_id" to captchaId,
            "captcha_answer" to captchaAnswer,
        ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: (JsonElement) -> Unit,
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

    LaunchedEffect(Unit) {
        val (id, bmp) = fetchCaptcha()
        captchaId = id
        captchaBitmap = bmp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isWrongPassword) {
                        Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repeatPassword,
                        onValueChange = {
                            repeatPassword = it
                            isWrongPassword =
                                repeatPassword != password && repeatPassword.isNotEmpty()
                        },
                        label = { Text("Repeat Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Капча
                    captchaBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Captcha",
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = captchaAnswer,
                        onValueChange = { captchaAnswer = it },
                        label = { Text("Captcha Answer") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!isWrongPassword && password.isNotEmpty()) {
                                coroutineScope.launch {
                                    val (success, data) =
                                        doRegister(
                                            login,
                                            password,
                                            captchaId,
                                            captchaAnswer,
                                        )
                                    if (success && data != null) {
                                        val jsonObj = data as? kotlinx.serialization.json.JsonObject
                                        serverMessage =
                                            jsonObj?.get("message")?.jsonPrimitive?.content
                                        mnemonic = jsonObj?.get("encrypted")?.jsonPrimitive?.content
                                        onRegisterSuccess(data)
                                    } else {
                                        val jsonObj = data as? kotlinx.serialization.json.JsonObject
                                        serverMessage =
                                            jsonObj?.get("message")?.jsonPrimitive?.content
                                                ?: "Unknown error"
                                        val (newId, newBmp) = fetchCaptcha()
                                        captchaId = newId
                                        captchaBitmap = newBmp
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isWrongPassword && password.isNotEmpty() && login.isNotEmpty(),
                    ) {
                        Text("Register")
                    }

                    serverMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            msg,
                            color = if (mnemonic != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }

                    mnemonic?.let { phrase ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Mnemonic Phrase:")
                        SelectionContainer {
                            Text(phrase, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(phrase))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Copy to Clipboard")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val (newId, newBmp) = fetchCaptcha()
                                captchaId = newId
                                captchaBitmap = newBmp
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Refresh Captcha")
                    }
                }
            }
        }
    }
}
