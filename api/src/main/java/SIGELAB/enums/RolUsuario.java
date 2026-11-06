package SIGELAB.enums;

// Roles del sistema. Los nombres coinciden con la columna Rol.Nombre de la base de datos.
public enum RolUsuario {
    ADMIN,       // acceso completo
    ENCARGADO,   // operación cotidiana: equipos, préstamos, devoluciones, mantenimiento y consultas
    DOCENTE      // solo consultas
}
