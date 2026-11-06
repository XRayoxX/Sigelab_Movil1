package SIGELAB.repository;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.model.Auditoria;
import SIGELAB.util.ConexionBD;
import SIGELAB.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL de dbo.Auditoria (RN-18).
 * La FechaHora la pone SQL Server con su valor por defecto CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0)).
 */
public class AuditoriaRepository {

    // Abre su propia conexión (por ejemplo, para los intentos de login)
    public void registrar(Integer idUsuario, String nombreUsuario, AccionAuditoria accion,
                          String entidad, Integer idRegistro, String descripcion) {
        try (Connection cn = ConexionBD.obtenerConexion()) {
            registrar(cn, idUsuario, nombreUsuario, accion, entidad, idRegistro, descripcion);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar la auditoría.", e);
        }
    }

    // Usa la conexión de una transacción: si la operación principal falla, la auditoría también se deshace
    public void registrar(Connection cn, Integer idUsuario, String nombreUsuario, AccionAuditoria accion,
                          String entidad, Integer idRegistro, String descripcion) {
        String sql = "INSERT INTO dbo.Auditoria (IdUsuario, NombreUsuario, Accion, Entidad, IdRegistro, Descripcion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (idUsuario == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, idUsuario);
            }
            ps.setString(2, nombreUsuario);
            ps.setString(3, accion.name());
            ps.setString(4, entidad);
            if (idRegistro == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, idRegistro);
            }
            ps.setString(6, descripcion != null && descripcion.length() > 500 ? descripcion.substring(0, 500) : descripcion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar la auditoría.", e);
        }
    }

    /**
     * Consulta con filtros opcionales: cualquier parámetro null se ignora.
     * El SQL se arma agregando condiciones, pero los valores siempre van como parámetros (?).
     */
    public List<Auditoria> consultar(String nombreUsuario, String entidad, String accion,
                                     LocalDate desde, LocalDate hasta) {
        StringBuilder sql = new StringBuilder(
                "SELECT IdAuditoria, IdUsuario, NombreUsuario, FechaHora, Accion, Entidad, IdRegistro, Descripcion "
                + "FROM dbo.Auditoria WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (nombreUsuario != null) {
            sql.append(" AND NombreUsuario = ?");
            parametros.add(nombreUsuario);
        }
        if (entidad != null) {
            sql.append(" AND Entidad = ?");
            parametros.add(entidad);
        }
        if (accion != null) {
            sql.append(" AND Accion = ?");
            parametros.add(accion);
        }
        if (desde != null) {
            sql.append(" AND FechaHora >= ?");
            parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
        }
        if (hasta != null) {
            sql.append(" AND FechaHora < ?");   // < día siguiente: incluye todo el día "hasta"
            parametros.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
        }
        sql.append(" ORDER BY FechaHora DESC, IdAuditoria DESC");

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            List<Auditoria> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Auditoria a = new Auditoria();
                    a.setIdAuditoria(rs.getLong("IdAuditoria"));
                    a.setIdUsuario((Integer) rs.getObject("IdUsuario"));
                    a.setNombreUsuario(rs.getString("NombreUsuario"));
                    a.setFechaHora(rs.getTimestamp("FechaHora").toLocalDateTime());
                    a.setAccion(rs.getString("Accion"));
                    a.setEntidad(rs.getString("Entidad"));
                    a.setIdRegistro((Integer) rs.getObject("IdRegistro"));
                    a.setDescripcion(rs.getString("Descripcion"));
                    lista.add(a);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar la auditoría.", e);
        }
    }
}

