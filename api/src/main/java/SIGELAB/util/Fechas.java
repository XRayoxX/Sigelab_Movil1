package SIGELAB.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Conversión entre el texto que escribe el usuario (dd/MM/yyyy) y las fechas de Java.
 */
public final class Fechas {

    // "uuuu" + STRICT rechaza fechas imposibles como 31/02/2026
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Fechas() {
    }

    // Convierte el texto a fecha; lanza BusinessException con un mensaje claro si no es válido
    public static LocalDate leer(String texto, String nombreCampo) {
        if (texto == null || texto.isBlank()) {
            throw new BusinessException("Ingrese la " + nombreCampo + " con el formato dd/MM/yyyy.");
        }
        try {
            return LocalDate.parse(texto.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new BusinessException("La " + nombreCampo + " no es válida. Use el formato dd/MM/yyyy, por ejemplo 25/10/2026.");
        }
    }

    // Igual que leer, pero si el campo está vacío devuelve null (filtro opcional)
    public static LocalDate leerOpcional(String texto, String nombreCampo) {
        return texto == null || texto.isBlank() ? null : leer(texto, nombreCampo);
    }

    public static String formatear(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    public static String formatear(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : fechaHora.format(FECHA_HORA);
    }
}
