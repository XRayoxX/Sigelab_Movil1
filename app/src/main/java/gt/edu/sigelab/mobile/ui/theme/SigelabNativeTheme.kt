package gt.edu.sigelab.mobile.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import gt.edu.sigelab.mobile.R

/** La captura de CameraX utiliza vistas Android y comparte el tema elegido en Compose. */
object SigelabNativeTheme {
    private fun isDark(context: Context): Boolean {
        val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return context.getSharedPreferences("sigelab_preferences", Context.MODE_PRIVATE).getBoolean("dark_mode", systemDark)
    }

    @JvmStatic fun applyFacial(activity: Activity) {
        val dark = isDark(activity)
        activity.findViewById<View>(R.id.facialRoot).setBackgroundColor(
            (if (dark) SigelabColors.Navy else Color(0xFFF5FAF5)).toArgb())
        activity.findViewById<TextView>(R.id.textoInstruccion).setTextColor(
            (if (dark) Color(0xFFE5F2FE) else Color(0xFF173753)).toArgb())
        activity.findViewById<TextView>(R.id.textoEstado).setTextColor(
            (if (dark) Color(0xFF94AFC8) else Color(0xFF58718B)).toArgb())
        activity.findViewById<Button>(R.id.botonCerrar).apply {
            backgroundTintList = ColorStateList.valueOf(SigelabColors.Green.toArgb())
            setTextColor(Color.White.toArgb())
            isAllCaps = false
        }
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    @JvmStatic fun resultColor(context: Context, success: Boolean): Int = when {
        success && isDark(context) -> Color(0xFF14B8A6)
        success -> SigelabColors.Green
        isDark(context) -> Color(0xFFFFB4AB)
        else -> Color(0xFFB3261E)
    }.toArgb()
}
