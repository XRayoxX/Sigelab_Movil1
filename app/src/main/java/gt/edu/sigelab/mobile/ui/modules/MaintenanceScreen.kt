package gt.edu.sigelab.mobile.ui.modules

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gt.edu.sigelab.mobile.*
import org.json.JSONObject

@Composable
fun MaintenanceScreen(session: UserSession) {
    var search by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    val path = "mantenimiento?" + Api.query("q" to query) + if (state.isBlank()) "" else "&estado=$state"
    val remote = rememberRemote(session, path, version)
    if (creating) {
        BackHandler { creating = false }
        MaintenanceEditor(session, { creating = false }, { creating = false; version++ })
        return
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mantenimiento", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(search, { search = it }, label = { Text("Código, nombre o responsable") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        ChoiceField("Estado", state, listOf("" to "Todos", "EN_PROCESO" to "En proceso", "FINALIZADO" to "Finalizado")) { state = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { query = search.trim(); version++ }, enabled = !remote.busy) { Text("Buscar") }
            OutlinedButton(onClick = { creating = true }) { Text("Nuevo ingreso") }
        }
        RemoteStatus(remote)
        if (!remote.busy && remote.error.isBlank() && remote.data.rows().isEmpty()) Text("No hay mantenimientos para esta búsqueda.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(remote.data.rows(), key = { it.optInt("idMantenimiento") }) { record ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("#${record.optInt("idMantenimiento")} · ${record.optJSONObject("equipo")?.text("codigoInventario")}", style = MaterialTheme.typography.titleMedium)
                        Text("${record.text("tipo")} · ${record.text("estado")}")
                        Text("Responsable: ${record.text("responsable")}")
                        Text(record.text("descripcion"))
                        Text("Ingreso: ${record.text("fechaIngreso").replace("T", " ")}")
                        if (record.text("estado") == "EN_PROCESO") Button(onClick = { selected = record }) { Text("Finalizar mantenimiento") }
                        else {
                            Text("Resultado: ${record.text("resultado")}")
                            Text("Salida: ${record.text("fechaSalida").replace("T", " ")}")
                            Text(record.text("observaciones"))
                        }
                    }
                }
            }
        }
    }
    selected?.let { record ->
        CloseMaintenance(session, record, { selected = null }, { selected = null; version++ })
    }
}
@Composable
private fun MaintenanceEditor(session: UserSession, onClose: () -> Unit, onSaved: () -> Unit) {
    val equipment = rememberRemote(session, "mantenimiento/equipos")
    var id by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PREVENTIVO") }
    var technician by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ingreso a mantenimiento", style = MaterialTheme.typography.headlineSmall)
        RemoteStatus(equipment)
        ChoiceField("Equipo", id, equipment.data.rows().map { it.text("idEquipo") to "${it.text("codigoInventario")} · ${it.text("nombre")} (${it.text("estado")})" }, !busy) { id = it }
        ChoiceField("Tipo", type, listOf("PREVENTIVO" to "Preventivo", "CORRECTIVO" to "Correctivo"), !busy) { type = it }
        Text("Un equipo disponible solo admite mantenimiento preventivo.")
        OutlinedTextField(technician, { technician = it }, label = { Text("Responsable / técnico") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(description, { description = it }, label = { Text("Descripción del trabajo o problema") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        ErrorText(error)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            if (id.isBlank() || technician.isBlank() || description.isBlank()) { error = "Complete equipo, responsable y descripción."; return@Button }
            busy = true; error = ""
            Api.call("mantenimiento", session.token, "POST", JSONObject().put("equipo", JSONObject().put("idEquipo", id.toInt()))
                .put("tipo", type).put("responsable", technician.trim()).put("descripcion", description.trim())) { r ->
                busy = false
                r.onSuccess { onSaved() }.onFailure { error = it.message.orEmpty() }
            }
        }, enabled = !busy && !equipment.busy && equipment.error.isBlank()) { Text("Registrar ingreso") }
        TextButton(onClick = onClose, enabled = !busy) { Text("Cancelar") }
    }
}
@Composable
private fun CloseMaintenance(session: UserSession, record: JSONObject, onClose: () -> Unit, onSaved: () -> Unit) {
    var result by remember { mutableStateOf("REPARADO") }
    var notes by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("Finalizar mantenimiento") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(record.optJSONObject("equipo")?.text("codigoInventario").orEmpty())
            ChoiceField("Resultado", result, listOf("REPARADO" to "Reparado → disponible", "NO_REPARABLE" to "No reparable → baja"), !busy) { result = it }
            OutlinedTextField(notes, { notes = it }, label = { Text("Observaciones") }, enabled = !busy)
            ErrorText(error)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        } }, confirmButton = { TextButton(onClick = {
            busy = true
            Api.call("mantenimiento/${record.optInt("idMantenimiento")}/finalizar", session.token, "POST",
                JSONObject().put("resultado", result).put("observaciones", notes)) { r ->
                busy = false; r.onSuccess { onSaved() }.onFailure { error = it.message.orEmpty() }
            }
        }, enabled = !busy) { Text("Confirmar cierre") } }, dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Cancelar") } })
}
