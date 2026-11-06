package gt.edu.sigelab.mobile.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gt.edu.sigelab.mobile.R
import gt.edu.sigelab.mobile.ui.theme.SigelabColors

@Composable
fun LoginScreen(
    username: String,
    password: String,
    busy: Boolean,
    message: String,
    dark: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onLogin: () -> Unit
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val canLogin = !busy && username.isNotBlank() && password.isNotBlank()
    val hero = if (dark) SigelabColors.Blue else SigelabColors.Forest
    fun submit() {
        if (canLogin) {
            keyboard?.hide()
            focus.clearFocus()
            onLogin()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val compact = maxHeight < 760.dp
        Box(Modifier.fillMaxWidth().height(if (compact) 270.dp else 315.dp).background(
            Brush.linearGradient(listOf(hero, if (dark) Color(0xFF0E2944) else Color(0xFF0E482A)))
        )) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.055f), size.width * 0.45f, Offset(size.width, size.height * 0.23f))
                drawCircle(Color.White.copy(alpha = 0.09f), size.width * 0.31f, Offset(size.width, size.height * 0.23f), style = Stroke(1.dp.toPx()))
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = 0.13f), shape = RoundedCornerShape(14.dp)) {
                        Icon(painterResource(R.drawable.ic_lab), null, tint = SigelabColors.Yellow,
                            modifier = Modifier.padding(10.dp).size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SIGELAB", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                        Text("Gestión de laboratorios", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    ThemeToggle(dark, onToggleTheme)
                }
                Spacer(Modifier.height(if (compact) 20.dp else 28.dp))
                Text("Tu laboratorio,\nsiempre contigo.", color = Color.White,
                    fontSize = if (compact) 26.sp else 30.sp,
                    lineHeight = if (compact) 32.sp else 36.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
                Text(if (compact) "Tus laboratorios, en un solo lugar." else "Gestiona tus equipos y préstamos\ndesde un solo lugar.", color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(if (compact) 20.dp else 26.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                ) {
                    Column(Modifier.padding(if (compact) 20.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Bienvenido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Inicia sesión con tu cuenta SIGELAB.", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Usuario", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = username,
                                onValueChange = onUsernameChange,
                                placeholder = { Text("Ingresa tu usuario", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_user), null) },
                                enabled = !busy,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Usuario" },
                                shape = RoundedCornerShape(12.dp),
                                colors = loginFieldColors(),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() })
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Contraseña", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = password,
                                onValueChange = onPasswordChange,
                                placeholder = { Text("Ingresa tu contraseña", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_lock), null) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }, enabled = !busy) {
                                        Icon(painterResource(if (showPassword) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                            if (showPassword) "Ocultar contraseña" else "Mostrar contraseña")
                                    }
                                },
                                enabled = !busy,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus).semantics { contentDescription = "Contraseña" },
                                shape = RoundedCornerShape(12.dp),
                                colors = loginFieldColors(),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { submit() })
                            )
                        }
                        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (dark) 0.65f else 0.5f),
                            shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(painterResource(R.drawable.ic_face_scan), null, tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(23.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Verificación facial", style = MaterialTheme.typography.labelLarge)
                                    Text("Después de tu contraseña, verificaremos tu identidad con la cámara.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (message.isNotBlank()) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                                Text(message, color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(12.dp))
                            }
                        }
                        Button(
                            onClick = { submit() },
                            enabled = canLogin,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SigelabColors.Green,
                                contentColor = Color.White,
                                disabledContainerColor = SigelabColors.Green.copy(alpha = if (dark) 0.32f else 0.12f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(if (busy) "Verificando..." else "Iniciar sesión", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (!busy) {
                                Spacer(Modifier.width(10.dp))
                                Icon(painterResource(R.drawable.ic_arrow_right), null, Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_lock), null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("Acceso seguro a tus laboratorios", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun ThemeToggle(dark: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = SigelabColors.Yellow, contentColor = Color(0xFF212529), shape = RoundedCornerShape(14.dp)) {
        IconButton(onClick = onToggle) {
            Icon(painterResource(if (dark) R.drawable.ic_sun else R.drawable.ic_moon),
                if (dark) "Cambiar a tema claro" else "Cambiar a tema oscuro", Modifier.size(22.dp))
        }
    }
}
