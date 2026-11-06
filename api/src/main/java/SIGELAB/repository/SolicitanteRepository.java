package SIGELAB.repository;

import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.TipoSolicitante;
import SIGELAB.model.Solicitante;
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
 * SQL de la tabla dbo.Solicitante.
 */
public class SolicitanteRepository {

    private static final String SELECT =
            "SELECT IdSolicitante, Carne, Nombre, Apellido, Tipo, Correo, Telefono, Estado FROM dbo.Solicitante ";

    public List<Solicitante> listar() {
        return consultar(SELECT + "ORDER BY Apellido, Nombre");
    }

    public List<Solicitante> listarActivos() {
        return consultar(SELECT + "WHERE Estado = 'ACTIVO' ORDER BY Apellido, Nombre");
    }

    public List<Solicitante> buscar(String texto) {
        String patron = "%" + texto + "%";
        return consultar(SELECT + "WHERE Carne LIKE ? OR Nombre LIKE ? OR Apellido LIKE ? ORDER BY Apellido, Nombre",
                patron, patron, patron);
    }

    public Optional<Solicitante> buscarPorCarne(String carne) {
        List<Solicitante> lista = consultar(SELECT + "WHERE Carne = ?", carne);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public Optional<Solicitante> buscarPorId(int idSolicitante) {
        List<Solicitante> lista = consultar(SELECT + "WHERE IdSolicitante = ?", String.valueOf(idSolicitante));
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    // Solicitante dueño de un préstamo (se usa para avisarle por correo)
    public Optional<Solicitante> buscarPorPrestamo(int idPrestamo) {
        List<Solicitante> lista = consultar(SELECT + "WHERE IdSolicitante = (SELECT IdSolicitante FROM dbo.Prestamo WHERE IdPrestamo = ?)",
                String.valueOf(idPrestamo));
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public int insertar(Solicitante s) {
        String sql = "INSERT INTO dbo.Solicitante (Carne, Nombre, Apellido, Tipo, Correo, Telefono, Estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            asignarParametros(ps, s);
            ps.executeUpdate();
            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                return llaves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el solicitante.", e);
        }
    }

    public void actualizar(Solicitante s) {
        String sql = "UPDATE dbo.Solicitante SET Carne = ?, Nombre = ?, Apellido = ?, Tipo = ?, Correo = ?, "
                + "Telefono = ?, Estado = ? WHERE IdSolicitante = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            asignarParametros(ps, s);
            ps.setInt(8, s.getIdSolicitante());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el solicitante.", e);
        }
    }

    public void cambiarEstado(int idSolicitante, EstadoRegistro estado) {
        String sql = "UPDATE dbo.Solicitante SET Estado = ? WHERE IdSolicitante = ?";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idSolicitante);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cambiar el estado del solicitante.", e);
        }
    }

    private void asignarParametros(PreparedStatement ps, Solicitante s) throws SQLException {
        ps.setString(1, s.getCarne());
        ps.setString(2, s.getNombre());
        ps.setString(3, s.getApellido());
        ps.setString(4, s.getTipo().name());
        ps.setString(5, s.getCorreo());
        ps.setString(6, s.getTelefono());
        ps.setString(7, s.getEstado().name());
    }

    private List<Solicitante> consultar(String sql, String... parametros) {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            List<Solicitante> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Solicitante s = new Solicitante();
                    s.setIdSolicitante(rs.getInt("IdSolicitante"));
                    s.setCarne(rs.getString("Carne"));
                    s.setNombre(rs.getString("Nombre"));
                    s.setApellido(rs.getString("Apellido"));
                    s.setTipo(TipoSolicitante.valueOf(rs.getString("Tipo")));
                    s.setCorreo(rs.getString("Correo"));
                    s.setTelefono(rs.getString("Telefono"));
                    s.setEstado(EstadoRegistro.valueOf(rs.getString("Estado")));
                    lista.add(s);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los solicitantes.", e);
        }
    }
}

