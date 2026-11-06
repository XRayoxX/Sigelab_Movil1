package SIGELAB.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Ejecuta varias operaciones SQL como UNA sola unidad (todo o nada).
 * Ejemplo: confirmar un préstamo = encabezado + detalle + cambio de estados + auditoría.
 * Si cualquier paso falla se hace ROLLBACK y la base queda como estaba antes.
 */
public final class Transaccion {

    private Transaccion() {
    }

    public static <T> T ejecutar(Function<Connection, T> operaciones) {
        try (Connection cn = ConexionBD.obtenerConexion()) {
            cn.setAutoCommit(false);           // a partir de aquí nada se guarda hasta el commit
            try {
                T resultado = operaciones.apply(cn);
                cn.commit();                   // todo salió bien: se guarda todo junto
                return resultado;
            } catch (RuntimeException e) {
                cn.rollback();                 // algo falló: se deshace todo
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo completar la operación en la base de datos.", e);
        }
    }
}
