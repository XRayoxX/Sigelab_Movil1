package SIGELAB.repository;

import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.model.Equipo;
import SIGELAB.model.Laboratorio;
import SIGELAB.model.TipoEquipo;
import SIGELAB.util.ConexionBD;
import SIGELAB.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL de dbo.Equipo. Usa JOIN con TipoEquipo y Laboratorio para traer sus nombres.
 */
public class EquipoRepository {

    private static final String SELECT =
            "SELECT e.IdEquipo, e.CodigoInventario, e.Nombre, e.Marca, e.Modelo, e.NumeroSerie, e.Estado, e.Observaciones, "
            + "t.IdTipoEquipo, t.Nombre AS NombreTipo, t.Estado AS EstadoTipo, "
            + "l.IdLaboratorio, l.Codigo AS CodigoLab, l.Nombre AS NombreLab, l.Ubicacion, l.Capacidad, l.Estado AS EstadoLab "
            + "FROM dbo.Equipo e "
            + "INNER JOIN dbo.TipoEquipo t ON t.IdTipoEquipo = e.IdTipoEquipo "
            + "INNER JOIN dbo.Laboratorio l ON l.IdLaboratorio = e.IdLaboratorio ";

    /**
     * Lista con filtros opcionales (null = sin filtro).
     * texto busca en código, nombre, marca y modelo.
     */
    public List<Equipo> listar(String texto, EstadoEquipo estado, Integer idLaboratorio) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (texto != null) {
            sql.append(" AND (e.CodigoInventario LIKE ? OR e.Nombre LIKE ? OR e.Marca LIKE ? OR e.Modelo LIKE ? OR e.NumeroSerie LIKE ?)");
            for (int i = 0; i < 5; i++) {
                parametros.add("%" + texto + "%");
            }
        }
        if (estado != null) {
            sql.append(" AND e.Estado = ?");
            parametros.add(estado.name());
        }
        if (idLaboratorio != null) {
            sql.append(" AND e.IdLaboratorio = ?");
            parametros.add(idLaboratorio);
        }
        sql.append(" ORDER BY e.CodigoInventario");
        return consultar(sql.toString(), parametros.toArray());
    }

    public List<Equipo> listarPorEstados(EstadoEquipo... estados) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE e.Estado IN (");
        for (int i = 0; i < estados.length; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(") ORDER BY e.CodigoInventario");
        Object[] parametros = new Object[estados.length];
        for (int i = 0; i < estados.length; i++) {
            parametros[i] = estados[i].name();
        }
        return consultar(sql.toString(), parametros);
    }

    // Búsqueda por el código leído del QR o escrito por el usuario
    public Optional<Equipo> buscarPorCodigo(String codigoInventario) {
        List<Equipo> lista = consultar(SELECT + "WHERE e.CodigoInventario = ?", codigoInventario);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public Optional<Equipo> buscarPorNumeroSerie(String numeroSerie) {
        List<Equipo> lista = consultar(SELECT + "WHERE e.NumeroSerie = ?", numeroSerie);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    // Versión transaccional: lee el estado actual dentro de la misma transacción
    public Optional<Equipo> buscarPorId(Connection cn, int idEquipo) {
        try (PreparedStatement ps = cn.prepareStatement(SELECT + "WHERE e.IdEquipo = ?")) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el equipo.", e);
        }
    }

    public Optional<Equipo> buscarPorId(int idEquipo) {
        try (Connection cn = ConexionBD.obtenerConexion()) {
            return buscarPorId(cn, idEquipo);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el equipo.", e);
        }
    }

    public int insertar(Equipo e) {
        String sql = "INSERT INTO dbo.Equipo (CodigoInventario, Nombre, IdTipoEquipo, Marca, Modelo, NumeroSerie, "
                + "IdLaboratorio, Estado, Observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getCodigoInventario());
            ps.setString(2, e.getNombre());
            ps.setInt(3, e.getTipoEquipo().getIdTipoEquipo());
            ps.setString(4, e.getMarca());
            ps.setString(5, e.getModelo());
            ps.setString(6, e.getNumeroSerie());
            ps.setInt(7, e.getLaboratorio().getIdLaboratorio());
            ps.setString(8, e.getEstado().name());
            ps.setString(9, e.getObservaciones());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("No se pudo guardar el equipo.", ex);
        }
    }

    // Actualiza los datos descriptivos. El Estado NO se cambia aquí: solo lo cambian los procesos.
    public void actualizarDatos(Equipo e) {
        String sql = "UPDATE dbo.Equipo SET CodigoInventario = ?, Nombre = ?, IdTipoEquipo = ?, Marca = ?, Modelo = ?, "
                + "NumeroSerie = ?, IdLaboratorio = ?, Observaciones = ? WHERE IdEquipo = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, e.getCodigoInventario());
            ps.setString(2, e.getNombre());
            ps.setInt(3, e.getTipoEquipo().getIdTipoEquipo());
            ps.setString(4, e.getMarca());
            ps.setString(5, e.getModelo());
            ps.setString(6, e.getNumeroSerie());
            ps.setInt(7, e.getLaboratorio().getIdLaboratorio());
            ps.setString(8, e.getObservaciones());
            ps.setInt(9, e.getIdEquipo());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("No se pudo actualizar el equipo.", ex);
        }
    }

    /**
     * Cambia el estado SOLO si el equipo sigue en el estado esperado.
     * Devuelve false si otro usuario lo cambió antes (así nunca se presta dos veces el mismo equipo).
     */
    public boolean cambiarEstado(Connection cn, int idEquipo, EstadoEquipo esperado, EstadoEquipo nuevo) {
        String sql = "UPDATE dbo.Equipo SET Estado = ? WHERE IdEquipo = ? AND Estado = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevo.name());
            ps.setInt(2, idEquipo);
            ps.setString(3, esperado.name());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado del equipo.", e);
        }
    }

    private List<Equipo> consultar(String sql, Object... parametros) {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            List<Equipo> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los equipos.", e);
        }
    }

    private Equipo mapear(ResultSet rs) throws SQLException {
        Equipo e = new Equipo();
        e.setIdEquipo(rs.getInt("IdEquipo"));
        e.setCodigoInventario(rs.getString("CodigoInventario"));
        e.setNombre(rs.getString("Nombre"));
        e.setMarca(rs.getString("Marca"));
        e.setModelo(rs.getString("Modelo"));
        e.setNumeroSerie(rs.getString("NumeroSerie"));
        e.setEstado(EstadoEquipo.valueOf(rs.getString("Estado")));
        e.setObservaciones(rs.getString("Observaciones"));
        e.setTipoEquipo(new TipoEquipo(rs.getInt("IdTipoEquipo"), rs.getString("NombreTipo"), null,
                EstadoRegistro.valueOf(rs.getString("EstadoTipo"))));
        e.setLaboratorio(new Laboratorio(rs.getInt("IdLaboratorio"), rs.getString("CodigoLab"), rs.getString("NombreLab"),
                rs.getString("Ubicacion"), rs.getInt("Capacidad"), EstadoRegistro.valueOf(rs.getString("EstadoLab"))));
        return e;
    }
}


