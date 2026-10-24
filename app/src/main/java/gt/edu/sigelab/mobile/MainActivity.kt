
package gt.edu.sigelab.mobile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import gt.edu.sigelab.mobile.navegacion.AppNavegacion
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.util.concurrent.Executors

data class UserSession(
    val token: String,
    val name: String,
    val role: String
)

data class Dashboard(
    val total: Int,
    val disponibles: Int,
    val prestados: Int,
    val danados: Int,
    val mantenimiento: Int,
    val baja: Int,
    val activos: Int,
    val vencidos: Int
)

object Api {

    private const val TAG = "SIGELAB_API"

    private val executor =
        Executors.newSingleThreadExecutor()

    private val main =
        Handler(Looper.getMainLooper())

    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject {

        val baseUrl = BuildConfig.API_URL.trimEnd('/')
        val fullUrl = "$baseUrl/${path.trimStart('/')}"

        var connection: HttpURLConnection? = null

        try {
            Log.d(TAG, "Solicitud $method a $fullUrl")

            connection =
                URL(fullUrl).openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.useCaches = false

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (!token.isNullOrBlank()) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (body != null) {
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                connection.outputStream.use { output ->
                    output.write(
                        body.toString().toByteArray(Charsets.UTF_8)
                    )
                }
            }

            val code = connection.responseCode

            Log.d(TAG, "Respuesta HTTP $code")

            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                val message = runCatching {
                    val json = JSONObject(responseText)
                    json.optString("error")
                        .ifBlank { json.optString("message") }
                }.getOrNull()
                    ?.ifBlank { "Error HTTP $code" }
                    ?: "Error HTTP $code"

                throw IllegalStateException(
                    "HTTP $code: $message"
                )
            }

            return JSONObject(responseText)

        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Tiempo de espera agotado", e)
            throw IllegalStateException(
                "La API no respondió a tiempo: $fullUrl",
                e
            )

        } catch (e: ConnectException) {
            Log.e(TAG, "Conexión rechazada", e)
            throw IllegalStateException(
                "No se pudo conectar con la API: $fullUrl",
                e
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error en solicitud HTTP", e)
            throw e

        } finally {
            connection?.disconnect()
        }
    }

    fun health(done: (Result<String>) -> Unit) {
        executor.execute {
            val result = runCatching {
                request("api/health", "GET")
                    .optString("status", "SIN ESTADO")
            }

            main.post { done(result) }
        }
    }

    fun login(
        username: String,
        password: String,
        done: (Result<UserSession>) -> Unit
    ) {
        executor.execute {
            val result = runCatching {
                val json = request(
                    "api/auth/login",
                    "POST",
                    JSONObject()
                        .put("username", username)
                        .put("password", password)
                )

                UserSession(
                    token = json.getString("token"),
                    name = json.getString("name"),
                    role = json.getString("role")
                )
            }

            main.post { done(result) }
        }
    }

    fun dashboard(
        token: String,
        done: (Result<Dashboard>) -> Unit
    ) {
        executor.execute {
            val result = runCatching {
                val json = request(
                    "api/dashboard",
                    "GET",
                    token = token
                )

                Dashboard(
                    total = json.getInt("totalEquipos"),
                    disponibles = json.getInt("disponibles"),
                    prestados = json.getInt("prestados"),
                    danados = json.getInt("danados"),
                    mantenimiento = json.getInt("enMantenimiento"),
                    baja = json.getInt("deBaja"),
                    activos = json.getInt("prestamosActivos"),
                    vencidos = json.getInt("prestamosVencidos")
                )
            }

            main.post { done(result) }
        }
    }

    fun logout(token: String) {
        executor.execute {
            runCatching {
                request(
                    "api/auth/logout",
                    "POST",
                    token = token
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                SigelabApp()
            }
        }
    }
}

@Composable
fun SigelabApp() {

    var session by remember {
        mutableStateOf<UserSession?>(null)
    }

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf("")
    }

    var connectionStatus by remember {
        mutableStateOf("")
    }

    var dashboard by remember {
        mutableStateOf<Dashboard?>(null)
    }

    fun reload(currentSession: UserSession) {
        busy = true
        error = ""

        Api.dashboard(currentSession.token) { result ->

            // Ignorar respuestas de una sesión ya cerrada.
            if (session?.token == currentSession.token) {
                result
                    .onSuccess {
                        dashboard = it
                        error = ""
                    }
                    .onFailure {
                        error = it.message
                            ?: "No fue posible cargar el dashboard"
                    }

                busy = false
            }
        }
    }

    val currentSession = session

    if (currentSession == null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                "SIGELAB",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                "Sistema de Gestión de Laboratorios",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Button(
                onClick = {
                    busy = true
                    error = ""

                    Api.login(username.trim(), password) { result ->
                        result
                            .onSuccess {
                                session = it
                                password = ""
                                dashboard = null
                                busy = false

                                // Cargar datos después del login.
                                reload(it)
                            }
                            .onFailure {
                                error = it.message
                                    ?: "No fue posible iniciar sesión"
                                busy = false
                            }
                    }
                },
                enabled = !busy &&
                        username.isNotBlank() &&
                        password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (busy) "Conectando..."
                    else "Iniciar sesión"
                )
            }

            HorizontalDivider()

            Text(
                "Diagnóstico de conexión",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedButton(
                onClick = {
                    busy = true
                    error = ""
                    connectionStatus = "Comprobando API..."

                    Api.health { result ->
                        result
                            .onSuccess {
                                connectionStatus =
                                    "API conectada correctamente: $it"
                            }
                            .onFailure {
                                connectionStatus = "Error de conexión"
                                error = it.message
                                    ?: "No fue posible conectar"
                            }

                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Probar conexión API")
            }

            if (connectionStatus.isNotBlank()) {
                Text(connectionStatus)
            }

            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (error.isNotBlank()) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

    } else {

        AppNavegacion(
            session = currentSession,

            onLogout = {
                Api.logout(currentSession.token)

                session = null
                dashboard = null
                password = ""
                error = ""
                connectionStatus = ""
                busy = false
            },

            dashboardContent = {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        "Panel principal",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text("Usuario: ${currentSession.name}")
                    Text("Rol: ${currentSession.role}")

                    Button(
                        onClick = {
                            reload(currentSession)
                        },
                        enabled = !busy
                    ) {
                        Text("Actualizar resumen")
                    }

                    if (busy) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (error.isNotBlank()) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    dashboard?.let { data ->

                        val indicadores = listOf(
                            "Total de equipos" to data.total,
                            "Disponibles" to data.disponibles,
                            "Prestados" to data.prestados,
                            "Dañados" to data.danados,
                            "En mantenimiento" to data.mantenimiento,
                            "De baja" to data.baja,
                            "Préstamos activos" to data.activos,
                            "Préstamos vencidos" to data.vencidos
                        )

                        indicadores.forEach { (titulo, cantidad) ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween
                                ) {
                                    Text(titulo)
                                    Text(
                                        "$cantidad",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
