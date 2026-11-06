package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Auditoria;
import SIGELAB.model.Usuario;
import SIGELAB.repository.AuditoriaRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Registra y consulta la auditoría. Los demás Service la usan para dejar rastro (RN-18).
 */
public class AuditoriaService {

    private final AuditoriaRepository repository = new AuditoriaRepository();

    // Registra con el usuario que tiene la sesión abierta, dentro de la transacción recibida
    public void registrar(Connection cn, AccionAuditoria accion, String entidad, Integer idRegistro, String descripcion) {
        Usuario actual = Sesion.getUsuarioActual();
        repository.registrar(cn, actual.getIdUsuario(), actual.getNombreUsuario(), accion, entidad, idRegistro, descripcion);
    }

    // Igual que el anterior, pero con su propia conexión (operaciones de una sola tabla)
    public void registrar(AccionAuditoria accion, String entidad, Integer idRegistro, String descripcion) {
        Usuario actual = Sesion.getUsuarioActual();
        repository.registrar(actual.getIdUsuario(), actual.getNombreUsuario(), accion, entidad, idRegistro, descripcion);
    }

    public List<Auditoria> consultar(String nombreUsuario, String entidad, String accion, LocalDate desde, LocalDate hasta) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha hasta no puede ser anterior a la fecha desde.");
        }
        return repository.consultar(nombreUsuario, entidad, accion, desde, hasta);
    }
}
