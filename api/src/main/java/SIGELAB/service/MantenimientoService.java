package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoMantenimiento;
import SIGELAB.enums.ResultadoMantenimiento;
import SIGELAB.enums.RolUsuario;
import SIGELAB.enums.TipoMantenimiento;
import SIGELAB.model.Equipo;
import SIGELAB.model.Mantenimiento;
import SIGELAB.repository.EquipoRepository;
import SIGELAB.repository.MantenimientoRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;
import SIGELAB.util.Transaccion;

import java.util.List;

/**
 * Reglas de mantenimiento (sección 6.9):
 *   DANADO     -> MANTENIMIENTO   (correctivo o preventivo)
 *   DISPONIBLE -> MANTENIMIENTO   (solo preventivo)
 *   MANTENIMIENTO -> DISPONIBLE   (REPARADO)       RN-15
 *   MANTENIMIENTO -> BAJA         (NO_REPARABLE)   RN-15
 */
public class MantenimientoService {

    private final MantenimientoRepository repository = new MantenimientoRepository();
    private final EquipoRepository equipoRepository = new EquipoRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    // Equipos que pueden ingresar a mantenimiento
    public List<Equipo> equiposParaMantenimiento() {
        return equipoRepository.listarPorEstados(EstadoEquipo.DANADO, EstadoEquipo.DISPONIBLE);
    }

    public List<Mantenimiento> listar(String texto, EstadoMantenimiento estado) {
        return repository.listar(texto == null || texto.isBlank() ? null : texto.trim(), estado);
    }

    public int enviarAMantenimiento(Mantenimiento m) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        validarIngreso(m);
        int idUsuario = Sesion.getIdUsuario();

        return Transaccion.ejecutar(cn -> {
            Equipo equipo = equipoRepository.buscarPorId(cn, m.getEquipo().getIdEquipo())
                    .orElseThrow(() -> new BusinessException("El equipo seleccionado ya no existe."));
            EstadoEquipo estadoActual = equipo.getEstado();
            if (estadoActual == EstadoEquipo.DISPONIBLE && m.getTipo() != TipoMantenimiento.PREVENTIVO) {
                throw new BusinessException("Un equipo DISPONIBLE solo puede ingresar a mantenimiento PREVENTIVO.");
            }
            if (estadoActual != EstadoEquipo.DANADO && estadoActual != EstadoEquipo.DISPONIBLE) {
                throw new BusinessException("El equipo " + equipo.getCodigoInventario() + " está " + estadoActual
                        + ". Solo pueden enviarse a mantenimiento equipos DANADO o DISPONIBLE.");
            }

            int idMantenimiento = repository.insertar(cn, m, idUsuario);
            if (!equipoRepository.cambiarEstado(cn, equipo.getIdEquipo(), estadoActual, EstadoEquipo.MANTENIMIENTO)) {
                throw new BusinessException("El equipo " + equipo.getCodigoInventario() + " acaba de cambiar de estado. Intente de nuevo.");
            }
            auditoria.registrar(cn, AccionAuditoria.EQUIPO_ENVIADO_MANTENIMIENTO, "Mantenimiento", idMantenimiento,
                    "Equipo " + equipo.getCodigoInventario() + ": " + estadoActual + " -> MANTENIMIENTO (" + m.getTipo() + ")");
            return idMantenimiento;
        });
    }

    // Devuelve el estado en que quedó el equipo (DISPONIBLE o BAJA)
    public EstadoEquipo finalizar(int idMantenimiento, ResultadoMantenimiento resultado, String observaciones) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (idMantenimiento <= 0) {
            throw new BusinessException("Seleccione en la tabla el mantenimiento que desea finalizar.");
        }
        if (resultado == null) {
            throw new BusinessException("Seleccione el resultado del mantenimiento (REPARADO o NO_REPARABLE).");
        }
        String obs = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        if (obs != null && obs.length() > 500) {
            throw new BusinessException("Las observaciones no pueden tener más de 500 caracteres.");
        }

        return Transaccion.ejecutar(cn -> {
            Mantenimiento m = repository.buscarPorId(cn, idMantenimiento)
                    .orElseThrow(() -> new BusinessException("El mantenimiento seleccionado no existe."));
            if (m.getEstado() != EstadoMantenimiento.EN_PROCESO || !repository.finalizar(cn, idMantenimiento, resultado, obs)) {
                throw new BusinessException("Este mantenimiento ya fue finalizado.");
            }
            // RN-15: al cerrar, el equipo queda DISPONIBLE o BAJA según el resultado
            EstadoEquipo nuevoEstado = resultado == ResultadoMantenimiento.REPARADO ? EstadoEquipo.DISPONIBLE : EstadoEquipo.BAJA;
            if (!equipoRepository.cambiarEstado(cn, m.getEquipo().getIdEquipo(), EstadoEquipo.MANTENIMIENTO, nuevoEstado)) {
                throw new BusinessException("El equipo " + m.getEquipo().getCodigoInventario() + " no está en MANTENIMIENTO.");
            }
            auditoria.registrar(cn, AccionAuditoria.MANTENIMIENTO_FINALIZADO, "Mantenimiento", idMantenimiento,
                    "Equipo " + m.getEquipo().getCodigoInventario() + " " + resultado + " -> " + nuevoEstado);
            return nuevoEstado;
        });
    }

    private void validarIngreso(Mantenimiento m) {
        if (m.getEquipo() == null) {
            throw new BusinessException("Seleccione el equipo.");
        }
        if (m.getTipo() == null) {
            throw new BusinessException("Seleccione el tipo de mantenimiento.");
        }
        m.setResponsable(m.getResponsable() == null ? null : m.getResponsable().trim());
        m.setDescripcion(m.getDescripcion() == null ? null : m.getDescripcion().trim());
        if (m.getResponsable() == null || m.getResponsable().isEmpty()) {
            throw new BusinessException("Indique el responsable (técnico) del mantenimiento.");
        }
        if (m.getResponsable().length() > 100) {
            throw new BusinessException("El responsable no puede tener más de 100 caracteres.");
        }
        if (m.getDescripcion() == null || m.getDescripcion().isEmpty()) {
            throw new BusinessException("Describa el trabajo o el problema del equipo.");
        }
        if (m.getDescripcion().length() > 500) {
            throw new BusinessException("La descripción no puede tener más de 500 caracteres.");
        }
    }
}
