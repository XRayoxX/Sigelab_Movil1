-- Solo consulta. Ejecutar en la base compartida correcta.
SELECT DB_NAME() AS BaseActual;

WITH Requeridos AS (
    SELECT nombre FROM (VALUES
      ('Rol'), ('Usuario'), ('Laboratorio'), ('TipoEquipo'), ('Equipo'),
      ('Solicitante'), ('Prestamo'), ('DetallePrestamo'), ('Devolucion'),
      ('Mantenimiento'), ('Auditoria'), ('vw_PrestamoResumen'), ('vw_Dashboard')
    ) AS nombres(nombre)
)
SELECT nombre, CASE WHEN OBJECT_ID('dbo.' + nombre) IS NULL THEN 'FALTA' ELSE 'OK' END AS Estado
FROM Requeridos;

SELECT nombre, CASE WHEN COL_LENGTH('dbo.Usuario', nombre) IS NULL THEN 'FALTA' ELSE 'OK' END AS Estado
FROM (VALUES ('SegundoNombre'), ('SegundoApellido'), ('NombreUsuario'), ('ContrasenaHash')) AS columnas(nombre);

SELECT nombre, CASE WHEN OBJECT_ID('dbo.' + nombre) IS NULL THEN 'FALTA' ELSE 'OK' END AS Estado
FROM (VALUES ('GenerarNombreUsuario'), ('TR_Usuario_GenerarNombreUsuario'), ('fn_AhoraGuatemala')) AS objetos(nombre);

SELECT OBJECT_NAME(parent_object_id) AS Tabla, name AS Restriccion, definition AS ValorPredeterminado
FROM sys.default_constraints
WHERE parent_object_id IN (OBJECT_ID('dbo.Prestamo'), OBJECT_ID('dbo.Devolucion'),
    OBJECT_ID('dbo.Mantenimiento'), OBJECT_ID('dbo.Auditoria'));

SELECT 'Usuario' AS Objeto, HAS_PERMS_BY_NAME('dbo.Usuario', 'OBJECT', 'SELECT') AS PuedeConsultar,
       HAS_PERMS_BY_NAME('dbo.Usuario', 'OBJECT', 'INSERT') AS PuedeInsertar,
       HAS_PERMS_BY_NAME('dbo.Usuario', 'OBJECT', 'UPDATE') AS PuedeActualizar;
