package SIGELAB.repository;

import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoMantenimiento;
import SIGELAB.enums.ResultadoMantenimiento;
import SIGELAB.enums.TipoMantenimiento;
import SIGELAB.model.Equipo;
import SIGELAB.model.Mantenimiento;
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
 * SQL de dbo.Mantenimiento. Las escrituras se usan dentro de una transacción.
 */
public class MantenimientoRepository {

    private static final String SELECT =
            "SELECT m.IdMantenimiento, m.FechaIngreso, m.TipoMantenimiento, m.Descripcion, m.Responsable, m.FechaSalida, "
            + "m.Resultado, m.Estado, m.Observaciones, e.IdEquipo, e.CodigoInventario, e.Nombre AS NombreEquipo, "
            + "e.Estado AS EstadoEquipo "
            + "FROM dbo.Mantenimiento m INNER JOIN dbo.Equipo e ON e.IdEquipo = m.IdEquipo ";

    public int insertar(Connection cn, Mantenimiento m, int idUsuarioRegistra) {
        // FechaIngreso y Estado (EN_PROCESO) los pone SQL Server por defecto
        String sql = "INSERT INTO dbo.Mantenimiento (IdEquipo, TipoMantenimiento, Descripcion, Responsable, IdUsuarioRegistra) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getEquipo().getIdEquipo());
            ps.setString(2, m.getTipo().name());
            ps.setString(3, m.getDescripcion());
            ps.setString(4, m.getResponsable());
            ps.setInt(5, idUsuarioRegistra);
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el mantenimiento.", e);
        }
    }

    // Solo cierra si sigue EN_PROCESO (no se puede finalizar dos veces)
    public boolean finalizar(Connection cn, int idMantenimiento, ResultadoMantenimiento resultado, String observaciones) {
        String sql = "UPDATE dbo.Mantenimiento SET Estado = 'FINALIZADO', FechaSalida = CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0)), Resultado = ?, "
                + "Observaciones = ? WHERE IdMantenimiento = ? AND Estado = 'EN_PROCESO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, resultado.name());
            ps.setString(2, observaciones);
            ps.setInt(3, idMantenimiento);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo finalizar el mantenimiento.", e);
        }
    }

    public Optional<Mantenimiento> buscarPorId(Connection cn, int idMantenimiento) {
        try (PreparedStatement ps = cn.prepareStatement(SELECT + "WHERE m.IdMantenimiento = ?")) {
            ps.setInt(1, idMantenimiento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el mantenimiento.", e);
        }
    }

    public List<Mantenimiento> listar(String texto, EstadoMantenimiento estado) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (texto != null) {
            sql.append(" AND (e.CodigoInventario LIKE ? OR e.Nombre LIKE ? OR m.Responsable LIKE ?)");
            for (int i = 0; i < 3; i++) {
                parametros.add("%" + texto + "%");
            }
        }
        if (estado != null) {
            sql.append(" AND m.Estado = ?");
            parametros.add(estado.name());
        }
        sql.append(" ORDER BY m.FechaIngreso DESC");
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            List<Mantenimiento> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los mantenimientos.", e);
        }
    }

    private Mantenimiento mapear(ResultSet rs) throws SQLException {
        Mantenimiento m = new Mantenimiento();
        m.setIdMantenimiento(rs.getInt("IdMantenimiento"));
        m.setFechaIngreso(rs.getTimestamp("FechaIngreso").toLocalDateTime());
        m.setTipo(TipoMantenimiento.valueOf(rs.getString("TipoMantenimiento")));
        m.setDescripcion(rs.getString("Descripcion"));
        m.setResponsable(rs.getString("Responsable"));
        Timestamp salida = rs.getTimestamp("FechaSalida");
        m.setFechaSalida(salida == null ? null : salida.toLocalDateTime());
        String resultado = rs.getString("Resultado");
        m.setResultado(resultado == null ? null : ResultadoMantenimiento.valueOf(resultado));
        m.setEstado(EstadoMantenimiento.valueOf(rs.getString("Estado")));
        m.setObservaciones(rs.getString("Observaciones"));
        Equipo e = new Equipo();
        e.setIdEquipo(rs.getInt("IdEquipo"));
        e.setCodigoInventario(rs.getString("CodigoInventario"));
        e.setNombre(rs.getString("NombreEquipo"));
        e.setEstado(EstadoEquipo.valueOf(rs.getString("EstadoEquipo")));
        m.setEquipo(e);
        return m;
    }
}

