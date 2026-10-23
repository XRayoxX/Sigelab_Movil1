package gt.edu.sigelab.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    record Session(int id, String name, String role, Instant expires) {}
    record LoginRequest(String username, String password) {}

    public ApiController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest data) {
        if (data == null || data.username() == null || data.password() == null || data.username().isBlank())
            return error(HttpStatus.BAD_REQUEST, "Ingrese usuario y contraseña.");
        // Compatibilidad temporal con el esquema actual; migrar SHA-256 a Argon2id/bcrypt.
        var users = jdbc.query("""
            SELECT TOP (1) u.IdUsuario, u.Nombre, u.Apellido, u.ContrasenaHash, u.Estado, r.Nombre AS Rol
            FROM dbo.Usuario u INNER JOIN dbo.Rol r ON r.IdRol = u.IdRol
            WHERE u.NombreUsuario = ?
            """, (rs, row) -> Map.<String, Object>of(
                "id", rs.getInt("IdUsuario"), "name", rs.getString("Nombre") + " " + rs.getString("Apellido"),
                "hash", rs.getString("ContrasenaHash"), "state", rs.getString("Estado"), "role", rs.getString("Rol")),
            data.username().trim());

        String attemptedHash = sha256(data.password());
        if (users.isEmpty() || !MessageDigest.isEqual(attemptedHash.getBytes(StandardCharsets.US_ASCII),
                ((String) users.get(0).get("hash")).toLowerCase().getBytes(StandardCharsets.US_ASCII)))
            return error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
        var u = users.get(0);
        if (!Objects.equals(u.get("state"), "ACTIVO"))
            return error(HttpStatus.FORBIDDEN, "Usuario inactivo.");

        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String name = (String) u.get("name");
        String role = (String) u.get("role");
        sessions.put(token, new Session((int) u.get("id"), name, role, Instant.now().plusSeconds(3600)));
        return ResponseEntity.ok(Map.of("token", token, "name", name, "role", role));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(HttpServletRequest request) {
        if (authorize(request) == null) return error(HttpStatus.UNAUTHORIZED, "Sesión no válida.");
        var list = jdbc.queryForList("""
            SELECT TotalEquipos, Disponibles, Prestados, Danados, EnMantenimiento,
                   DeBaja, PrestamosActivos, PrestamosVencidos FROM dbo.vw_Dashboard
            """);
        if (list.isEmpty()) return error(HttpStatus.SERVICE_UNAVAILABLE, "Resumen no disponible.");
        var row = list.get(0);
        return ResponseEntity.ok(Map.of(
            "totalEquipos", row.get("TotalEquipos"), "disponibles", row.get("Disponibles"),
            "prestados", row.get("Prestados"), "danados", row.get("Danados"),
            "enMantenimiento", row.get("EnMantenimiento"), "deBaja", row.get("DeBaja"),
            "prestamosActivos", row.get("PrestamosActivos"), "prestamosVencidos", row.get("PrestamosVencidos")));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = bearer(request);
        if (token != null) sessions.remove(token);
        return ResponseEntity.noContent().build();
    }

    private Session authorize(HttpServletRequest request) {
        String token = bearer(request);
        if (token == null) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (Instant.now().isAfter(session.expires())) { sessions.remove(token); return null; }
        return session;
    }

    private String bearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    private String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String text) {
        return ResponseEntity.status(status).body(Map.of("error", text));
    }
}
