package SIGELAB.model;

/**
 * Contadores del formulario principal (sección 6.11), leídos de la vista dbo.vw_Dashboard.
 */
public record ResumenDashboard(int totalEquipos, int disponibles, int prestados, int danados,
                               int enMantenimiento, int deBaja, int prestamosActivos, int prestamosVencidos) {
}
