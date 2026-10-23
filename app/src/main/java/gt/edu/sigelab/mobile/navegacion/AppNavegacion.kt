package gt.edu.sigelab.mobile.navegacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gt.edu.sigelab.mobile.UserSession
import gt.edu.sigelab.mobile.ui.menu.MainMenuScreen

@Composable
fun AppNavegacion(
    session: UserSession,
    onLogout: () -> Unit,
    dashboardContent: @Composable () -> Unit
) {
    var pantallaActual by remember(session.token) {
        mutableStateOf("menu")
    }

    when (pantallaActual) {

        "menu" -> {
            MainMenuScreen(
                nombreUsuario = session.name,
                rolUsuario = session.role,
                onNavigate = { ruta ->
                    pantallaActual = ruta
                },
                onLogout = onLogout
            )
        }

        "dashboard" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pantallaActual = "menu"
                    }
                ) {
                    Text("Volver al menú")
                }

                dashboardContent()
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SIGELAB",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Módulo: ${pantallaActual.replaceFirstChar {
                        it.uppercase()
                    }}",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Este módulo se encuentra en desarrollo."
                )

                Button(
                    onClick = {
                        pantallaActual = "menu"
                    }
                ) {
                    Text("Volver al menú")
                }
            }
        }
    }
}
