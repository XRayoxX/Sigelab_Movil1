package SIGELAB.util;

/**
 * Se lanza cuando una operación viola una regla de negocio
 * (por ejemplo: "Ya existe un laboratorio con el código LAB-01").
 * El mensaje siempre está pensado para mostrarse directamente al usuario.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String mensaje) {
        super(mensaje);
    }
}
