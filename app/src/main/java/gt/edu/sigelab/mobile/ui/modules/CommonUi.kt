package gt.edu.sigelab.mobile.ui.modules

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gt.edu.sigelab.mobile.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter

fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key)
fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONObject.rows(): List<JSONObject> = optJSONArray("items")?.objects().orEmpty()

class Remote {
    var data by mutableStateOf(JSONObject())
    var busy by mutableStateOf(true)
    var error by mutableStateOf("")
}
@Composable
fun rememberRemote(session: UserSession, path: String, version: Int = 0): Remote {
    val remote = remember(session.token, path) { Remote() }
    DisposableEffect(session.token, path, version) {
        var active = true
        remote.busy = true; remote.error = ""
        Api.call(path, session.token) { result ->
            if (active) {
                result.onSuccess { remote.data = it }.onFailure { remote.error = it.message.orEmpty() }
                remote.busy = false
            }
        }
        onDispose { active = false }
    }
    return remote
}
@Composable
fun RemoteStatus(remote: Remote) {
    if (remote.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (remote.error.isNotBlank()) Text(remote.error, color = MaterialTheme.colorScheme.error)
}
@Composable
fun ErrorText(message: String) { if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error) }

@Composable
fun ChoiceField(label: String, value: String, options: List<Pair<String, String>>, enabled: Boolean = true, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text("$label: " + (options.firstOrNull { it.first == value }?.second ?: "Seleccionar"))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) },
        text = {
            LazyColumn { items(options) { option ->
                TextButton(onClick = { onChange(option.first); open = false }, modifier = Modifier.fillMaxWidth()) { Text(option.second) }
            } }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text("Cerrar") } })
}
val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
@Composable
fun DateField(label: String, value: String, withTime: Boolean = false, onChange: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val initial = runCatching { LocalDate.parse(value.take(10), dateFormat) }.getOrElse { LocalDate.now(ZoneId.of("America/Guatemala")) }
        DatePickerDialog(context, { _, year, month, day ->
            val date = LocalDate.of(year, month + 1, day).format(dateFormat)
            if (withTime) TimePickerDialog(context, { _, hour, minute ->
                onChange(date + " " + "%02d:%02d".format(hour, minute))
            }, 17, 0, true).show() else onChange(date)
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }, modifier = Modifier.fillMaxWidth()) { Text("$label: " + value.ifBlank { "Seleccionar fecha" }) }
}
@Composable
fun DashboardScreen(session: UserSession) {
    var revision by remember { mutableIntStateOf(0) }
    val remote = rememberRemote(session, "dashboard", revision)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Panel principal", style = MaterialTheme.typography.headlineMedium)
        Text("Hola, ${session.name}")
        OutlinedButton(onClick = { revision++ }, enabled = !remote.busy) { Text("Actualizar resumen") }
        RemoteStatus(remote)
        listOf("totalEquipos" to "Total de equipos", "disponibles" to "Disponibles", "prestados" to "Prestados",
            "danados" to "Dañados", "enMantenimiento" to "En mantenimiento", "deBaja" to "De baja",
            "prestamosActivos" to "Préstamos activos", "prestamosVencidos" to "Préstamos vencidos").forEach { (key, label) ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label); Text(remote.data.text(key).ifBlank { "—" }, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
