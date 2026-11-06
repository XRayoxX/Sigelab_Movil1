package gt.edu.sigelab.mobile.ui.modules

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gt.edu.sigelab.mobile.*
import org.json.JSONObject
import java.io.OutputStream

val reportTypes = listOf("Equipos por estado", "Préstamos activos", "Préstamos finalizados", "Préstamos vencidos",
    "Préstamos entre fechas", "Historial de un equipo", "Historial de mantenimiento", "Devoluciones entre fechas", "Equipos más prestados")
val equipmentStates = listOf("DISPONIBLE", "PRESTADO", "DANADO", "MANTENIMIENTO", "BAJA")

@Composable
fun ReportsScreen(session: UserSession) {
    val labs = rememberRemote(session, "laboratorios?activos=true")
    var type by remember { mutableStateOf(reportTypes.first()) }
    var lab by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var until by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<JSONObject?>(null) }
    var reportTitle by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    if (report != null) {
        androidx.activity.compose.BackHandler { report = null }
        Column {
            TextButton(onClick = { report = null }) { Text("‹ Cambiar filtros") }
            Text(reportTitle, style = MaterialTheme.typography.titleLarge)
            ReportTable(report!!, reportTitle)
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reportes", style = MaterialTheme.typography.headlineMedium)
        ChoiceField("Reporte", type, reportTypes.map { it to it }, !busy) { type = it }
        RemoteStatus(labs)
        ChoiceField("Laboratorio", lab, listOf("" to "Todos") + labs.data.rows().map { it.text("idLaboratorio") to it.text("nombre") }, !busy) { lab = it }
        OutlinedTextField(code, { code = it }, label = { Text("Código de equipo (opcional)") }, modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true)
        ChoiceField("Estado del equipo", state, listOf("" to "Todos") + equipmentStates.map { it to it }, !busy) { state = it }
        DateField("Desde", from) { from = it }
        DateField("Hasta", until) { until = it }
        TextButton(onClick = { from = ""; until = ""; lab = ""; code = ""; state = "" }, enabled = !busy) { Text("Limpiar filtros") }
        ErrorText(error)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            if (from.isNotBlank() && until.isNotBlank() && java.time.LocalDate.parse(from, dateFormat).isAfter(java.time.LocalDate.parse(until, dateFormat))) {
                error = "La fecha inicial debe ser anterior o igual a la final."; return@Button
            }
            busy = true; error = ""
            val requestedType = type
            val path = "reportes?" + Api.query("tipo" to type, "codigo" to code.trim(), "estado" to state, "desde" to from, "hasta" to until) +
                if (lab.isBlank()) "" else "&laboratorio=$lab"
            Api.call(path, session.token) { r ->
                busy = false
                r.onSuccess { reportTitle = requestedType; report = it }.onFailure { error = it.message.orEmpty() }
            }
        }, enabled = !busy) { Text("Generar reporte") }
    }
}

@Composable
fun ReportTable(report: JSONObject, title: String = "Historial de equipo") {
    val context = LocalContext.current
    val columns = report.optJSONArray("columnas")
    val rows = report.optJSONArray("filas")
    val labels = (0 until (columns?.length() ?: 0)).map { columns!!.optString(it) }
    val data = (0 until (rows?.length() ?: 0)).map { index ->
        val row = rows!!.optJSONArray(index)
        labels.indices.map { i -> if (row == null || row.isNull(i)) "" else row.optString(i) }
    }
    var message by remember { mutableStateOf("") }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            message = runCatching {
                context.contentResolver.openOutputStream(it)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write("\uFEFF")
                    (listOf(labels) + data).forEach { row ->
                        writer.write(row.joinToString(",") { value ->
                            val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
                            "\"" + safe.replace("\"", "\"\"") + "\""
                        } + "\r\n")
                    }
                } ?: error("Destino no disponible")
                "CSV guardado."
            }.getOrElse { "No fue posible guardar el archivo." }
        }
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            message = runCatching {
                context.contentResolver.openOutputStream(it)?.use { stream -> exportPdf(stream, title, labels, data) } ?: error("Destino no disponible")
                "PDF guardado."
            }.getOrElse { "No fue posible guardar el archivo." }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${data.size} registro(s)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { csv.launch("SIGELAB-reporte.csv") }, enabled = data.isNotEmpty()) { Text("Guardar CSV") }
            OutlinedButton(onClick = { pdf.launch("SIGELAB-reporte.pdf") }, enabled = data.isNotEmpty()) { Text("Guardar PDF") }
        }
        OutlinedButton(onClick = {
            message = runCatching {
                val manager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                manager.print("SIGELAB - $title", ReportPrintAdapter(title, labels, data), android.print.PrintAttributes.Builder()
                    .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).build())
                "Seleccione una impresora o guarde en PDF desde el diálogo del sistema."
            }.getOrElse { "No fue posible abrir la impresión." }
        }, enabled = data.isNotEmpty()) { Text("Imprimir reporte") }
        if (message.isNotBlank()) Text(message)
        if (data.isEmpty()) Text("No hay resultados con los filtros seleccionados.")
        Box(Modifier.fillMaxWidth().weight(1f, fill = false).horizontalScroll(rememberScrollState())) {
            Column(Modifier.width((180 * labels.size.coerceAtLeast(1)).dp)) {
                Row { labels.forEach { Text(it, Modifier.width(180.dp).padding(8.dp), style = MaterialTheme.typography.titleSmall) } }
                HorizontalDivider()
                LazyColumn {
                    items(data) { row ->
                        Row { row.forEach { Text(it, Modifier.width(180.dp).padding(8.dp), style = MaterialTheme.typography.bodySmall) } }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** Exporta todos los campos y pagina por líneas; no corta columnas anchas del reporte. */
internal fun exportPdf(output: OutputStream, title: String, columns: List<String>, rows: List<List<String>>, ranges: Array<out android.print.PageRange>? = null): Int {
    val document = PdfDocument()
    try {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.BLACK }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let { document.finishPage(it) }
            pageNumber++
            page = if (ranges == null || ranges.any { pageNumber - 1 >= it.start && pageNumber - 1 <= it.end })
                document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()) else null
            y = 40f
            page?.canvas?.drawText("SIGELAB · $title", 32f, y, paint)
            y += 20f
            page?.canvas?.drawText("Página $pageNumber", 32f, y, paint)
            y += 26f
        }
        fun line(text: String) {
            if (pageNumber == 0 || y > 800f) newPage()
            page?.canvas?.drawText(text, 32f, y, paint)
            y += 16f
        }
        newPage()
        rows.forEachIndexed { index, row ->
            line("Registro ${index + 1}")
            columns.forEachIndexed { i, label ->
                val text = "$label: ${row.getOrElse(i) { "" }}"
                text.split('\n').forEach { paragraph ->
                    var remaining = paragraph.replace('\r', ' ')
                    if (remaining.isEmpty()) line("")
                    while (remaining.isNotEmpty()) {
                        val count = paint.breakText(remaining, true, 530f, null).coerceAtLeast(1)
                        line(remaining.take(count)); remaining = remaining.drop(count)
                    }
                }
            }
            line("")
        }
        page?.let { document.finishPage(it) }
        document.writeTo(output)
        return pageNumber
    } finally { document.close() }
}

@Composable
fun AuditScreen(session: UserSession) {
    var user by remember { mutableStateOf("") }
    var entity by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var until by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }
    val remote = rememberRemote(session, "auditoria?$query", version)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Auditoría", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(user, { user = it }, label = { Text("Usuario exacto (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(entity, { entity = it }, label = { Text("Entidad") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(action, { action = it }, label = { Text("Acción") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        DateField("Desde", from) { from = it }
        DateField("Hasta", until) { until = it }
        Button(onClick = {
            query = Api.query("usuario" to user.trim(), "entidad" to entity.trim(), "accion" to action.trim(), "desde" to from, "hasta" to until); version++
        }, enabled = !remote.busy) { Text("Consultar") }
        RemoteStatus(remote)
        if (!remote.busy && remote.error.isBlank() && remote.data.rows().isEmpty()) Text("No hay movimientos con estos filtros.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(remote.data.rows(), key = { it.optLong("idAuditoria") }) { record ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(record.text("accion"), style = MaterialTheme.typography.titleSmall)
                        Text("${record.text("fechaHora").replace("T", " ")} · ${record.text("nombreUsuario")}")
                        Text("${record.text("entidad")} #${record.text("idRegistro")}")
                        Text(record.text("descripcion"))
                    }
                }
            }
        }
    }
}




