package SIGELAB.enums;

// Ciclo de vida del equipo (sección 6.5). Sin tildes ni Ñ para coincidir con el CHECK de la base.
public enum EstadoEquipo {
    DISPONIBLE,     // puede agregarse a un préstamo
    PRESTADO,       // está en un préstamo activo
    DANADO,         // regresó con problema; no puede prestarse
    MANTENIMIENTO,  // en reparación o revisión; no puede prestarse
    BAJA            // ya no se utiliza
}
