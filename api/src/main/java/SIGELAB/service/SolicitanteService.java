package SIGELAB.service;

import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Solicitante;
import SIGELAB.repository.SolicitanteRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Sesion;

import java.util.List;
import java.util.Optional;

/**
 * Reglas de Solicitante: carné único, datos obligatorios y correo con formato válido.
 */
public class SolicitanteService {

    private final SolicitanteRepository repository = new SolicitanteRepository();

    public List<Solicitante> listar() {
        return repository.listar();
    }

    // Solo los ACTIVOS pueden recibir un préstamo
    public List<Solicitante> listarActivos() {
        return repository.listarActivos();
    }

    public List<Solicitante> buscar(String texto) {
        return texto == null || texto.isBlank() ? repository.listar() : repository.buscar(texto.trim());
    }

    public Solicitante registrar(Solicitante s) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        validar(s);
        if (repository.buscarPorCarne(s.getCarne()).isPresent()) {
            throw new BusinessException("Ya existe un solicitante con el carné " + s.getCarne() + ".");
        }
        s.setIdSolicitante(repository.insertar(s));
        return s;
    }

    public void actualizar(Solicitante s) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (s.getIdSolicitante() <= 0) {
            throw new BusinessException("Seleccione en la tabla el solicitante que desea actualizar.");
        }
        validar(s);
        Optional<Solicitante> mismoCarne = repository.buscarPorCarne(s.getCarne());
        if (mismoCarne.isPresent() && mismoCarne.get().getIdSolicitante() != s.getIdSolicitante()) {
            throw new BusinessException("Ya existe otro solicitante con el carné " + s.getCarne() + ".");
        }
        repository.actualizar(s);
    }

    public void desactivar(int idSolicitante) {
        Sesion.exigirRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        if (idSolicitante <= 0) {
            throw new BusinessException("Seleccione en la tabla el solicitante que desea desactivar.");
        }
        repository.cambiarEstado(idSolicitante, EstadoRegistro.INACTIVO);
    }

    private void validar(Solicitante s) {
        s.setCarne(limpio(s.getCarne()));
        s.setNombre(limpio(s.getNombre()));
        s.setApellido(limpio(s.getApellido()));
        s.setCorreo(limpio(s.getCorreo()));
        s.setTelefono(limpio(s.getTelefono()));

        if (s.getCarne() == null) {
            throw new BusinessException("El carné o código del solicitante es obligatorio.");
        }
        if (s.getCarne().length() > 20) {
            throw new BusinessException("El carné no puede tener más de 20 caracteres.");
        }
        if (s.getNombre() == null || s.getApellido() == null) {
            throw new BusinessException("El nombre y el apellido son obligatorios.");
        }
        if (s.getTipo() == null) {
            throw new BusinessException("Seleccione el tipo de solicitante.");
        }
        if (s.getCorreo() != null && !s.getCorreo().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("El correo electrónico no tiene un formato válido.");
        }
        if (s.getTelefono() != null && !s.getTelefono().matches("^[0-9+\\- ]{8,15}$")) {
            throw new BusinessException("El teléfono debe tener entre 8 y 15 dígitos (se permiten guiones y espacios).");
        }
        if (s.getEstado() == null) {
            throw new BusinessException("Seleccione un estado válido.");
        }
    }

    private String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
