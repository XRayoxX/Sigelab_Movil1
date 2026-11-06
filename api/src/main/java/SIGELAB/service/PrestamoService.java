package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.Equipo;
import SIGELAB.model.Prestamo;
import SIGELAB.model.Solicitante;
import SIGELAB.repository.EquipoRepository;
import SIGELAB.repository.PrestamoRepository;
import SIGELAB.repository.SolicitanteRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;
import SIGELAB.util.Transaccion;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reglas del préstamo (sección 6.7 y RN-05 a RN-09).
 * Aquí se valida que un equipo prestado no pueda prestarse otra vez: NO en la interfaz.
 */
public class PrestamoService {

    private final PrestamoRepository repository = new PrestamoRepository();
    private final EquipoRepository equipoRepository = new EquipoRepository();
    private final SolicitanteRepository solicitanteRepository = new SolicitanteRepository();
    private final EquipoService equipoService = new EquipoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final CorreoService correoService = new CorreoService();

    /**
     * Valida si un equipo puede agregarse a la lista del préstamo que se está armando.
     * RN-07 / RN-08: solo DISPONIBLE. RN-06: no puede repetirse en el mismo préstamo.
     */
    public Equipo validarEquipoParaPrestamo(String codigoInventario, List<Equipo> equiposAgregados) {
        Equipo equipo = equipoService.identificarPorCodigo(codigoInventario);
        validarDisponible(equipo);
        for (Equipo agregado : equiposAgregados) {
            if (agregado.getIdEquipo() == equipo.getIdEquipo()) {
                throw new BusinessException("El equipo " + equipo.getCodigoInventario() + " ya está en este préstamo.");
            }
        }
        return equipo;
    }

    /**
     * Confirma el préstamo como UNA transacción:
     * 1) encabezado, 2) detalle por equipo, 3) cada equipo pasa a PRESTADO (RN-09), 4) auditoría.
     * Si algo falla, se hace ROLLBACK y no queda nada a medias.
     */
    public int confirmar(Prestamo prestamo, List<Equipo> equipos) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        validarEncabezado(prestamo);

        // RN-05: al menos un equipo
        if (equipos == null || equipos.isEmpty()) {
            throw new BusinessException("Agregue al menos un equipo al préstamo.");
        }
        // RN-06: el mismo equipo no puede aparecer dos veces
        Set<Integer> ids = new HashSet<>();
        for (Equipo e : equipos) {
            if (!ids.add(e.getIdEquipo())) {
                throw new BusinessException("El equipo " + e.getCodigoInventario() + " aparece dos veces en el préstamo.");
            }
        }

        int idUsuario = Sesion.getIdUsuario();
        List<Equipo> equiposConfirmados = new ArrayList<>();
        int numeroPrestamo = Transaccion.ejecutar(cn -> {
            int idPrestamo = repository.insertarEncabezado(cn, prestamo, idUsuario);
            StringBuilder codigos = new StringBuilder();
            for (Equipo e : equipos) {
                // Se vuelve a leer el estado DENTRO de la transacción: pudo cambiar desde que se agregó a la lista
                Equipo actual = equipoRepository.buscarPorId(cn, e.getIdEquipo())
                        .orElseThrow(() -> new BusinessException("El equipo " + e.getCodigoInventario() + " ya no existe."));
                validarDisponible(actual);
                equiposConfirmados.add(actual);
                repository.insertarDetalle(cn, idPrestamo, actual.getIdEquipo());
                // RN-09: DISPONIBLE -> PRESTADO (solo si sigue DISPONIBLE en este instante)
                if (!equipoRepository.cambiarEstado(cn, actual.getIdEquipo(), EstadoEquipo.DISPONIBLE, EstadoEquipo.PRESTADO)) {
                    throw new BusinessException("El equipo " + actual.getCodigoInventario() + " acaba de cambiar de estado. Intente de nuevo.");
                }
                codigos.append(codigos.length() == 0 ? "" : ", ").append(actual.getCodigoInventario());
            }
            auditoria.registrar(cn, AccionAuditoria.PRESTAMO_CREADO, "Prestamo", idPrestamo,
                    "Préstamo a " + prestamo.getSolicitante().getCarne() + " con " + equipos.size() + " equipo(s): " + codigos);
            return idPrestamo;
        });

        // Solo después del COMMIT: se avisa al solicitante qué equipos se llevó
        correoService.notificarPrestamo(numeroPrestamo, prestamo, equiposConfirmados);
        return numeroPrestamo;
    }

    public List<Prestamo> listar(String texto, String filtro) {
        String busqueda = texto == null || texto.isBlank() ? null : texto.trim();
        String estado = filtro == null || "TODOS".equals(filtro) ? null : filtro;
        return repository.listar(busqueda, estado);
    }

    public List<DetallePrestamo> listarDetalles(int idPrestamo) {
        return repository.listarDetalles(idPrestamo);
    }

    // RN-07 / RN-08 / RN-14: PRESTADO, DANADO, MANTENIMIENTO y BAJA bloquean nuevos préstamos
    private void validarDisponible(Equipo equipo) {
        if (equipo.getEstado() != EstadoEquipo.DISPONIBLE) {
            throw new BusinessException("El equipo " + equipo.getCodigoInventario() + " está " + equipo.getEstado()
                    + " y no puede prestarse. Solo se prestan equipos DISPONIBLES.");
        }
    }

    private void validarEncabezado(Prestamo prestamo) {
        if (prestamo.getSolicitante() == null) {
            throw new BusinessException("Seleccione el solicitante.");
        }
        // Se consulta la base: el solicitante debe existir y estar ACTIVO
        Solicitante s = solicitanteRepository.buscarPorId(prestamo.getSolicitante().getIdSolicitante())
                .orElseThrow(() -> new BusinessException("El solicitante seleccionado no existe."));
        if (s.getEstado() != EstadoRegistro.ACTIVO) {
            throw new BusinessException("El solicitante " + s.getNombreCompleto() + " está INACTIVO.");
        }
        prestamo.setSolicitante(s);

        if (prestamo.getFechaEsperadaDevolucion() == null) {
            throw new BusinessException("Ingrese la fecha esperada de devolución.");
        }
        if (prestamo.getFechaEsperadaDevolucion().toLocalDate().isBefore(LocalDate.now(java.time.ZoneId.of("America/Guatemala")))) {
            throw new BusinessException("La fecha esperada de devolución no puede ser anterior a hoy.");
        }
        if (prestamo.getObservaciones() != null) {
            prestamo.setObservaciones(prestamo.getObservaciones().isBlank() ? null : prestamo.getObservaciones().trim());
            if (prestamo.getObservaciones() != null && prestamo.getObservaciones().length() > 500) {
                throw new BusinessException("Las observaciones no pueden tener más de 500 caracteres.");
            }
        }
    }
}

