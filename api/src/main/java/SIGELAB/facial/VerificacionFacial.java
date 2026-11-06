package SIGELAB.facial;

/** Resultado que únicamente establece el servidor tras consultar el servicio facial. */
public final class VerificacionFacial {
    private VerificacionFacial() {}
    public enum Resultado { APROBADO, RECHAZADO, CANCELADO, NO_DISPONIBLE }
}
