package SIGELAB.repository;

import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SQL de dbo.Devolucion. Siempre se usa dentro de la transacción de la devolución.
 * Devolver NO elimina el préstamo: se agrega este registro y se conserva el historial.
 */
public class DevolucionRepository {

    public void insertar(Connection cn, int idDetallePrestamo, CondicionDevolucion condicion,
                         String observaciones, int idUsuarioRecibe) {
        // FechaDevolucion la pone SQL Server con CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0))
        String sql = "INSERT INTO dbo.Devolucion (IdDetallePrestamo, Condicion, Observaciones, IdUsuarioRecibe) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idDetallePrestamo);
            ps.setString(2, condicion.name());
            ps.setString(3, observaciones);
            ps.setInt(4, idUsuarioRecibe);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar la devolución.", e);
        }
    }
}

