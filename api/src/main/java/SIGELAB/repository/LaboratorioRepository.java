package SIGELAB.repository;

import SIGELAB.enums.EstadoRegistro;
import SIGELAB.model.Laboratorio;
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
 * Único lugar donde se escribe SQL para la tabla dbo.Laboratorio.
 * No valida reglas de negocio: solo lee y escribe datos.
 */
public class LaboratorioRepository {

    private static final String COLUMNAS = "IdLaboratorio, Codigo, Nombre, Ubicacion, Capacidad, Estado";

    public List<Laboratorio> listar() {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Laboratorio ORDER BY Codigo";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return leerLista(rs);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los laboratorios.", e);
        }
    }

    public List<Laboratorio> listarActivos() {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Laboratorio WHERE Estado = 'ACTIVO' ORDER BY Codigo";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return leerLista(rs);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los laboratorios.", e);
        }
    }

    // Busca por código o nombre que contenga el texto (LIKE '%texto%')
    public List<Laboratorio> buscar(String texto) {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Laboratorio "
                + "WHERE Codigo LIKE ? OR Nombre LIKE ? ORDER BY Codigo";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            String patron = "%" + texto + "%";
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return leerLista(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo realizar la búsqueda de laboratorios.", e);
        }
    }

    public Optional<Laboratorio> buscarPorCodigo(String codigo) {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Laboratorio WHERE Codigo = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el laboratorio.", e);
        }
    }

    // Inserta y devuelve el Id generado por IDENTITY
    public int insertar(Laboratorio lab) {
        String sql = "INSERT INTO dbo.Laboratorio (Codigo, Nombre, Ubicacion, Capacidad, Estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lab.getCodigo());
            ps.setString(2, lab.getNombre());
            ps.setString(3, lab.getUbicacion());
            ps.setInt(4, lab.getCapacidad());
            ps.setString(5, lab.getEstado().name());
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el laboratorio.", e);
        }
    }

    public void actualizar(Laboratorio lab) {
        String sql = "UPDATE dbo.Laboratorio SET Codigo = ?, Nombre = ?, Ubicacion = ?, Capacidad = ?, Estado = ? "
                + "WHERE IdLaboratorio = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, lab.getCodigo());
            ps.setString(2, lab.getNombre());
            ps.setString(3, lab.getUbicacion());
            ps.setInt(4, lab.getCapacidad());
            ps.setString(5, lab.getEstado().name());
            ps.setInt(6, lab.getIdLaboratorio());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el laboratorio.", e);
        }
    }

    public void cambiarEstado(int idLaboratorio, EstadoRegistro estado) {
        String sql = "UPDATE dbo.Laboratorio SET Estado = ? WHERE IdLaboratorio = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idLaboratorio);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado del laboratorio.", e);
        }
    }

    private List<Laboratorio> leerLista(ResultSet rs) throws SQLException {
        List<Laboratorio> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    // Convierte la fila actual del ResultSet en un objeto Laboratorio
    private Laboratorio mapear(ResultSet rs) throws SQLException {
        return new Laboratorio(
                rs.getInt("IdLaboratorio"),
                rs.getString("Codigo"),
                rs.getString("Nombre"),
                rs.getString("Ubicacion"),
                rs.getInt("Capacidad"),
                EstadoRegistro.valueOf(rs.getString("Estado")));
    }
}

