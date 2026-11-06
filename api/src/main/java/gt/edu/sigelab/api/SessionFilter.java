package gt.edu.sigelab.api;
import SIGELAB.util.Sesion;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;

@Component
public class SessionFilter extends OncePerRequestFilter {
    private final ApiController auth;
    public SessionFilter(ApiController auth) { this.auth = auth; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/") || path.equals("/api/health") || path.startsWith("/api/auth/") || path.startsWith("/api/face/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            try { Sesion.iniciar(auth.authorize(request)); }
            catch (ResponseStatusException ex) {
                response.setStatus(ex.getStatusCode().value()); response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"La sesión expiró. Inicie sesión nuevamente.\"}"); return;
            }
            catch (SIGELAB.util.DataAccessException ex) {
                response.setStatus(503); response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"La base de datos no está disponible. Intente nuevamente.\"}"); return;
            }
            chain.doFilter(request, response);
        } finally { Sesion.cerrar(); }
    }
}
