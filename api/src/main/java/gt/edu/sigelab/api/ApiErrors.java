package gt.edu.sigelab.api;
import SIGELAB.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(BusinessException.class) ResponseEntity<?> business(BusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason() == null ? "Solicitud rechazada." : ex.getReason()));
    }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class}) ResponseEntity<?> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Revise los campos y las fechas de la solicitud."));
    }
    @ExceptionHandler({DataAccessException.class, org.springframework.dao.DataAccessException.class}) ResponseEntity<?> database(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("Operación SQL fallida", ex);
        return ResponseEntity.status(409).body(Map.of("error", "No se pudo guardar o consultar. Verifique la conexión, los datos únicos y el estado actual; actualice antes de reintentar."));
    }
}
