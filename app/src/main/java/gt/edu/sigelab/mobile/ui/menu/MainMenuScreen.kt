package gt.edu.sigelab.mobile.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    nombreUsuario: String,
    rolUsuario: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {

    val modulos = listOf(
        "dashboard" to "Panel principal",
        "equipos" to "Inventario de equipos",
        "laboratorios" to "Laboratorios",
        "prestamos" to "Préstamos",
        "devoluciones" to "Devoluciones",
        "mantenimiento" to "Mantenimiento",
        "usuarios" to "Usuarios",
        "reportes" to "Reportes"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "SIGELAB",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Sistema de Gestión de Laboratorios",
            style = MaterialTheme.typography.bodyMedium
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "Bienvenido, $nombreUsuario",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Rol: $rolUsuario",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "Menú principal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        modulos.forEach { (ruta, titulo) ->

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate(ruta)
                    },
                shape = RoundedCornerShape(14.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "›",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}
