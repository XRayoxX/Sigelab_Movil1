package SIGELAB.util;

/** Configuración de notificaciones por variables de entorno del servidor. */
public final class Configuracion {
    private static volatile java.util.function.Function<String, String> properties = key -> null;
    private Configuracion() {}
    public static void configurar(java.util.function.Function<String, String> resolver) {
        properties = java.util.Objects.requireNonNull(resolver);
    }
    public static String valor(String clave) {
        String env = "SIGELAB_" + clave.toUpperCase(java.util.Locale.ROOT).replace('.', '_');
        String value = System.getenv(env);
        if (value == null || value.isBlank()) value = properties.apply(env);
        if (value == null || value.isBlank()) value = properties.apply("sigelab." + clave);
        return value == null || value.isBlank() ? null : value.trim();
    }
    public static String valor(String clave, String fallback) {
        String value = valor(clave);
        return value == null ? fallback : value;
    }
}
