package SIGELAB.util;

import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;

/**
 * Guarda el usuario que inició sesión para que cualquier capa sepa quién está operando
 * (por ejemplo, para registrar la auditoría o el "usuario que registra" un préstamo).
 */
public final class Sesion {

    private static final ThreadLocal<Usuario> actual = new ThreadLocal<>();

    private Sesion() {
    }

    public static void iniciar(Usuario usuario) {
        actual.set(usuario);
    }

    public static void cerrar() {
        actual.remove();
    }

    public static Usuario getUsuarioActual() {
        return actual.get();
    }

    /**
     * Validación de permisos en la lógica (no solo ocultando botones).
     * Los Service la llaman antes de operar.
     */
    public static void exigirRol(RolUsuario... roles) {
        if (actual.get() == null) {
            throw new BusinessException("Debe iniciar sesión para realizar esta operación.");
        }
        if (!tieneRol(roles)) {
            throw new BusinessException("Su rol (" + actual.get().getRol() + ") no tiene permiso para realizar esta operación.");
        }
    }

    public static int getIdUsuario() {
        if (actual.get() == null) {
            throw new BusinessException("Debe iniciar sesión para realizar esta operación.");
        }
        return actual.get().getIdUsuario();
    }

    public static boolean tieneRol(RolUsuario... roles) {
        if (actual.get() == null) {
            return false;
        }
        for (RolUsuario rol : roles) {
            if (actual.get().getRol() == rol) {
                return true;
            }
        }
        return false;
    }
}

