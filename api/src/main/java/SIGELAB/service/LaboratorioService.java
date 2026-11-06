package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Laboratorio;
import SIGELAB.repository.LaboratorioRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;

import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio de Laboratorio. Aquí se decide SI una operación puede hacerse;
 * el Repository solo la ejecuta.
 */
public class LaboratorioService {

    private final LaboratorioRepository repository = new LaboratorioRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    public List<Laboratorio> listar() {
        return repository.listar();
    }

    // Para los ComboBox (Equipos, Reportes): solo laboratorios ACTIVOS
    public List<Laboratorio> listarActivos() {
        return repository.listarActivos();
    }

    public List<Laboratorio> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return repository.listar();
        }
        return repository.buscar(texto.trim());
    }

    public Laboratorio registrar(Laboratorio lab) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        normalizar(lab);
        validarCampos(lab);

        // RN-03: el código de laboratorio debe ser único
        if (repository.buscarPorCodigo(lab.getCodigo()).isPresent()) {
            throw new BusinessException("Ya existe un laboratorio con el código " + lab.getCodigo() + ".");
        }
        lab.setIdLaboratorio(repository.insertar(lab));
        auditoria.registrar(AccionAuditoria.LABORATORIO_CREADO, "Laboratorio", lab.getIdLaboratorio(),
                "Laboratorio " + lab.getCodigo() + " creado");
        return lab;
    }

    public void actualizar(Laboratorio lab) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (lab.getIdLaboratorio() <= 0) {
            throw new BusinessException("Seleccione en la tabla el laboratorio que desea actualizar.");
        }
        normalizar(lab);
        validarCampos(lab);

        // RN-03: el código puede quedar igual, pero no puede ser el de OTRO laboratorio
        Optional<Laboratorio> mismoCodigo = repository.buscarPorCodigo(lab.getCodigo());
        if (mismoCodigo.isPresent() && mismoCodigo.get().getIdLaboratorio() != lab.getIdLaboratorio()) {
            throw new BusinessException("Ya existe otro laboratorio con el código " + lab.getCodigo() + ".");
        }
        repository.actualizar(lab);
        auditoria.registrar(AccionAuditoria.LABORATORIO_MODIFICADO, "Laboratorio", lab.getIdLaboratorio(),
                "Laboratorio " + lab.getCodigo() + " actualizado (estado " + lab.getEstado() + ")");
    }

    // Desactivación lógica: no se borra para conservar el historial de sus equipos
    public void desactivar(int idLaboratorio) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (idLaboratorio <= 0) {
            throw new BusinessException("Seleccione en la tabla el laboratorio que desea desactivar.");
        }
        repository.cambiarEstado(idLaboratorio, EstadoRegistro.INACTIVO);
        auditoria.registrar(AccionAuditoria.LABORATORIO_MODIFICADO, "Laboratorio", idLaboratorio, "Laboratorio desactivado");
    }

    private void normalizar(Laboratorio lab) {
        lab.setCodigo(lab.getCodigo() == null ? "" : lab.getCodigo().trim().toUpperCase());
        lab.setNombre(lab.getNombre() == null ? "" : lab.getNombre().trim());
        lab.setUbicacion(lab.getUbicacion() == null ? "" : lab.getUbicacion().trim());
    }

    private void validarCampos(Laboratorio lab) {
        if (lab.getCodigo().isEmpty()) {
            throw new BusinessException("El código del laboratorio es obligatorio.");
        }
        if (lab.getCodigo().length() > 15) {
            throw new BusinessException("El código no puede tener más de 15 caracteres.");
        }
        if (lab.getNombre().isEmpty()) {
            throw new BusinessException("El nombre del laboratorio es obligatorio.");
        }
        if (lab.getUbicacion().isEmpty()) {
            throw new BusinessException("La ubicación del laboratorio es obligatoria.");
        }
        if (lab.getCapacidad() <= 0) {
            throw new BusinessException("La capacidad debe ser un número mayor que cero.");
        }
        if (lab.getEstado() == null) {
            throw new BusinessException("Seleccione un estado válido.");
        }
    }
}
