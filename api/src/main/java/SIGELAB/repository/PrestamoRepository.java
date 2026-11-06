package SIGELAB.repository;

import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.enums.EstadoDetalle;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoPrestamo;
import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.Equipo;
import SIGELAB.model.Prestamo;
import SIGELAB.model.Solicitante;
import SIGELAB.util.ConexionBD;
import SIGELAB.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL de dbo.Prestamo y dbo.DetallePrestamo.
 * Los métodos que reciben Connection se usan dentro de una transacción (Transaccion.ejecutar).
 */
public class PrestamoRepository {

    /** Serializa devoluciones del mismo préstamo, incluso si son equipos diferentes. */
    public void bloquearPrestamoPorDetalle(Connection cn, int detalleId) {
        String sql = "SELECT p.IdPrestamo FROM dbo.Prestamo p WITH (UPDLOCK, HOLDLOCK) "
                + "INNER JOIN dbo.DetallePrestamo d ON d.IdPrestamo = p.IdPrestamo WHERE d.IdDetallePrestamo = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, detalleId);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new SIGELAB.util.BusinessException("Detalle de préstamo no encontrado."); }
        } catch (SQLException ex) { throw new DataAccessException("No se pudo bloquear el préstamo para registrar su devolución.", ex); }
    }

    // vw_PrestamoResumen ya calcula total de equipos, pendientes y si está vencido (RN-17)
    private static final String SELECT_RESUMEN =
            "SELECT IdPrestamo, FechaPrestamo, FechaEsperadaDevolucion, FechaFinalizacion, Estado, Carne, Solicitante, "
            + "RegistradoPor, TotalEquipos, EquiposPendientes, EsVencido FROM dbo.vw_PrestamoResumen ";

    private static final String SELECT_DETALLE =
            "SELECT d.IdDetallePrestamo, d.IdPrestamo, p.Estado AS EstadoPrestamo, d.Estado AS EstadoDetalle, "
            + "e.IdEquipo, e.CodigoInventario, e.Nombre AS NombreEquipo, e.Estado AS EstadoEquipo, "
            + "dv.Condicion, dv.FechaDevolucion, dv.Observaciones AS ObsDevolucion "
            + "FROM dbo.DetallePrestamo d "
            + "INNER JOIN dbo.Prestamo p ON p.IdPrestamo = d.IdPrestamo "
            + "INNER JOIN dbo.Equipo e ON e.IdEquipo = d.IdEquipo "
            + "LEFT JOIN dbo.Devolucion dv ON dv.IdDetallePrestamo = d.IdDetallePrestamo ";

    // ---------------------------------------------------------------- escritura (transaccional)

    public int insertarEncabezado(Connection cn, Prestamo p, int idUsuarioRegistra) {
        // FechaPrestamo no se envía: SQL Server pone CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0)) por defecto
        String sql = "INSERT INTO dbo.Prestamo (IdSolicitante, FechaEsperadaDevolucion, IdUsuarioRegistra, Estado, Observaciones) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getSolicitante().getIdSolicitante());
            ps.setTimestamp(2, Timestamp.valueOf(p.getFechaEsperadaDevolucion()));
            ps.setInt(3, idUsuarioRegistra);
            ps.setString(4, EstadoPrestamo.ACTIVO.name());
            ps.setString(5, p.getObservaciones());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el encabezado del préstamo.", e);
        }
    }

    public void insertarDetalle(Connection cn, int idPrestamo, int idEquipo) {
        String sql = "INSERT INTO dbo.DetallePrestamo (IdPrestamo, IdEquipo, Estado) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            ps.setInt(2, idEquipo);
            ps.setString(3, EstadoDetalle.PENDIENTE.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el detalle del préstamo.", e);
        }
    }

    // Solo cambia si sigue PENDIENTE: evita devolver dos veces el mismo equipo (RN-11)
    public boolean marcarDetalleDevuelto(Connection cn, int idDetallePrestamo) {
        String sql = "UPDATE dbo.DetallePrestamo SET Estado = 'DEVUELTO' WHERE IdDetallePrestamo = ? AND Estado = 'PENDIENTE'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idDetallePrestamo);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el detalle del préstamo.", e);
        }
    }

    public int contarPendientes(Connection cn, int idPrestamo) {
        String sql = "SELECT COUNT(*) FROM dbo.DetallePrestamo WHERE IdPrestamo = ? AND Estado = 'PENDIENTE'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron contar los equipos pendientes.", e);
        }
    }

    public void finalizar(Connection cn, int idPrestamo) {
        String sql = "UPDATE dbo.Prestamo SET Estado = 'FINALIZADO', FechaFinalizacion = CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0)) "
                + "WHERE IdPrestamo = ? AND Estado = 'ACTIVO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo finalizar el préstamo.", e);
        }
    }

    // ---------------------------------------------------------------- consultas

    /**
     * @param texto  busca por No. de préstamo, carné o nombre del solicitante (null = todos)
     * @param filtro ACTIVO, FINALIZADO, VENCIDO o null para todos
     */
    public List<Prestamo> listar(String texto, String filtro) {
        StringBuilder sql = new StringBuilder(SELECT_RESUMEN).append("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (texto != null) {
            sql.append(" AND (CAST(IdPrestamo AS varchar(10)) = ? OR Carne LIKE ? OR Solicitante LIKE ?)");
            parametros.add(texto);
            parametros.add("%" + texto + "%");
            parametros.add("%" + texto + "%");
        }
        if ("VENCIDO".equals(filtro)) {
            sql.append(" AND EsVencido = 1");
        } else if (filtro != null) {
            sql.append(" AND Estado = ?");
            parametros.add(filtro);
        }
        sql.append(" ORDER BY IdPrestamo DESC");

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            List<Prestamo> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearResumen(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los préstamos.", e);
        }
    }

    public List<DetallePrestamo> listarDetalles(int idPrestamo) {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_DETALLE + "WHERE d.IdPrestamo = ? ORDER BY e.CodigoInventario")) {
            ps.setInt(1, idPrestamo);
            List<DetallePrestamo> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearDetalle(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el detalle del préstamo.", e);
        }
    }

    public Optional<DetallePrestamo> buscarDetalle(Connection cn, int idDetallePrestamo) {
        try (PreparedStatement ps = cn.prepareStatement(SELECT_DETALLE + "WHERE d.IdDetallePrestamo = ?")) {
            ps.setInt(1, idDetallePrestamo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearDetalle(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el detalle del préstamo.", e);
        }
    }

    private Prestamo mapearResumen(ResultSet rs) throws SQLException {
        Prestamo p = new Prestamo();
        p.setIdPrestamo(rs.getInt("IdPrestamo"));
        p.setFechaPrestamo(rs.getTimestamp("FechaPrestamo").toLocalDateTime());
        p.setFechaEsperadaDevolucion(rs.getTimestamp("FechaEsperadaDevolucion").toLocalDateTime());
        Timestamp fin = rs.getTimestamp("FechaFinalizacion");
        p.setFechaFinalizacion(fin == null ? null : fin.toLocalDateTime());
        p.setEstado(EstadoPrestamo.valueOf(rs.getString("Estado")));
        Solicitante s = new Solicitante();
        s.setCarne(rs.getString("Carne"));
        s.setNombre(rs.getString("Solicitante"));
        s.setApellido("");
        p.setSolicitante(s);
        p.setUsuarioRegistra(rs.getString("RegistradoPor"));
        p.setTotalEquipos(rs.getInt("TotalEquipos"));
        p.setEquiposPendientes(rs.getInt("EquiposPendientes"));
        p.setVencido(rs.getBoolean("EsVencido"));
        return p;
    }

    private DetallePrestamo mapearDetalle(ResultSet rs) throws SQLException {
        DetallePrestamo d = new DetallePrestamo();
        d.setIdDetallePrestamo(rs.getInt("IdDetallePrestamo"));
        d.setIdPrestamo(rs.getInt("IdPrestamo"));
        d.setEstadoPrestamo(EstadoPrestamo.valueOf(rs.getString("EstadoPrestamo")));
        d.setEstado(EstadoDetalle.valueOf(rs.getString("EstadoDetalle")));
        Equipo e = new Equipo();
        e.setIdEquipo(rs.getInt("IdEquipo"));
        e.setCodigoInventario(rs.getString("CodigoInventario"));
        e.setNombre(rs.getString("NombreEquipo"));
        e.setEstado(EstadoEquipo.valueOf(rs.getString("EstadoEquipo")));
        d.setEquipo(e);
        String condicion = rs.getString("Condicion");
        d.setCondicion(condicion == null ? null : CondicionDevolucion.valueOf(condicion));
        Timestamp fecha = rs.getTimestamp("FechaDevolucion");
        d.setFechaDevolucion(fecha == null ? null : fecha.toLocalDateTime());
        d.setObservacionDevolucion(rs.getString("ObsDevolucion"));
        return d;
    }
}

