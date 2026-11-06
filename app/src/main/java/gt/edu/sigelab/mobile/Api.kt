package gt.edu.sigelab.mobile

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

data class UserSession(val token: String, val name: String, val role: String)
class ApiException(val status: Int, message: String) : Exception(message)

object Api {
    private val executor = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())
    var onUnauthorized: ((String) -> Unit)? = null

    fun query(vararg values: Pair<String, String>): String =
        values.joinToString("&") { (key, value) -> "$key=" + URLEncoder.encode(value, "UTF-8") }

    fun call(path: String, token: String? = null, method: String = "GET", body: JSONObject? = null,
             done: (Result<JSONObject>) -> Unit) {
        executor.execute {
            val result = runCatching {
                val connection = URL(BuildConfig.API_URL.trimEnd('/') + "/api/" + path).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = method
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.setRequestProperty("Accept", "application/json")
                    token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                    body?.let {
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        connection.outputStream.use { stream -> stream.write(it.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(text.ifBlank { "{}" }) }.getOrElse {
                        throw ApiException(status, "El servidor devolvió una respuesta inválida.")
                    }
                    if (status !in 200..299) throw ApiException(status, json.optString("error").ifBlank { json.optString("message", "Error HTTP $status") })
                    json
                } finally { connection.disconnect() }
            }.recoverCatching {
                if (it is ApiException) throw it
                throw IllegalStateException("No fue posible conectar con el servidor. Revise su conexión e intente nuevamente.", it)
            }
            main.post {
                val failure = result.exceptionOrNull()
                if (failure is ApiException && failure.status == 401 && token != null) onUnauthorized?.invoke(token)
                done(result)
            }
        }
    }
}
