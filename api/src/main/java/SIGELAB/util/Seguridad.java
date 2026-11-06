package SIGELAB.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Las contraseñas nunca se guardan en texto plano: en la base se guarda su hash SHA-256
 * (64 caracteres hexadecimales). Para validar un login se calcula el hash de lo que
 * escribió el usuario y se compara con el guardado.
 */
public final class Seguridad {

    private Seguridad() {
    }

    public static String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));   // cada byte -> 2 caracteres hexadecimales en minúscula
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", e);
        }
    }
}
