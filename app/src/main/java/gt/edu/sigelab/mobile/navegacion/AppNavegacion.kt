package gt.edu.sigelab.mobile.navegacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gt.edu.sigelab.mobile.UserSession
import gt.edu.sigelab.mobile.ui.modules.*
import gt.edu.sigelab.mobile.ui.menu.MainMenuScreen

@Composable
fun AppNavegacion(session: UserSession, onLogout: () -> Unit) {
    var route by remember(session.token) { mutableStateOf("menu") }
    BackHandler(route != "menu") { route = "menu" }
    if (route == "menu") {
        MainMenuScreen(session.name, session.role, { route = it }, onLogout)
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            TextButton(onClick = { route = "menu" }) { Text("‹ Menú principal") }
            Box(Modifier.weight(1f)) {
                key(route, session.token) {
                    when (route) {
                        "dashboard" -> DashboardScreen(session)
                        "prestamos", "devoluciones" -> LoansScreen(session, route == "devoluciones")
                        "mantenimiento" -> MaintenanceScreen(session)
                        "reportes" -> ReportsScreen(session)
                        "auditoria" -> AuditScreen(session)
                        else -> CatalogScreen(session, route)
                    }
                }
            }
        }
    }
}
