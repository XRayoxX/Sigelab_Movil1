package gt.edu.sigelab.mobile.ui.modules

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import gt.edu.sigelab.mobile.*
import org.json.JSONObject
import sigelab.facial.CapturaFacialActivity

data class Field(val key: String, val label: String, val required: Boolean = true,
    val choices: List<Pair<String, String>>? = null, val number: Boolean = false, val nestedId: String? = null, val secret: Boolean = false)
val recordStates = listOf("ACTIVO" to "Activo", "INACTIVO" to "Inactivo")
fun moduleTitle(module: String) = mapOf("equipos" to "Inventario de equipos", "laboratorios" to "Laboratorios",
    "tipos" to "Tipos de equipo", "solicitantes" to "Solicitantes", "usuarios" to "Usuarios")[module].orEmpty()
fun idKey(module: String) = mapOf("equipos" to "idEquipo", "laboratorios" to "idLaboratorio",
    "tipos" to "idTipoEquipo", "solicitantes" to "idSolicitante", "usuarios" to "idUsuario")[module].orEmpty()

@Composable
fun CatalogScreen(session: UserSession, module: String) {
    var search by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }
    var stateFilter by remember { mutableStateOf("") }
    var labFilter by remember { mutableStateOf("") }
    val filterLabs = if (module == "equipos") rememberRemote(session, "laboratorios?activos=true") else null
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var editing by remember { mutableStateOf(false) }
    val suffix = (if (stateFilter.isBlank()) "" else "&estado=$stateFilter") + (if (labFilter.isBlank()) "" else "&laboratorio=$labFilter")
    val remote = rememberRemote(session, module + "?" + Api.query("q" to query) + suffix, version)
    val scan = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { stateFilter = ""; labFilter = ""; search = it; query = it }
    }
    if (editing) {
        BackHandler { editing = false }
        CatalogEditor(session, module, selected, onClose = { editing = false }, onSaved = { editing = false; version++ })
        return
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(moduleTitle(module), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(search, { search = it }, label = { Text(if (module == "equipos") "Código, nombre, marca o serie" else "Buscar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { query = search.trim(); version++ }, enabled = !remote.busy) { Text("Buscar") }
            OutlinedButton(onClick = { selected = null; editing = true }) { Text("Nuevo") }
            if (module == "equipos") OutlinedButton(onClick = { scan.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Escanee el código del equipo").setBeepEnabled(false)) }) { Text("Leer QR") }
        }
        if (module == "equipos") {
            ChoiceField("Estado", stateFilter, listOf("" to "Todos") + equipmentStates.map { it to it }) { stateFilter = it }
            filterLabs?.let { RemoteStatus(it) }
            ChoiceField("Laboratorio", labFilter, listOf("" to "Todos") + filterLabs?.data?.rows().orEmpty().map { it.text("idLaboratorio") to it.text("nombre") }) { labFilter = it }
        }
        RemoteStatus(remote)
        if (!remote.busy && remote.error.isBlank() && remote.data.rows().isEmpty()) Text("No hay registros para esta búsqueda.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(remote.data.rows(), key = { it.optInt(idKey(module)) }) { row ->
                ElevatedCard(onClick = { selected = row; editing = true }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(row.text("codigoInventario").ifBlank { row.text("codigo") }.ifBlank { row.text("carne") }.ifBlank { row.text("nombreUsuario") }.ifBlank { row.text("nombre") }, style = MaterialTheme.typography.titleMedium)
                        Text(row.text("nombreCompleto").ifBlank { row.text("nombre") })
                        Text(row.text("estado"), color = MaterialTheme.colorScheme.primary)
                        if (module == "equipos") Text(listOf(row.text("marca"), row.text("modelo"), row.optJSONObject("laboratorio")?.text("nombre").orEmpty()).filter { it.isNotBlank() }.joinToString(" · "))
                        Text("Ver detalles y editar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogEditor(session: UserSession, module: String, original: JSONObject?, onClose: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val labs = if (module == "equipos") rememberRemote(session, "laboratorios?activos=true") else null
    val types = if (module == "equipos") rememberRemote(session, "tipos?activos=true") else null
    fun options(remote: Remote?, key: String, property: String): List<Pair<String, String>> {
        val choices = remote?.data?.rows().orEmpty().map { it.text(key) to it.text("nombre") }.toMutableList()
        val old = original?.optJSONObject(property)
        if (old != null && choices.none { it.first == old.text(key) }) choices.add(old.text(key) to old.text("nombre"))
        return choices
    }
    val fields = when (module) {
        "equipos" -> listOf(Field("codigoInventario", "Código de inventario"), Field("nombre", "Nombre"),
            Field("tipoEquipo", "Tipo de equipo", choices = options(types, "idTipoEquipo", "tipoEquipo"), nestedId = "idTipoEquipo"),
            Field("marca", "Marca"), Field("modelo", "Modelo"), Field("numeroSerie", "Número de serie", false),
            Field("laboratorio", "Laboratorio", choices = options(labs, "idLaboratorio", "laboratorio"), nestedId = "idLaboratorio"),
            Field("observaciones", "Observaciones", false))
        "laboratorios" -> listOf(Field("codigo", "Código"), Field("nombre", "Nombre"), Field("ubicacion", "Ubicación"),
            Field("capacidad", "Capacidad", number = true), Field("estado", "Estado", choices = recordStates))
        "tipos" -> listOf(Field("nombre", "Nombre"), Field("descripcion", "Descripción", false), Field("estado", "Estado", choices = recordStates))
        "solicitantes" -> listOf(Field("carne", "Carné"), Field("nombre", "Nombre"), Field("apellido", "Apellido"),
            Field("tipo", "Tipo", choices = listOf("ESTUDIANTE", "DOCENTE", "ADMINISTRATIVO").map { it to it }),
            Field("correo", "Correo", false), Field("telefono", "Teléfono", false), Field("estado", "Estado", choices = recordStates))
        else -> listOf(Field("nombre", "Primer nombre"), Field("segundoNombre", "Segundo nombre", false),
            Field("apellido", "Primer apellido"), Field("segundoApellido", "Segundo apellido", false), Field("correo", "Correo"),
            Field("rol", "Rol", choices = listOf("ADMIN", "ENCARGADO", "DOCENTE").map { it to it }),
            Field("estado", "Estado", choices = recordStates), Field("password", "Contraseña (vacía para conservar)", original == null, secret = true),
            Field("confirmation", "Confirmar contraseña", original == null, secret = true))
    }
    val values = remember(original, module) {
        mutableStateMapOf<String, String>().apply {
            fields.forEach { field -> put(field.key, if (field.nestedId != null) original?.optJSONObject(field.key)?.text(field.nestedId).orEmpty()
                else original?.text(field.key).orEmpty().ifBlank { if (field.key == "estado") "ACTIVO" else "" }) }
        }
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var confirmDeactivate by remember { mutableStateOf(false) }
    var confirmFace by remember { mutableStateOf(false) }
    var replaceFace by remember { mutableStateOf(false) }
    var deleteFace by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(false) }
    val face = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val json = runCatching { JSONObject(result.data?.getStringExtra("json").orEmpty()) }.getOrNull()
        error = if (result.resultCode == Activity.RESULT_OK && result.data?.getIntExtra("status", 500) in 200..299) "Rostro registrado correctamente."
            else json?.text("error")?.ifBlank { null } ?: "No se completó el registro facial."
    }
    if (history && original != null) {
        BackHandler { history = false }
        val remote = rememberRemote(session, "equipos/historial?" + Api.query("codigo" to original.text("codigoInventario")))
        Column {
            TextButton(onClick = { history = false }) { Text("‹ Volver al equipo") }
            RemoteStatus(remote)
            if (!remote.busy && remote.error.isBlank()) ReportTable(remote.data)
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (original == null) "Nuevo registro" else "Detalles del registro", style = MaterialTheme.typography.headlineSmall)
        if (module == "usuarios") Text("El nombre de usuario se genera automáticamente al guardar." + original?.text("nombreUsuario")?.let { "\nUsuario: $it" }.orEmpty())
        if (module == "equipos" && original != null) {
            Text("Estado: ${original.text("estado")}")
            Text("El estado se actualiza mediante préstamos, devoluciones y mantenimiento.")
            EquipmentQr(original.text("codigoInventario"))
            OutlinedButton(onClick = { history = true }) { Text("Consultar historial") }
        }
        labs?.let { RemoteStatus(it) }; types?.let { RemoteStatus(it) }
        fields.forEach { field ->
            if (field.choices != null) ChoiceField(field.label, values[field.key].orEmpty(), field.choices, !busy) { values[field.key] = it }
            else OutlinedTextField(values[field.key].orEmpty(), { values[field.key] = it }, label = { Text(field.label + if (field.required) " *" else "") },
                modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = field.key != "observaciones" && field.key != "descripcion",
                visualTransformation = if (field.secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
        }
        ErrorText(error)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            val missing = fields.firstOrNull { it.required && values[it.key].isNullOrBlank() }
            if (missing != null) { error = "Complete: ${missing.label}"; return@Button }
            if (fields.any { it.number && (values[it.key]?.toIntOrNull() ?: 0) <= 0 }) { error = "Ingrese una capacidad mayor que cero."; return@Button }
            if (module == "usuarios" && values["password"] != values["confirmation"]) { error = "Las contraseñas no coinciden."; return@Button }
            val body = JSONObject()
            original?.let { body.put(idKey(module), it.optInt(idKey(module))) }
            fields.filter { !it.secret }.forEach { field ->
                val value = values[field.key].orEmpty().trim()
                body.put(field.key, when {
                    field.nestedId != null -> JSONObject().put(field.nestedId, value.toInt())
                    field.number -> value.toInt()
                    value.isBlank() && !field.required -> JSONObject.NULL
                    else -> value
                })
            }
            val payload = if (module == "usuarios") JSONObject().put("usuario", body).put("password", values["password"]).put("confirmation", values["confirmation"]) else body
            busy = true; error = ""
            Api.call(module, session.token, "POST", payload) { r ->
                busy = false
                r.onSuccess { onSaved() }.onFailure { error = it.message.orEmpty() }
            }
        }, enabled = !busy && labs?.busy != true && types?.busy != true && labs?.error.orEmpty().isBlank() && types?.error.orEmpty().isBlank(), modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
        if (original != null && module == "usuarios") {
            OutlinedButton(onClick = {
                busy = true
                Api.call("face/status/${original.optInt("idUsuario")}", session.token) { result ->
                    busy = false
                    result.onSuccess { error = if (it.optBoolean("enrolled")) "Rostro registrado." else "Sin rostro registrado." }
                        .onFailure { error = it.message.orEmpty() }
                }
            }, enabled = !busy) { Text("Consultar registro facial") }
            OutlinedButton(onClick = { replaceFace = false; confirmFace = true }, enabled = !busy) { Text("Registrar rostro") }
            OutlinedButton(onClick = { replaceFace = true; confirmFace = true }, enabled = !busy) { Text("Reemplazar rostro") }
            OutlinedButton(onClick = { deleteFace = true }, enabled = !busy) { Text("Eliminar rostro") }
        }
        if (original != null && module != "equipos") OutlinedButton(onClick = { confirmDeactivate = true }, enabled = !busy) { Text("Desactivar registro") }
        TextButton(onClick = onClose, enabled = !busy) { Text("Cancelar") }
        Spacer(Modifier.height(20.dp))
    }
    if (confirmDeactivate) AlertDialog(onDismissRequest = { confirmDeactivate = false }, title = { Text("Desactivar registro") },
        text = { Text("El registro quedará inactivo y se conservará su historial.") }, confirmButton = {
            TextButton(onClick = {
                confirmDeactivate = false; busy = true
                Api.call("$module/${original!!.optInt(idKey(module))}/desactivar", session.token, "POST") { r ->
                    busy = false; r.onSuccess { onSaved() }.onFailure { error = it.message.orEmpty() }
                }
            }) { Text("Desactivar") }
        }, dismissButton = { TextButton(onClick = { confirmDeactivate = false }) { Text("Cancelar") } })
    if (deleteFace) AlertDialog(onDismissRequest = { deleteFace = false }, title = { Text("Eliminar registro facial") },
        text = { Text("La persona no podrá iniciar sesión hasta que un administrador registre nuevamente su rostro.") },
        confirmButton = { TextButton(onClick = {
            deleteFace = false; busy = true
            Api.call("face/profile/${original!!.optInt("idUsuario")}", session.token, "DELETE") { result ->
                busy = false
                result.onSuccess { error = "Registro facial eliminado." }.onFailure { error = it.message.orEmpty() }
            }
        }) { Text("Eliminar rostro") } }, dismissButton = { TextButton(onClick = { deleteFace = false }) { Text("Cancelar") } })
    if (confirmFace) AlertDialog(onDismissRequest = { confirmFace = false }, title = { Text("Registro facial") },
        text = { Text("Verifique la identidad de la persona seleccionada. Se abrirá la cámara para registrar o reemplazar su rostro.") },
        confirmButton = { TextButton(onClick = {
            confirmFace = false
            face.launch(Intent(context, CapturaFacialActivity::class.java).putExtra("modo", "REGISTRAR")
                .putExtra("idUsuario", original!!.optInt("idUsuario")).putExtra("reemplazar", replaceFace).putExtra("token", session.token))
        }) { Text("Abrir cámara") } }, dismissButton = { TextButton(onClick = { confirmFace = false }) { Text("Cancelar") } })
}

@Composable
fun EquipmentQr(code: String) {
    val context = LocalContext.current
    val bitmap = remember(code) {
        val matrix = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 400, 400)
        Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 400) for (x in 0 until 400) setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    var message by remember { mutableStateOf("") }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        uri?.let {
            message = runCatching {
                context.contentResolver.openOutputStream(it)?.use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
                    ?: error("No fue posible abrir el destino.")
                "QR guardado."
            }.getOrElse { "No fue posible guardar el QR." }
        }
    }
    Image(bitmap.asImageBitmap(), "QR del equipo $code", Modifier.size(180.dp))
    OutlinedButton(onClick = { save.launch("SIGELAB-$code.png") }) { Text("Guardar QR") }
    if (message.isNotBlank()) Text(message)
}




