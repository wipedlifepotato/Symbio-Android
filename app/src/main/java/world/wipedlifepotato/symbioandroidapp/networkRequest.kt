package world.wipedlifepotato.symbioandroidapp

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Proxy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val BASE_URL = BuildConfig.BASE_URL

val client by lazy {
    val proxy = Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress("localhost", 4444))
    OkHttpClient.Builder().proxy(proxy).build()
}

suspend fun fetchCaptcha(): Pair<String, android.graphics.Bitmap?> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("$BASE_URL/captcha")
        .get()
        .build()

    client.newCall(request).execute().use { response ->
        Log.d("FetchCaptchaResponse",response.toString())
        if (!response.isSuccessful) return@withContext "" to null
        val captchaId = response.header("X-Captcha-Id") ?: ""
        val bytes = response.body?.byteStream()?.use { it.readBytes() }
        val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        captchaId to bitmap
    }
}

suspend fun networkRequest(
    path: String,
    fields: Map<String, String>,
    token: String? = null
): Pair<Boolean, JsonObject?> = withContext(Dispatchers.IO) {
    val proxy = Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress("localhost", 4444))
    val client = OkHttpClient.Builder().proxy(proxy).build()

    val json = buildJsonObject { fields.forEach { (k, v) -> put(k, v) } }
    Log.d("RequestJsonBody", json.toString())

    val requestBuilder = Request.Builder()
        .url("$BASE_URL$path")
        .post(RequestBody.create("application/json".toMediaTypeOrNull(), json.toString()))

    // Добавляем заголовок авторизации, если есть токен
    if (!token.isNullOrEmpty()) {
        requestBuilder.addHeader("Authorization", "Bearer $token")
    }

    val request = requestBuilder.build()
    Log.d("RequestBody", request.toString())

    try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            Log.d("RequestBody_", body ?: "----")
            Log.d("RequestBody", response.toString())

            val jsonResponse = if (!body.isNullOrEmpty()) Json.parseToJsonElement(body).jsonObject else null
            val success = response.isSuccessful

            return@withContext success to jsonResponse
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false to null
    }
}

suspend fun sendBitcoin(
    to: String,
    amount: String,
    token: String
): Pair<Boolean, Any?> = withContext(Dispatchers.IO) {
    val proxy = Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress("localhost", 4444))
    val client = OkHttpClient.Builder().proxy(proxy).build()

    val url = "$BASE_URL/api/wallet/bitcoinSend?to=$to&amount=$amount"
    Log.d("SendBitcoinURL", url)

    val request = Request.Builder()
        .url(url)
        .post(RequestBody.create("application/json".toMediaTypeOrNull(), "{}")) // Empty body
        .addHeader("Authorization", "Bearer $token")
        .build()

    try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            Log.d("SendBitcoinResponse", body ?: "----")

            val success = response.isSuccessful
            val responseData: Any? = if (!body.isNullOrEmpty()) {
                try {
                    Json.parseToJsonElement(body).jsonObject
                } catch (e: Exception) {
                    body
                }
            } else null

            return@withContext success to responseData
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false to null
    }
}

suspend fun doLogin(username: String, password: String, captchaId: String, captchaAnswer: String): Pair<Boolean, JsonObject?> =
    networkRequest("/auth", mapOf(
        "username" to username,
        "password" to password,
        "captcha_id" to captchaId,
        "captcha_answer" to captchaAnswer
    ))

suspend fun doRestore(username: String, mnemonic: String, newPassword: String, captchaId: String, captchaAnswer: String): Pair<Boolean, JsonObject?> =
    networkRequest("/restoreuser", mapOf(
        "username" to username,
        "mnemonic" to mnemonic,
        "new_password" to newPassword,
        "captcha_id" to captchaId,
        "captcha_answer" to captchaAnswer
    ))


