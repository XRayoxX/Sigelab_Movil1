/* ==========================================================================
   SIGELAB - Script 05: Fechas y horas en hora de Guatemala
   Para una base que YA existe (por ejemplo la de Azure). Una base nueva creada
   con el script 01 ya trae estos cambios.
   --------------------------------------------------------------------------
   Problema: Azure SQL Database trabaja siempre en UTC, 6 horas adelante de
   Guatemala. SYSDATETIME() devolvía esa hora en Auditoría, Préstamo,
   Devolución, Mantenimiento, etc., y la vista de vencidos comparaba contra UTC.
   Solución: la función dbo.fn_AhoraGuatemala() y todos los valores por
   defecto y vistas la usan. Se puede ejecutar varias veces sin problema.
   Los registros que ya existían NO se modifican aquí (ver script 06).
   ========================================================================== */

-- En Azure, conéctese directamente a la base (sigelabadministrador) y omita el USE.
-- USE Sigelab;
-- GO

CREATE OR ALTER FUNCTION dbo.fn_AhoraGuatemala()
RETURNS DATETIME2(0)
AS
BEGIN
    RETURN CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0));
END;
GO

-- Cualquier usuario que inserte filas necesita poder ejecutarla (la usan los valores por defecto)
GRANT EXECUTE ON dbo.fn_AhoraGuatemala TO PUBLIC;
GO

/* ---------- Valores por defecto de las fechas automáticas ---------- */
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Usuario_FechaCreacion')
    ALTER TABLE dbo.Usuario DROP CONSTRAINT DF_Usuario_FechaCreacion;
ALTER TABLE dbo.Usuario ADD CONSTRAINT DF_Usuario_FechaCreacion DEFAULT dbo.fn_AhoraGuatemala() FOR FechaCreacion;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Equipo_FechaRegistro')
    ALTER TABLE dbo.Equipo DROP CONSTRAINT DF_Equipo_FechaRegistro;
ALTER TABLE dbo.Equipo ADD CONSTRAINT DF_Equipo_FechaRegistro DEFAULT dbo.fn_AhoraGuatemala() FOR FechaRegistro;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Prestamo_Fecha')
    ALTER TABLE dbo.Prestamo DROP CONSTRAINT DF_Prestamo_Fecha;
ALTER TABLE dbo.Prestamo ADD CONSTRAINT DF_Prestamo_Fecha DEFAULT dbo.fn_AhoraGuatemala() FOR FechaPrestamo;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Devolucion_Fecha')
    ALTER TABLE dbo.Devolucion DROP CONSTRAINT DF_Devolucion_Fecha;
ALTER TABLE dbo.Devolucion ADD CONSTRAINT DF_Devolucion_Fecha DEFAULT dbo.fn_AhoraGuatemala() FOR FechaDevolucion;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Mantenimiento_Ingreso')
    ALTER TABLE dbo.Mantenimiento DROP CONSTRAINT DF_Mantenimiento_Ingreso;
ALTER TABLE dbo.Mantenimiento ADD CONSTRAINT DF_Mantenimiento_Ingreso DEFAULT dbo.fn_AhoraGuatemala() FOR FechaIngreso;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Auditoria_FechaHora')
    ALTER TABLE dbo.Auditoria DROP CONSTRAINT DF_Auditoria_FechaHora;
ALTER TABLE dbo.Auditoria ADD CONSTRAINT DF_Auditoria_FechaHora DEFAULT dbo.fn_AhoraGuatemala() FOR FechaHora;
GO

/* ---------- Vistas que calculan préstamos vencidos (RN-17) ---------- */
CREATE OR ALTER VIEW dbo.vw_PrestamoResumen AS
SELECT  p.IdPrestamo,
        p.FechaPrestamo,
        p.FechaEsperadaDevolucion,
        p.FechaFinalizacion,
        p.Estado,
        s.Carne,
        s.Nombre + ' ' + s.Apellido                                   AS Solicitante,
        u.NombreUsuario                                               AS RegistradoPor,
        COUNT(d.IdDetallePrestamo)                                    AS TotalEquipos,
        SUM(CASE WHEN d.Estado = 'PENDIENTE' THEN 1 ELSE 0 END)       AS EquiposPendientes,
        CAST(CASE WHEN p.Estado = 'ACTIVO'
                   AND p.FechaEsperadaDevolucion < dbo.fn_AhoraGuatemala()
                   AND SUM(CASE WHEN d.Estado = 'PENDIENTE' THEN 1 ELSE 0 END) > 0
                  THEN 1 ELSE 0 END AS BIT)                           AS EsVencido
FROM    dbo.Prestamo        p
JOIN    dbo.Solicitante     s ON s.IdSolicitante = p.IdSolicitante
JOIN    dbo.Usuario         u ON u.IdUsuario     = p.IdUsuarioRegistra
JOIN    dbo.DetallePrestamo d ON d.IdPrestamo    = p.IdPrestamo
GROUP BY p.IdPrestamo, p.FechaPrestamo, p.FechaEsperadaDevolucion, p.FechaFinalizacion,
         p.Estado, s.Carne, s.Nombre, s.Apellido, u.NombreUsuario;
GO

CREATE OR ALTER VIEW dbo.vw_Dashboard AS
SELECT
    (SELECT COUNT(*) FROM dbo.Equipo)                                   AS TotalEquipos,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'DISPONIBLE')       AS Disponibles,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'PRESTADO')         AS Prestados,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'DANADO')           AS Danados,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'MANTENIMIENTO')    AS EnMantenimiento,
    (SELECT COUNT(*) FROM dbo.Equipo WHERE Estado = 'BAJA')             AS DeBaja,
    (SELECT COUNT(*) FROM dbo.Prestamo WHERE Estado = 'ACTIVO')         AS PrestamosActivos,
    (SELECT COUNT(*)
       FROM dbo.Prestamo p
      WHERE p.Estado = 'ACTIVO'
        AND p.FechaEsperadaDevolucion < dbo.fn_AhoraGuatemala()
        AND EXISTS (SELECT 1 FROM dbo.DetallePrestamo d
                     WHERE d.IdPrestamo = p.IdPrestamo AND d.Estado = 'PENDIENTE')) AS PrestamosVencidos;
GO

SELECT dbo.fn_AhoraGuatemala() AS HoraGuatemala, SYSDATETIME() AS HoraServidor;
PRINT 'Fechas automáticas configuradas en hora de Guatemala.';
GO
