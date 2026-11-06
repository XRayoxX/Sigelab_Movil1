package gt.edu.sigelab.api;

import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;
import SIGELAB.service.AuthService;
import SIGELAB.service.CorreoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/face")
public class FacialController {
    private final ApiController auth;
    private final String url, key;
    private final RestTemplate http;

    public FacialController(ApiController auth, @Value("${SIGELAB_FACE_URL:${sigelab.facial.url:}}") String url, @Value("${SIGELAB_FACE_KEY:${sigelab.facial.key:}}") String key) {
        this.auth = auth; this.url = url.replaceAll("/+$", ""); this.key = key;
        var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(20000); factory.setReadTimeout(90000);
        http = new RestTemplate(factory);
    }
    private HttpHeaders headers() {
        if (!url.startsWith("https://") || key.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configure SIGELAB_FACE_URL y SIGELAB_FACE_KEY en el servidor.");
        HttpHeaders h = new HttpHeaders(); h.set("X-API-Key", key); h.set("X-Client", "sigelab-mobile-api"); return h;
    }
    private Map<String, Object> call(String path, HttpMethod method, Object body) {
        try {
            HttpHeaders h = headers();
            if (body != null) h.setContentType(MediaType.MULTIPART_FORM_DATA);
            @SuppressWarnings("unchecked") Map<String, Object> result = http.exchange(url + path, method, new HttpEntity<>(body, h), Map.class).getBody();
            if (result == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta facial vacía.");
            return result;
        } catch (HttpStatusCodeException ex) {
            String message = "El servicio facial rechazó la solicitud.";
            try {
                String detail = new com.fasterxml.jackson.databind.ObjectMapper().readTree(ex.getResponseBodyAsString()).path("detail").asText("");
                if (!detail.isBlank()) message = detail.substring(0, Math.min(detail.length(), 500));
            } catch (IOException ignored) { }
            throw new ResponseStatusException(ex.getStatusCode().is4xxClientError() ? ex.getStatusCode() : HttpStatus.BAD_GATEWAY, message);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No fue posible completar la operación facial. Revise el registro del rostro e intente nuevamente.");
        }
    }
    @GetMapping("/health") public Object health() {
        return call("/health", HttpMethod.GET, null);
    }
    @GetMapping("/challenge") public Object challenge(HttpServletRequest request, @RequestParam(required=false) Integer userId) {
        if (userId == null) {
            operator(request);
            return call("/api/face/challenge", HttpMethod.GET, null);
        }
        Usuario user = auth.pendingUser(request, false);
        if (userId != user.getIdUsuario()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El desafío debe corresponder al usuario que inició sesión.");
        Map<String, Object> profile = call("/api/face/status/" + user.getIdUsuario(), HttpMethod.GET, null);
        if (!Boolean.TRUE.equals(profile.get("enrolled")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Su cuenta no tiene rostro registrado. Solicite al administrador el registro facial.");
        return call("/api/face/challenge?userId=" + user.getIdUsuario(), HttpMethod.GET, null);
    }
    @PostMapping(value="/verify", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object verify(HttpServletRequest request, @RequestParam String challengeToken,
            @RequestParam(defaultValue="false") boolean mirrored, @RequestParam List<MultipartFile> images) throws IOException {
        Usuario user = auth.pendingUser(request, true);
        var body = images(images, mirrored); body.add("userId", String.valueOf(user.getIdUsuario())); body.add("challengeToken", challengeToken);
        Map<String, Object> result;
        try { result = call("/api/face/verify", HttpMethod.POST, body); }
        catch (ResponseStatusException ex) {
            int status = ex.getStatusCode().value();
            if (status == 400 || status == 409 || status == 422) registrarRechazo(user);
            throw ex;
        }
        if (!Boolean.TRUE.equals(result.get("matched"))) {
            registrarRechazo(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El rostro no coincide o no pasó la prueba de vida.");
        }
        return auth.complete(user);
    }
    private void registrarRechazo(Usuario user) {
        new AuthService().registrarFalloFacial(user, "Verificación facial móvil rechazada");
        new CorreoService().notificarFalloFacial(user);
    }
    @PostMapping(value="/enroll", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object enroll(HttpServletRequest request, @RequestParam int userId, @RequestParam(defaultValue="false") boolean replace,
            @RequestParam(defaultValue="false") boolean mirrored, @RequestParam List<MultipartFile> images) throws IOException {
        Usuario admin = auth.authorize(request);
        if (admin.getRol() != RolUsuario.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede registrar rostros.");
        var body = images(images, mirrored); body.add("userId", String.valueOf(userId)); body.add("replace", String.valueOf(replace));
        return call("/api/face/enroll", HttpMethod.POST, body);
    }
    @GetMapping("/status/{id}") public Object status(HttpServletRequest request, @PathVariable int id) {
        Usuario user = auth.authorize(request);
        if (user.getRol() != RolUsuario.ADMIN && user.getIdUsuario() != id) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return call("/api/face/status/" + id, HttpMethod.GET, null);
    }
    @DeleteMapping("/profile/{id}") public Object delete(HttpServletRequest request, @PathVariable int id) {
        Usuario admin = auth.authorize(request);
        if (admin.getRol() != RolUsuario.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede eliminar rostros.");
        return call("/api/face/profile/" + id + "?adminUserId=" + admin.getIdUsuario(), HttpMethod.DELETE, null);
    }
    @PostMapping(value="/identify", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object identify(HttpServletRequest request, @RequestParam String challengeToken,
            @RequestParam(defaultValue="false") boolean mirrored, @RequestParam List<MultipartFile> images) throws IOException {
        operator(request);
        var body = images(images, mirrored); body.add("challengeToken", challengeToken);
        return call("/api/face/identify", HttpMethod.POST, body);
    }
    @PostMapping(value="/analyze", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object analyze(HttpServletRequest request, @RequestParam(defaultValue="false") boolean mirrored,
            @RequestParam List<MultipartFile> images) throws IOException {
        operator(request);
        return call("/api/face/analyze", HttpMethod.POST, images(images, mirrored));
    }
    private void operator(HttpServletRequest request) {
        Usuario user = auth.authorize(request);
        if (user.getRol() != RolUsuario.ADMIN && user.getRol() != RolUsuario.ENCARGADO)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta operación requiere un administrador o encargado.");
    }
    private MultiValueMap<String, Object> images(List<MultipartFile> images, boolean mirrored) throws IOException {
        if (images.isEmpty() || images.size() > 12) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envíe entre 1 y 12 fotografías.");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(); body.add("mirrored", String.valueOf(mirrored));
        for (MultipartFile file : images) {
            if (file.isEmpty() || file.getSize() > 2_000_000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fotografía inválida o demasiado grande.");
            var resource = new ByteArrayResource(file.getBytes()) { @Override public String getFilename() { return "capture.jpg"; } };
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.IMAGE_JPEG);
            body.add("images", new HttpEntity<>(resource, headers));
        }
        return body;
    }
}
