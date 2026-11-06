package gt.edu.sigelab.mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import gt.edu.sigelab.mobile.navegacion.AppNavegacion
import gt.edu.sigelab.mobile.ui.login.LoginScreen
import gt.edu.sigelab.mobile.ui.login.ThemeToggle
import gt.edu.sigelab.mobile.ui.theme.SigelabColors
import gt.edu.sigelab.mobile.ui.theme.SigelabTheme
import org.json.JSONObject
import sigelab.facial.CapturaFacialActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SigelabApp() }
    }
}

@Composable
fun SigelabApp() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("sigelab_preferences", Context.MODE_PRIVATE) }
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(preferences.getBoolean("dark_mode", systemDark)) }
    var session by remember { mutableStateOf<UserSession?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var pendingToken by remember { mutableStateOf<String?>(null) }
    val toggleTheme = {
        dark = !dark
        preferences.edit().putBoolean("dark_mode", dark).apply()
    }
    SideEffect {
        val activity = context as? Activity
        activity?.let {
            val bars = WindowCompat.getInsetsController(it.window, it.window.decorView)
            bars.isAppearanceLightStatusBars = !dark && session != null
            bars.isAppearanceLightNavigationBars = !dark
        }
    }
    val facial = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val token = pendingToken
        pendingToken = null
        busy = false
        val json = runCatching { JSONObject(result.data?.getStringExtra("json").orEmpty()) }.getOrNull()
        if (result.resultCode == Activity.RESULT_OK && json != null && json.has("token")) {
            session = UserSession(json.getString("token"), json.getString("name"), json.getString("role"))
            message = ""
        } else {
            token?.let { Api.call("auth/logout", it, "POST") {} }
            message = json?.optString("error")?.ifBlank { null } ?: "No se completó la verificación facial. Inicie sesión nuevamente."
        }
    }
    DisposableEffect(Unit) {
        Api.onUnauthorized = { token ->
            if (session?.token == token) {
                session = null
                message = "Su sesión expiró o su cuenta cambió. Inicie sesión nuevamente."
            }
        }
        onDispose { Api.onUnauthorized = null }
    }
    SigelabTheme(dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box {
                if (session == null) {
                    Box(Modifier.fillMaxWidth().height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        .background(if (dark) SigelabColors.Blue else SigelabColors.Forest))
                }
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    val current = session
                    if (current != null) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                            ThemeToggle(dark, toggleTheme)
                        }
                        AppNavegacion(current, onLogout = {
                            Api.call("auth/logout", current.token, "POST") {}
                            session = null; password = ""; message = ""
                        })
                    } else {
                        LoginScreen(
                            username = username,
                            password = password,
                            busy = busy,
                            message = message,
                            dark = dark,
                            onUsernameChange = { username = it },
                            onPasswordChange = { password = it },
                            onToggleTheme = toggleTheme,
                            onLogin = {
                                busy = true; message = ""
                                Api.call("auth/login", method = "POST", body = JSONObject().put("username", username.trim()).put("password", password)) { response ->
                                    password = ""
                                    response.onSuccess { json ->
                                        if (!json.has("pendingToken")) {
                                            busy = false; message = "Actualice la API del servidor para habilitar la verificación facial."
                                        } else {
                                            pendingToken = json.getString("pendingToken")
                                            facial.launch(Intent(context, CapturaFacialActivity::class.java)
                                                .putExtra("modo", "VERIFICAR").putExtra("idUsuario", json.getInt("userId"))
                                                .putExtra("token", pendingToken))
                                        }
                                    }.onFailure { busy = false; message = it.message.orEmpty() }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
