package gt.edu.sigelab.api;

import SIGELAB.enums.*;
import SIGELAB.model.Usuario;
import SIGELAB.repository.UsuarioRepository;
import SIGELAB.service.AuthService;
import SIGELAB.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ApiController {
    record Session(String username, Instant expires) {}
    record LoginRequest(String username, String password) {}
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Session> pending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private record Attempts(int count, Instant expires) {}
    private final SecureRandom random = new SecureRandom();
    private final AuthService auth = new AuthService();
    private final UsuarioRepository users = new UsuarioRepository();

    public ApiController(DataSource source) { ConexionBD.configurar(source); }
    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "UP"); }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody LoginRequest data, HttpServletRequest request) {
        cleanup();
        if (data == null || data.username() == null || data.password() == null || data.username().length() > 30 || data.password().length() > 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingrese usuario y contraseña válidos.");
        String key = request.getRemoteAddr() + ":" + data.username().trim().toLowerCase(Locale.ROOT);
        Attempts tries = attempts.compute(key, (k, old) -> old == null || old.expires().isBefore(Instant.now())
                ? new Attempts(1, Instant.now().plusSeconds(300)) : new Attempts(old.count() + 1, old.expires()));
        if (tries.count() > 10) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Espere cinco minutos antes de intentar nuevamente.");
        Usuario user;
        try { user = auth.validarCredenciales(data.username(), data.password()); }
        catch (BusinessException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage()); }
        String token = token();
        pending.put(token, new Session(user.getNombreUsuario(), Instant.now().plusSeconds(180)));
        return Map.of("pendingToken", token, "userId", user.getIdUsuario(), "requiresFace", true);
    }

    Usuario pendingUser(HttpServletRequest request, boolean consume) {
        String token = bearer(request);
        Session session = token == null ? null : (consume ? pending.remove(token) : pending.get(token));
        return resolve(session);
    }
    public Usuario authorize(HttpServletRequest request) {
        String token = bearer(request);
        return resolve(token == null ? null : sessions.get(token));
    }
    private Usuario resolve(Session session) {
        if (session == null || !session.expires().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión expiró. Inicie sesión nuevamente.");
        Usuario user = users.buscarPorNombreUsuario(session.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no disponible."));
        if (user.getEstado() != EstadoRegistro.ACTIVO) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta inactiva.");
        return user;
    }
    Map<String, Object> complete(Usuario user) {
        try {
            auth.completarLogin(user, SIGELAB.facial.VerificacionFacial.Resultado.APROBADO);
            Usuario current = Sesion.getUsuarioActual();
            String token = token();
            sessions.put(token, new Session(user.getNombreUsuario(), Instant.now().plusSeconds(3600)));
            new SIGELAB.service.CorreoService().notificarInicioSesion(current);
            return Map.of("token", token, "name", current.getNombreCompleto(), "role", current.getRol().name(), "matched", true);
        } finally { Sesion.cerrar(); }
    }
    @PostMapping("/auth/logout") public Map<String, Boolean> logout(HttpServletRequest request) {
        String token = bearer(request);
        if (token != null) { sessions.remove(token); pending.remove(token); }
        return Map.of("ok", true);
    }
    private String token() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    static String bearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
    private void cleanup() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(e -> !e.getValue().expires().isAfter(now));
        pending.entrySet().removeIf(e -> !e.getValue().expires().isAfter(now));
        attempts.entrySet().removeIf(e -> !e.getValue().expires().isAfter(now));
        if (pending.size() + sessions.size() + attempts.size() > 20000)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio ocupado. Intente más tarde.");
    }
}
