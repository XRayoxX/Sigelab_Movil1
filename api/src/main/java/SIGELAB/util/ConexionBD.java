package SIGELAB.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Los repositorios usan el pool configurado por Spring, nunca credenciales del APK. */
public final class ConexionBD {
    private static volatile DataSource source;
    private ConexionBD() {}
    public static void configurar(DataSource dataSource) { source = dataSource; }
    public static Connection obtenerConexion() {
        if (source == null) throw new DataAccessException("La conexión no está configurada.");
        try { return source.getConnection(); }
        catch (SQLException ex) { throw new DataAccessException("No fue posible conectar con la base de datos.", ex); }
    }
}
