package SIGELAB.util;

/**
 * Error técnico al hablar con la base de datos (conexión caída, SQL inválido, etc.).
 * Envuelve la SQLException original para que la interfaz muestre un mensaje entendible
 * sin perder el detalle técnico (queda en getCause()).
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensaje) {
        super(mensaje);
    }

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
