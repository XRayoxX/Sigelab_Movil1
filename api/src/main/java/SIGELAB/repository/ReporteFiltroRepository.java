
package SIGELAB.repository;

import SIGELAB.model.ResultadoReporte;
import SIGELAB.util.ConexionBD;
import SIGELAB.util.DataAccessException;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteFiltroRepository {

    public ResultadoReporte consultar(
            String tipo,
            Integer laboratorio,
            String codigo,
            String estado,
            LocalDate desde,
            LocalDate hasta) {

        Consulta q = new Consulta();

        switch (tipo) {
            case "Equipos por estado" ->
                    equipos(q, laboratorio, codigo,
                            estado, desde, hasta);

            case "Préstamos activos",
                 "Préstamos finalizados",
                 "Préstamos vencidos",
                 "Préstamos entre fechas" ->
                    prestamos(q, tipo, laboratorio,
                            codigo, estado, desde, hasta);

            case "Historial de un equipo" ->
                    historial(q, laboratorio, codigo,
                            estado, desde, hasta);

            case "Historial de mantenimiento" ->
                    mantenimiento(q, laboratorio, codigo,
                            estado, desde, hasta);

            case "Devoluciones entre fechas" ->
                    devoluciones(q, laboratorio, codigo,
                            estado, desde, hasta);

            case "Equipos más prestados" ->
                    masPrestados(q, laboratorio, codigo,
                            estado, desde, hasta);

            default -> throw new IllegalArgumentException(
                    "Reporte desconocido: " + tipo
            );
        }

        return ejecutar(q);
    }

    private void equipos(
            Consulta q,
            Integer lab,
            String codigo,
            String estado,
            LocalDate desde,
            LocalDate hasta) {

        boolean detalle = codigo != null
                || estado != null;

        if (detalle) {
            q.sql.append("""
                SELECT e.CodigoInventario AS Codigo,
                       e.Nombre,
                       t.Nombre AS Tipo,
                       e.Marca,
                       e.Modelo,
                       l.Codigo AS Laboratorio,
                       e.Estado,
                       e.FechaRegistro AS Fecha,
                       e.Observaciones
                FROM dbo.Equipo e
                JOIN dbo.TipoEquipo t
                  ON t.IdTipoEquipo=e.IdTipoEquipo
                JOIN dbo.Laboratorio l
                  ON l.IdLaboratorio=e.IdLaboratorio
                WHERE 1=1
                """);
        } else {
            q.sql.append("""
                SELECT e.Estado,
                       COUNT(*) AS Cantidad
                FROM dbo.Equipo e
                WHERE 1=1
                """);
        }

        filtroEquipo(q, "e", lab, codigo, estado);
        filtroFecha(q, "e.FechaRegistro", desde, hasta);

        if (detalle) {
            q.sql.append(" ORDER BY e.CodigoInventario");
        } else {
            q.sql.append(
                    " GROUP BY e.Estado ORDER BY Cantidad DESC"
            );
        }
    }

    private void prestamos(
            Consulta q,
            String tipo,
            Integer lab,
            String codigo,
            String estado,
            LocalDate desde,
            LocalDate hasta) {

        q.sql.append("""
            SELECT p.IdPrestamo AS [No.],
                   p.Carne AS [Carne],
                   p.Solicitante,
                   p.FechaPrestamo AS [Fecha prestamo],
                   p.FechaEsperadaDevolucion AS [Fecha esperada],
                   p.FechaFinalizacion AS [Fecha finalizacion],
                   p.RegistradoPor AS [Registrado por],
                   p.Estado,
                   p.TotalEquipos AS Equipos,
                   p.EquiposPendientes AS Pendientes,
                   CASE WHEN p.EsVencido=1
                     THEN 'SI' ELSE 'NO'
                   END AS Vencido
            FROM dbo.vw_PrestamoResumen p
            WHERE 1=1
            """);

        switch (tipo) {
            case "Préstamos activos" ->
                    q.sql.append(" AND p.Estado='ACTIVO'");

            case "Préstamos finalizados" ->
                    q.sql.append(" AND p.Estado='FINALIZADO'");

            case "Préstamos vencidos" ->
                    q.sql.append(" AND p.EsVencido=1");
        }

        filtroFecha(q, "p.FechaPrestamo", desde, hasta);

        // EXISTS evita duplicar préstamos que tengan
        // varios equipos coincidentes.
        if (lab != null || codigo != null || estado != null) {

            q.sql.append("""
                AND EXISTS (
                    SELECT 1
                    FROM dbo.DetallePrestamo d
                    JOIN dbo.Equipo e
                      ON e.IdEquipo=d.IdEquipo
                    WHERE d.IdPrestamo=p.IdPrestamo
                """);

            filtroEquipo(q, "e", lab, codigo, estado);

            q.sql.append(")");
        }

        q.sql.append(
                " ORDER BY p.FechaPrestamo DESC"
        );
    }

    private void historial(
            Consulta q,
            Integer lab,
            String codigo,
            String estado,
            LocalDate desde,
            LocalDate hasta) {

        q.sql.append("""
            SELECT h.Codigo,
                   h.Fecha,
                   h.Movimiento,
                   h.Detalle,
                   h.Resultado
            FROM (
                SELECT e.CodigoInventario AS Codigo,
                       e.IdLaboratorio,
                       e.Estado AS EstadoEquipo,
                       p.FechaPrestamo AS Fecha,
                       'PRESTAMO' AS Movimiento,
                       CONCAT('Prestamo No. ',p.IdPrestamo,
                              ' a ',s.Nombre,' ',s.Apellido)
                              AS Detalle,
                       CASE WHEN d.Estado='PENDIENTE'
                            THEN 'PENDIENTE'
                            ELSE 'DEVUELTO'
                       END AS Resultado
                FROM dbo.DetallePrestamo d
                JOIN dbo.Prestamo p
                  ON p.IdPrestamo=d.IdPrestamo
                JOIN dbo.Equipo e
                  ON e.IdEquipo=d.IdEquipo
                JOIN dbo.Solicitante s
                  ON s.IdSolicitante=p.IdSolicitante

                UNION ALL

                SELECT e.CodigoInventario,
                       e.IdLaboratorio,
                       e.Estado,
                       m.FechaIngreso,
                       'MANTENIMIENTO',
                       CONCAT(m.TipoMantenimiento,
                              ': ',m.Descripcion),
                       CASE WHEN m.Estado='EN_PROCESO'
                            THEN 'EN PROCESO'
                            ELSE m.Resultado
                       END
                FROM dbo.Mantenimiento m
                JOIN dbo.Equipo e
                  ON e.IdEquipo=m.IdEquipo
            ) h
            WHERE 1=1
            """);

        if (lab != null) {
            q.sql.append(" AND h.IdLaboratorio=?");
            q.parametros.add(lab);
        }

        if (codigo != null) {
            q.sql.append(" AND h.Codigo=?");
            q.parametros.add(codigo);
        }

        if (estado != null) {
            q.sql.append(" AND h.EstadoEquipo=?");
            q.parametros.add(estado);
        }

        filtroFecha(q, "h.Fecha", desde, hasta);

        q.sql.append(" ORDER BY h.Fecha DESC");
    }

    private void mantenimiento(
            Consulta q,
            Integer lab,
            String codigo,
            String estado,
            LocalDate desde,
            LocalDate hasta) {

        q.sql.append("""
            SELECT m.IdMantenimiento AS ID,
                   e.CodigoInventario AS Codigo,
                   e.Nombre AS Equipo,
                   l.Codigo AS Laboratorio,
                   e.Estado AS [Estado equipo],
                   m.TipoMantenimiento AS Tipo,
                   m.Descripcion,
                   m.Responsable,
                   m.FechaIngreso AS [Fecha ingreso],
                   m.FechaSalida AS [Fecha salida],
                   m.Resultado,
                   m.Estado AS [Estado mantenimiento]
            FROM dbo.Mantenimiento m
            JOIN dbo.Equipo e
              ON e.IdEquipo=m.IdEquipo
            JOIN dbo.Laboratorio l
              ON l.IdLaboratorio=e.IdLaboratorio
            WHERE 1=1
            """);

        filtroEquipo(q, "e", lab, codigo, estado);
        filtroFecha(q, "m.FechaIngreso", desde, hasta);

        q.sql.append(" ORDER BY m.FechaIngreso DESC");
    }

    private void devoluciones(
            Consulta q,
            Integer lab,
            String codigo,
            String estado,
            LocalDate desde,
            LocalDate hasta) {

        q.sql.append("""
            SELECT dv.FechaDevolucion AS [Fecha devolucion],
                   p.IdPrestamo AS [No. prestamo],
                   e.CodigoInventario AS Codigo,
                   e.Nombre AS Equipo,
                   l.Codigo AS Laboratorio,
                   e.Estado AS [Estado actual],
                   CONCAT(s.Nombre,' ',s.Apellido)
                     AS Solicitante,
                   dv.Condicion,
                   u.NombreUsuario AS [Recibido por],
                   dv.Observaciones
            FROM dbo.Devolucion dv
            JOIN dbo.DetallePrestamo d
              ON d.IdDetallePrestamo=dv.IdDetallePrestamo
            JOIN dbo.Prestamo p
              ON p.IdPrestamo=d.IdPrestamo
            JOIN dbo.Equipo e
              ON e.IdEquipo=d.IdEquipo
            JOIN dbo.Laboratorio l
              ON l.IdLaboratorio=e.IdLaboratorio
            JOIN dbo.Solicitante s
              ON s.IdSolicitante=p.IdSolicitante
            JOIN dbo.Usuario u
              ON u.IdUsuario=dv.IdUsuarioRecibe
            WHERE 1=1
            """);

        filtroEquipo(q, "e", lab, codigo, estado);

        filtroFecha(
                q,
                "dv.FechaDevolucion",
                desde,
                hasta
        );

        q.sql.append(" ORDER BY dv.FechaDevolucion DESC");
    }

    private void masPrestados(
            Consulta q,
            Integer lab,
            String codigo,
            String estado,
            LocalDate desde,
            LocalDate hasta) {

        q.sql.append("""
            SELECT e.CodigoInventario AS Codigo,
                   e.Nombre AS Equipo,
                   t.Nombre AS Tipo,
                   l.Codigo AS Laboratorio,
                   e.Estado,
            """);

        // Contamos solo préstamos dentro del rango,
        // si se indicó alguno.
        q.sql.append(
                " COUNT(d.IdDetallePrestamo) AS [Veces prestado] "
        );

        q.sql.append("""
            FROM dbo.Equipo e
            JOIN dbo.TipoEquipo t
              ON t.IdTipoEquipo=e.IdTipoEquipo
            JOIN dbo.Laboratorio l
              ON l.IdLaboratorio=e.IdLaboratorio
            LEFT JOIN dbo.DetallePrestamo d
              ON d.IdEquipo=e.IdEquipo
            """);

        if (desde != null || hasta != null) {
            // Para un rango, solo se cuentan los
            // préstamos realizados en esas fechas.
            q.sql.append("""
                AND EXISTS (
                    SELECT 1
                    FROM dbo.Prestamo p
                    WHERE p.IdPrestamo=d.IdPrestamo
                """);

            filtroFecha(q, "p.FechaPrestamo", desde, hasta);

            q.sql.append(")");
        }

        q.sql.append(" WHERE 1=1");

        filtroEquipo(q, "e", lab, codigo, estado);

        q.sql.append("""
            GROUP BY e.CodigoInventario,
                     e.Nombre,
                     t.Nombre,
                     l.Codigo,
                     e.Estado
            ORDER BY [Veces prestado] DESC,
                     e.CodigoInventario
            """);
    }

    // ====================================================
    // FILTROS COMPARTIDOS
    // ====================================================

    private void filtroEquipo(
            Consulta q,
            String alias,
            Integer lab,
            String codigo,
            String estado) {

        if (lab != null) {
            q.sql.append(" AND ")
                    .append(alias)
                    .append(".IdLaboratorio=?");

            q.parametros.add(lab);
        }

        if (codigo != null) {
            q.sql.append(" AND ")
                    .append(alias)
                    .append(".CodigoInventario=?");

            q.parametros.add(codigo);
        }

        if (estado != null) {
            q.sql.append(" AND ")
                    .append(alias)
                    .append(".Estado=?");

            q.parametros.add(estado);
        }
    }

    private void filtroFecha(
            Consulta q,
            String columna,
            LocalDate desde,
            LocalDate hasta) {

        if (desde != null) {
            q.sql.append(" AND ")
                    .append(columna)
                    .append(" >= ?");

            q.parametros.add(
                    Timestamp.valueOf(
                            desde.atStartOfDay()
                    )
            );
        }

        if (hasta != null) {
            q.sql.append(" AND ")
                    .append(columna)
                    .append(" < ?");

            q.parametros.add(
                    Timestamp.valueOf(
                            hasta.plusDays(1).atStartOfDay()
                    )
            );
        }
    }

    // ====================================================
    // EJECUTAR SELECT
    // ====================================================

    private ResultadoReporte ejecutar(Consulta q) {

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps =
                     cn.prepareStatement(q.sql.toString())) {

            for (int i = 0; i < q.parametros.size(); i++) {

                ps.setObject(
                        i + 1,
                        q.parametros.get(i)
                );
            }

            try (ResultSet rs = ps.executeQuery()) {

                ResultSetMetaData meta =
                        rs.getMetaData();

                List<String> columnas = new ArrayList<>();

                for (int i = 1;
                     i <= meta.getColumnCount();
                     i++) {

                    columnas.add(
                            meta.getColumnLabel(i)
                    );
                }

                ResultadoReporte resultado =
                        new ResultadoReporte(columnas);

                while (rs.next()) {

                    Object[] fila =
                            new Object[columnas.size()];

                    for (int i = 0;
                         i < fila.length;
                         i++) {

                        Object valor =
                                rs.getObject(i + 1);

                        if (valor instanceof Timestamp fecha) {

                            valor = fecha.toLocalDateTime()
                                    .toString()
                                    .replace('T', ' ');
                        }

                        fila[i] = valor;
                    }

                    resultado.agregarFila(fila);
                }

                return resultado;
            }

        } catch (SQLException e) {

            throw new DataAccessException(
                    "No se pudo generar el reporte filtrado.",
                    e
            );
        }
    }

    private static class Consulta {

        final StringBuilder sql =
                new StringBuilder();

        final List<Object> parametros =
                new ArrayList<>();
    }
}


