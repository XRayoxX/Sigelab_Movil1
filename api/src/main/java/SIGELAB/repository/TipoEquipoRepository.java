package SIGELAB.repository;

import SIGELAB.enums.EstadoRegistro;
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
 * SQL de la tabla dbo.TipoEquipo.
 */
public class TipoEquipoRepository {

    private static final String SELECT = "SELECT IdTipoEquipo, Nombre, Descripcion, Estado FROM dbo.TipoEquipo ";

    public List<TipoEquipo> listar() {
        return consultar(SELECT + "ORDER BY Nombre");
    }

    public List<TipoEquipo> listarActivos() {
        return consultar(SELECT + "WHERE Estado = 'ACTIVO' ORDER BY Nombre");
    }

    public List<TipoEquipo> buscar(String texto) {
        return consultar(SELECT + "WHERE Nombre LIKE ? OR Descripcion LIKE ? ORDER BY Nombre",
                "%" + texto + "%", "%" + texto + "%");
    }

    public Optional<TipoEquipo> buscarPorNombre(String nombre) {
        List<TipoEquipo> lista = consultar(SELECT + "WHERE Nombre = ?", nombre);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public int insertar(TipoEquipo tipo) {
        String sql = "INSERT INTO dbo.TipoEquipo (Nombre, Descripcion, Estado) VALUES (?, ?, ?)";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setString(3, tipo.getEstado().name());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el tipo de equipo.", e);
        }
    }

    public void actualizar(TipoEquipo tipo) {
        String sql = "UPDATE dbo.TipoEquipo SET Nombre = ?, Descripcion = ?, Estado = ? WHERE IdTipoEquipo = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setString(3, tipo.getEstado().name());
            ps.setInt(4, tipo.getIdTipoEquipo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el tipo de equipo.", e);
        }
    }

    public void cambiarEstado(int idTipoEquipo, EstadoRegistro estado) {
        String sql = "UPDATE dbo.TipoEquipo SET Estado = ? WHERE IdTipoEquipo = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idTipoEquipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado del tipo de equipo.", e);
        }
    }

    private List<TipoEquipo> consultar(String sql, String... parametros) {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            List<TipoEquipo> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new TipoEquipo(
                            rs.getInt("IdTipoEquipo"),
                            rs.getString("Nombre"),
                            rs.getString("Descripcion"),
                            EstadoRegistro.valueOf(rs.getString("Estado"))));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los tipos de equipo.", e);
        }
    }
}

