package SIGELAB.repository;

import SIGELAB.model.ResultadoReporte;
import SIGELAB.model.ResumenDashboard;
import SIGELAB.util.ConexionBD;
import SIGELAB.util.DataAccessException;
import SIGELAB.util.Fechas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de solo lectura para Reportes, Dashboard e historial (sección 6.11).
 * Aquí se demuestra el uso de SELECT, JOIN, WHERE, ORDER BY, BETWEEN, COUNT y GROUP BY.
 */
public class ReporteRepository {

    public ResumenDashboard resumenDashboard() {
        String sql = "SELECT TotalEquipos, Disponibles, Prestados, Danados, EnMantenimiento, DeBaja, "
                + "PrestamosActivos, PrestamosVencidos FROM dbo.vw_Dashboard";
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new ResumenDashboard(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                    rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8));
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cargar el resumen del dashboard.", e);
        }
    }

    // COUNT + GROUP BY: cuántos equipos hay en cada estado
    public ResultadoReporte equiposPorEstadoResumen(Integer idLaboratorio) {
        String sql = "SELECT e.Estado, COUNT(*) AS Cantidad FROM dbo.Equipo e "
                + (idLaboratorio == null ? "" : "WHERE e.IdLaboratorio = ? ")
                + "GROUP BY e.Estado ORDER BY Cantidad DESC";
        return ejecutar(sql, idLaboratorio == null ? new Object[0] : new Object[]{idLaboratorio});
    }

    // Detalle de los equipos que están en un estado concreto
    public ResultadoReporte equiposEnEstado(String estado, Integer idLaboratorio) {
        String sql = "SELECT e.CodigoInventario AS Codigo, e.Nombre, t.Nombre AS Tipo, e.Marca, e.Modelo, "
                + "l.Codigo AS Laboratorio, e.Estado, e.Observaciones "
                + "FROM dbo.Equipo e JOIN dbo.TipoEquipo t ON t.IdTipoEquipo = e.IdTipoEquipo "
                + "JOIN dbo.Laboratorio l ON l.IdLaboratorio = e.IdLaboratorio WHERE e.Estado = ? "
                + (idLaboratorio == null ? "" : "AND e.IdLaboratorio = ? ")
                + "ORDER BY e.CodigoInventario";
        return ejecutar(sql, idLaboratorio == null ? new Object[]{estado} : new Object[]{estado, idLaboratorio});
    }

    private static final String COLUMNAS_PRESTAMO =
            "SELECT IdPrestamo AS [No.], Carne AS [Carné], Solicitante, FechaPrestamo AS [Fecha préstamo], "
            + "FechaEsperadaDevolucion AS [Fecha esperada], FechaFinalizacion AS [Fecha finalización], "
            + "RegistradoPor AS [Registrado por], Estado, TotalEquipos AS [Equipos], EquiposPendientes AS [Pendientes], "
            + "CASE WHEN EsVencido = 1 THEN 'SÍ' ELSE 'NO' END AS Vencido FROM dbo.vw_PrestamoResumen ";

    public ResultadoReporte prestamosPorEstado(String estado) {
        return ejecutar(COLUMNAS_PRESTAMO + "WHERE Estado = ? ORDER BY FechaPrestamo DESC", estado);
    }

    // RN-17: vencido = fecha esperada pasada y todavía con equipos pendientes (lo calcula la vista)
    public ResultadoReporte prestamosVencidos() {
        return ejecutar(COLUMNAS_PRESTAMO + "WHERE EsVencido = 1 ORDER BY FechaEsperadaDevolucion");
    }

    // BETWEEN entre dos fechas (se toma el día "hasta" completo)
    public ResultadoReporte prestamosEntreFechas(LocalDate desde, LocalDate hasta) {
        return ejecutar(COLUMNAS_PRESTAMO + "WHERE FechaPrestamo BETWEEN ? AND ? ORDER BY FechaPrestamo",
                inicio(desde), fin(hasta));
    }

    public ResultadoReporte devolucionesEntreFechas(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT dv.FechaDevolucion AS [Fecha devolución], p.IdPrestamo AS [No. préstamo], "
                + "e.CodigoInventario AS Codigo, e.Nombre AS Equipo, s.Nombre + ' ' + s.Apellido AS Solicitante, "
                + "dv.Condicion AS [Condición], u.NombreUsuario AS [Recibido por], dv.Observaciones "
                + "FROM dbo.Devolucion dv "
                + "JOIN dbo.DetallePrestamo d ON d.IdDetallePrestamo = dv.IdDetallePrestamo "
                + "JOIN dbo.Prestamo p ON p.IdPrestamo = d.IdPrestamo "
                + "JOIN dbo.Equipo e ON e.IdEquipo = d.IdEquipo "
                + "JOIN dbo.Solicitante s ON s.IdSolicitante = p.IdSolicitante "
                + "JOIN dbo.Usuario u ON u.IdUsuario = dv.IdUsuarioRecibe "
                + "WHERE dv.FechaDevolucion BETWEEN ? AND ? ORDER BY dv.FechaDevolucion";
        return ejecutar(sql, inicio(desde), fin(hasta));
    }

    // Historial completo de un equipo: sus préstamos y sus mantenimientos, en orden cronológico
    public ResultadoReporte historialEquipo(String codigoInventario) {
        String sql = "SELECT * FROM ("
                + " SELECT p.FechaPrestamo AS Fecha, 'PRÉSTAMO' AS Movimiento, "
                + "  'Préstamo No. ' + CAST(p.IdPrestamo AS varchar(10)) + ' a ' + s.Nombre + ' ' + s.Apellido AS Detalle, "
                + "  CASE WHEN d.Estado = 'PENDIENTE' THEN 'PENDIENTE' "
                + "       ELSE 'Devuelto ' + dv.Condicion + ' el ' + CONVERT(varchar(10), dv.FechaDevolucion, 103) END AS Resultado, "
                + "  u.NombreUsuario AS Usuario "
                + " FROM dbo.DetallePrestamo d "
                + " JOIN dbo.Prestamo p ON p.IdPrestamo = d.IdPrestamo "
                + " JOIN dbo.Solicitante s ON s.IdSolicitante = p.IdSolicitante "
                + " JOIN dbo.Usuario u ON u.IdUsuario = p.IdUsuarioRegistra "
                + " JOIN dbo.Equipo e ON e.IdEquipo = d.IdEquipo "
                + " LEFT JOIN dbo.Devolucion dv ON dv.IdDetallePrestamo = d.IdDetallePrestamo "
                + " WHERE e.CodigoInventario = ? "
                + " UNION ALL "
                + " SELECT m.FechaIngreso, 'MANTENIMIENTO', m.TipoMantenimiento + ': ' + m.Descripcion, "
                + "  CASE WHEN m.Estado = 'EN_PROCESO' THEN 'EN PROCESO' "
                + "       ELSE m.Resultado + ' el ' + CONVERT(varchar(10), m.FechaSalida, 103) END, "
                + "  u.NombreUsuario "
                + " FROM dbo.Mantenimiento m "
                + " JOIN dbo.Equipo e ON e.IdEquipo = m.IdEquipo "
                + " JOIN dbo.Usuario u ON u.IdUsuario = m.IdUsuarioRegistra "
                + " WHERE e.CodigoInventario = ?"
                + ") h ORDER BY Fecha DESC";
        return ejecutar(sql, codigoInventario, codigoInventario);
    }

    // Filtros opcionales: código de equipo, laboratorio y rango de fechas de ingreso
    public ResultadoReporte historialMantenimiento(String codigoInventario, Integer idLaboratorio,
                                                   LocalDate desde, LocalDate hasta) {
        StringBuilder sql = new StringBuilder(
                "SELECT m.IdMantenimiento AS ID, e.CodigoInventario AS Codigo, e.Nombre AS Equipo, l.Codigo AS Laboratorio, "
                + "m.TipoMantenimiento AS Tipo, m.Descripcion AS [Descripción], m.Responsable, "
                + "m.FechaIngreso AS [Fecha ingreso], m.FechaSalida AS [Fecha salida], m.Resultado, m.Estado "
                + "FROM dbo.Mantenimiento m JOIN dbo.Equipo e ON e.IdEquipo = m.IdEquipo "
                + "JOIN dbo.Laboratorio l ON l.IdLaboratorio = e.IdLaboratorio WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (codigoInventario != null) {
            sql.append(" AND e.CodigoInventario = ?");
            parametros.add(codigoInventario);
        }
        if (idLaboratorio != null) {
            sql.append(" AND e.IdLaboratorio = ?");
            parametros.add(idLaboratorio);
        }
        if (desde != null && hasta != null) {
            sql.append(" AND m.FechaIngreso BETWEEN ? AND ?");
            parametros.add(inicio(desde));
            parametros.add(fin(hasta));
        }
        sql.append(" ORDER BY m.FechaIngreso DESC");
        return ejecutar(sql.toString(), parametros.toArray());
    }

    // COUNT + GROUP BY + ORDER BY DESC: ranking de equipos por número de préstamos
    public ResultadoReporte equiposMasPrestados(Integer idLaboratorio) {
        String sql = "SELECT e.CodigoInventario AS Codigo, e.Nombre AS Equipo, t.Nombre AS Tipo, l.Codigo AS Laboratorio, "
                + "COUNT(d.IdDetallePrestamo) AS [Veces prestado] "
                + "FROM dbo.Equipo e JOIN dbo.TipoEquipo t ON t.IdTipoEquipo = e.IdTipoEquipo "
                + "JOIN dbo.Laboratorio l ON l.IdLaboratorio = e.IdLaboratorio "
                + "LEFT JOIN dbo.DetallePrestamo d ON d.IdEquipo = e.IdEquipo "
                + (idLaboratorio == null ? "" : "WHERE e.IdLaboratorio = ? ")
                + "GROUP BY e.CodigoInventario, e.Nombre, t.Nombre, l.Codigo "
                + "ORDER BY [Veces prestado] DESC, e.CodigoInventario";
        return ejecutar(sql, idLaboratorio == null ? new Object[0] : new Object[]{idLaboratorio});
    }

    private Timestamp inicio(LocalDate fecha) {
        return Timestamp.valueOf(fecha.atStartOfDay());
    }

    private Timestamp fin(LocalDate fecha) {
        return Timestamp.valueOf(fecha.atTime(23, 59, 59));
    }

    // Ejecuta cualquier SELECT y arma el resultado usando los nombres de columna de la consulta
    private ResultadoReporte ejecutar(String sql, Object... parametros) {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                List<String> columnas = new ArrayList<>();
                for (int c = 1; c <= meta.getColumnCount(); c++) {
                    columnas.add(meta.getColumnLabel(c));
                }
                ResultadoReporte resultado = new ResultadoReporte(columnas);
                while (rs.next()) {
                    Object[] fila = new Object[columnas.size()];
                    for (int c = 0; c < fila.length; c++) {
                        Object valor = rs.getObject(c + 1);
                        // Las fechas se muestran como dd/MM/yyyy HH:mm
                        fila[c] = valor instanceof Timestamp ts ? Fechas.formatear(ts.toLocalDateTime()) : valor;
                    }
                    resultado.agregarFila(fila);
                }
                return resultado;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo generar el reporte.", e);
        }
    }
}

