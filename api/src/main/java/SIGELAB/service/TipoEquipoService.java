package SIGELAB.service;

import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.TipoEquipo;
import SIGELAB.repository.TipoEquipoRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;

import java.util.List;
import java.util.Optional;

/**
 * Reglas del catálogo de tipos de equipo: nombre obligatorio y único.
 */
public class TipoEquipoService {

    private final TipoEquipoRepository repository = new TipoEquipoRepository();

    public List<TipoEquipo> listar() {
        return repository.listar();
    }

    public List<TipoEquipo> listarActivos() {
        return repository.listarActivos();
    }

    public List<TipoEquipo> buscar(String texto) {
        return texto == null || texto.isBlank() ? repository.listar() : repository.buscar(texto.trim());
    }

    public TipoEquipo registrar(TipoEquipo tipo) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        validar(tipo);
        if (repository.buscarPorNombre(tipo.getNombre()).isPresent()) {
            throw new BusinessException("Ya existe un tipo de equipo llamado " + tipo.getNombre() + ".");
        }
        tipo.setIdTipoEquipo(repository.insertar(tipo));
        return tipo;
    }

    public void actualizar(TipoEquipo tipo) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (tipo.getIdTipoEquipo() <= 0) {
            throw new BusinessException("Seleccione en la tabla el tipo de equipo que desea actualizar.");
        }
        validar(tipo);
        Optional<TipoEquipo> mismoNombre = repository.buscarPorNombre(tipo.getNombre());
        if (mismoNombre.isPresent() && mismoNombre.get().getIdTipoEquipo() != tipo.getIdTipoEquipo()) {
            throw new BusinessException("Ya existe otro tipo de equipo llamado " + tipo.getNombre() + ".");
        }
        repository.actualizar(tipo);
    }

    public void desactivar(int idTipoEquipo) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (idTipoEquipo <= 0) {
            throw new BusinessException("Seleccione en la tabla el tipo de equipo que desea desactivar.");
        }
        repository.cambiarEstado(idTipoEquipo, EstadoRegistro.INACTIVO);
    }

    private void validar(TipoEquipo tipo) {
        tipo.setNombre(tipo.getNombre() == null ? "" : tipo.getNombre().trim());
        if (tipo.getNombre().isEmpty()) {
            throw new BusinessException("El nombre del tipo de equipo es obligatorio.");
        }
        if (tipo.getNombre().length() > 50) {
            throw new BusinessException("El nombre no puede tener más de 50 caracteres.");
        }
        if (tipo.getDescripcion() != null) {
            tipo.setDescripcion(tipo.getDescripcion().isBlank() ? null : tipo.getDescripcion().trim());
            if (tipo.getDescripcion() != null && tipo.getDescripcion().length() > 150) {
                throw new BusinessException("La descripción no puede tener más de 150 caracteres.");
            }
        }
        if (tipo.getEstado() == null) {
            throw new BusinessException("Seleccione un estado válido.");
        }
    }
}
