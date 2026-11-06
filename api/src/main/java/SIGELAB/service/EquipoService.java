package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Equipo;
import SIGELAB.model.ResultadoReporte;
import SIGELAB.repository.EquipoRepository;
import SIGELAB.repository.ReporteRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;

import java.util.List;
import java.util.Optional;

/**
 * Reglas de Equipo.
 * - RN-04: el código de inventario es único.
 * - Un equipo nuevo siempre nace DISPONIBLE.
 * - El estado NO se edita a mano: solo cambia con préstamos, devoluciones y mantenimiento.
 */
public class EquipoService {

    private final EquipoRepository repository = new EquipoRepository();
    private final ReporteRepository reporteRepository = new ReporteRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    public List<Equipo> listar(String texto, EstadoEquipo estado, Integer idLaboratorio) {
        String filtro = texto == null || texto.isBlank() ? null : texto.trim();
        return repository.listar(filtro, estado, idLaboratorio);
    }

    public List<Equipo> listarPorEstados(EstadoEquipo... estados) {
        return repository.listarPorEstados(estados);
    }

    /**
     * Identifica un equipo por su código (escrito o leído del QR).
     * Solo consulta: no cambia ningún estado (RN-19).
     */
    public Equipo identificarPorCodigo(String codigoInventario) {
        String codigo = codigoInventario == null ? "" : codigoInventario.trim().toUpperCase();
        if (codigo.isEmpty()) {
            throw new BusinessException("Escriba o lea el código de inventario del equipo.");
        }
        return repository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new BusinessException("No existe ningún equipo con el código " + codigo + "."));
    }

    public Equipo registrar(Equipo e) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        validar(e);
        e.setEstado(EstadoEquipo.DISPONIBLE);
        validarUnicos(e);
        e.setIdEquipo(repository.insertar(e));
        auditoria.registrar(AccionAuditoria.EQUIPO_CREADO, "Equipo", e.getIdEquipo(),
                "Equipo " + e.getCodigoInventario() + " registrado como DISPONIBLE");
        return e;
    }

    public void actualizar(Equipo e) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (e.getIdEquipo() <= 0) {
            throw new BusinessException("Seleccione en la tabla el equipo que desea actualizar.");
        }
        validar(e);
        validarUnicos(e);
        repository.actualizarDatos(e);
        auditoria.registrar(AccionAuditoria.EQUIPO_MODIFICADO, "Equipo", e.getIdEquipo(),
                "Datos del equipo " + e.getCodigoInventario() + " actualizados");
    }

    public ResultadoReporte historial(String codigoInventario) {
        Equipo equipo = identificarPorCodigo(codigoInventario);
        return reporteRepository.historialEquipo(equipo.getCodigoInventario());
    }

    private void validar(Equipo e) {
        e.setCodigoInventario(e.getCodigoInventario() == null ? null : e.getCodigoInventario().trim().toUpperCase());
        e.setNombre(limpio(e.getNombre()));
        e.setMarca(limpio(e.getMarca()));
        e.setModelo(limpio(e.getModelo()));
        e.setNumeroSerie(limpio(e.getNumeroSerie()));
        e.setObservaciones(limpio(e.getObservaciones()));

        if (e.getCodigoInventario() == null || e.getCodigoInventario().isEmpty()) {
            throw new BusinessException("El código de inventario es obligatorio.");
        }
        if (e.getCodigoInventario().length() > 20) {
            throw new BusinessException("El código de inventario no puede tener más de 20 caracteres.");
        }
        if (e.getNombre() == null || e.getMarca() == null || e.getModelo() == null) {
            throw new BusinessException("El nombre, la marca y el modelo son obligatorios.");
        }
        if (e.getTipoEquipo() == null) {
            throw new BusinessException("Seleccione un tipo de equipo válido.");
        }
        if (e.getLaboratorio() == null) {
            throw new BusinessException("Seleccione un laboratorio válido.");
        }
        if (e.getObservaciones() != null && e.getObservaciones().length() > 500) {
            throw new BusinessException("Las observaciones no pueden tener más de 500 caracteres.");
        }
    }

    private void validarUnicos(Equipo e) {
        // RN-04: mensaje claro en lugar de la SQLException de clave única
        Optional<Equipo> mismoCodigo = repository.buscarPorCodigo(e.getCodigoInventario());
        if (mismoCodigo.isPresent() && mismoCodigo.get().getIdEquipo() != e.getIdEquipo()) {
            throw new BusinessException("Ya existe un equipo con el código " + e.getCodigoInventario() + ".");
        }
        if (e.getNumeroSerie() != null) {
            Optional<Equipo> mismaSerie = repository.buscarPorNumeroSerie(e.getNumeroSerie());
            if (mismaSerie.isPresent() && mismaSerie.get().getIdEquipo() != e.getIdEquipo()) {
                throw new BusinessException("El número de serie " + e.getNumeroSerie() + " ya pertenece al equipo "
                        + mismaSerie.get().getCodigoInventario() + ".");
            }
        }
    }

    private String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
