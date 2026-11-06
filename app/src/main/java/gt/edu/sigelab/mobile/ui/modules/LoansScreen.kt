package gt.edu.sigelab.mobile.ui.modules

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import gt.edu.sigelab.mobile.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LoansScreen(session: UserSession, returns: Boolean) {
    var search by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("TODOS") }
    var version by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    val path = (if (returns) "devoluciones" else "prestamos") + "?" + Api.query("q" to query, "estado" to state)
    val remote = rememberRemote(session, path, version)
    if (creating) {
        BackHandler { creating = false }
        LoanEditor(session, { creating = false }, { creating = false; version++ })
        return
    }
    selected?.let { loan ->
        BackHandler { selected = null }
        LoanDetail(session, loan, { selected = null; version++ })
        return
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (returns) "Devoluciones" else "Préstamos", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(search, { search = it }, label = { Text("Número, carné o solicitante") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (!returns) ChoiceField("Estado", state, listOf("TODOS", "ACTIVO", "FINALIZADO", "VENCIDO").map { it to it }) { state = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { query = search.trim(); version++ }, enabled = !remote.busy) { Text("Buscar") }
            if (!returns) OutlinedButton(onClick = { creating = true }) { Text("Nuevo préstamo") }
        }
        RemoteStatus(remote)
        if (!remote.busy && remote.error.isBlank() && remote.data.rows().isEmpty()) Text("No hay préstamos para esta búsqueda.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(remote.data.rows(), key = { it.optInt("idPrestamo") }) { loan ->
                ElevatedCard(onClick = { selected = loan }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Préstamo #${loan.optInt("idPrestamo")}", style = MaterialTheme.typography.titleMedium)
                        Text(loan.optJSONObject("solicitante")?.text("nombreCompleto").orEmpty().ifBlank { loan.optJSONObject("solicitante")?.text("nombre").orEmpty() })
                        Text("Devolver: ${loan.text("fechaEsperadaDevolucion").replace("T", " ")}")
                        Text("${loan.text("estado")} · ${loan.optInt("equiposPendientes")} pendiente(s)")
                        if (loan.optBoolean("vencido")) Text("VENCIDO", color = MaterialTheme.colorScheme.error)
                        Text("Ver equipos y registrar devolución")
                    }
                }
            }
        }
    }
}
@Composable
private fun LoanEditor(session: UserSession, onClose: () -> Unit, onSaved: () -> Unit) {
    val applicants = rememberRemote(session, "solicitantes?activos=true")
    val available = rememberRemote(session, "equipos?estado=DISPONIBLE")
    var applicant by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<JSONObject>() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    fun addCode(value: String) {
        if (busy) return
        busy = true; error = ""
        Api.call("equipos/codigo?" + Api.query("codigo" to value.trim()), session.token) { r ->
            busy = false
            r.onSuccess { equipment ->
                when {
                    equipment.text("estado") != "DISPONIBLE" -> error = "El equipo está ${equipment.text("estado")}."
                    cart.any { it.optInt("idEquipo") == equipment.optInt("idEquipo") } -> error = "El equipo ya está en el préstamo."
                    else -> { cart.add(equipment); code = "" }
                }
            }.onFailure { error = it.message.orEmpty() }
        }
    }
    val scan = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let { addCode(it) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nuevo préstamo", style = MaterialTheme.typography.headlineSmall)
        RemoteStatus(applicants); RemoteStatus(available)
        ChoiceField("Solicitante", applicant, applicants.data.rows().map { it.text("idSolicitante") to "${it.text("carne")} · ${it.text("nombreCompleto")}" }, !busy) { applicant = it }
        DateField("Devolución esperada", date, withTime = true) { date = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        Text("Equipos disponibles", style = MaterialTheme.typography.titleMedium)
        ChoiceField("Agregar de inventario", "", available.data.rows().filter { e -> cart.none { it.optInt("idEquipo") == e.optInt("idEquipo") } }
            .map { it.text("codigoInventario") to "${it.text("codigoInventario")} · ${it.text("nombre")}" }, !busy) { addCode(it) }
        OutlinedTextField(code, { code = it }, label = { Text("Código de inventario") }, modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { addCode(code) }, enabled = !busy && code.isNotBlank()) { Text("Agregar") }
            OutlinedButton(onClick = { scan.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Agregar equipo por QR").setBeepEnabled(false)) }, enabled = !busy) { Text("Leer QR") }
        }
        Text("${cart.size} equipo(s) seleccionados")
        cart.toList().forEach { equipment ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${equipment.text("codigoInventario")} · ${equipment.text("nombre")}")
                    TextButton(onClick = { cart.remove(equipment) }, enabled = !busy) { Text("Quitar") }
                }
            }
        }
        ErrorText(error)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            val expected = runCatching { LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")) }.getOrNull()
            if (expected == null || !expected.isAfter(LocalDateTime.now(ZoneId.of("America/Guatemala")))) { error = "Seleccione una fecha y hora futura de devolución."; return@Button }
            if (applicant.isBlank() || cart.isEmpty()) { error = "Seleccione solicitante y al menos un equipo."; return@Button }
            busy = true; error = ""
            val body = JSONObject().put("solicitanteId", applicant.toInt()).put("fechaEsperadaDevolucion", expected.toString())
                .put("observaciones", notes).put("equipos", JSONArray(cart.map { it.optInt("idEquipo") }))
            Api.call("prestamos", session.token, "POST", body) { r ->
                busy = false
                r.onSuccess { onSaved() }.onFailure { error = it.message.orEmpty() }
            }
        }, enabled = !busy && !applicants.busy && applicants.error.isBlank(), modifier = Modifier.fillMaxWidth()) { Text("Confirmar préstamo") }
        TextButton(onClick = onClose, enabled = !busy) { Text("Cancelar") }
    }
}
@Composable
private fun LoanDetail(session: UserSession, loan: JSONObject, onClose: () -> Unit) {
    var version by remember { mutableIntStateOf(0) }
    val details = rememberRemote(session, "prestamos/${loan.optInt("idPrestamo")}/detalles", version)
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var condition by remember { mutableStateOf("BUENO") }
    var notes by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Préstamo #${loan.optInt("idPrestamo")}", style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = onClose, enabled = !busy) { Text("‹ Volver a préstamos") }
        RemoteStatus(details)
        Text("Pendientes: ${details.data.rows().count { it.text("estado") == "PENDIENTE" }}")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(details.data.rows(), key = { it.optInt("idDetallePrestamo") }) { detail ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        val e = detail.optJSONObject("equipo")
                        Text("${e?.text("codigoInventario")} · ${e?.text("nombre")}", style = MaterialTheme.typography.titleMedium)
                        Text(detail.text("estado"))
                        if (detail.text("estado") == "DEVUELTO") {
                            Text("Condición: ${detail.text("condicion")}")
                            Text(detail.text("fechaDevolucion").replace("T", " "))
                            Text(detail.text("observacionDevolucion"))
                        } else if (detail.text("estadoPrestamo") == "ACTIVO") Button(onClick = {
                            selected = detail; notes = ""; condition = "BUENO"; error = ""
                        }, enabled = !busy) { Text("Registrar devolución") }
                    }
                }
            }
        }
    }
    selected?.let { detail ->
        AlertDialog(onDismissRequest = { if (!busy) selected = null }, title = { Text("Devolver equipo") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(detail.optJSONObject("equipo")?.text("codigoInventario").orEmpty())
                ChoiceField("Condición", condition, listOf("BUENO" to "Bueno", "DANADO" to "Dañado"), !busy) { condition = it }
                OutlinedTextField(notes, { notes = it }, label = { Text("Observaciones del equipo") }, enabled = !busy)
                ErrorText(error)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            } },
            confirmButton = { TextButton(onClick = {
                if (condition == "DANADO" && notes.isBlank()) { error = "Describa el daño del equipo."; return@TextButton }
                busy = true; error = ""
                Api.call("devoluciones", session.token, "POST", JSONObject().put("detalleId", detail.optInt("idDetallePrestamo"))
                    .put("condicion", condition).put("observaciones", notes)) { r ->
                    busy = false
                    r.onSuccess { selected = null; version++ }.onFailure { error = it.message.orEmpty() }
                }
            }, enabled = !busy) { Text("Confirmar devolución") } },
            dismissButton = { TextButton(onClick = { selected = null }, enabled = !busy) { Text("Cancelar") } })
    }
}
