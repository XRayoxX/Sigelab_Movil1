package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;
import SIGELAB.repository.UsuarioRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Seguridad;
import SIGELAB.util.Sesion;

import java.util.List;
import java.util.Optional;


public class UsuarioService {

    private static final int LARGO_MINIMO_CONTRASENA = 6;

    private final UsuarioRepository repository = new UsuarioRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    public List<Usuario> listar() {
        Sesion.exigirRol(RolUsuario.ADMIN);
        return repository.listar();
    }

    public List<Usuario> buscar(String texto) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        return texto == null || texto.isBlank() ? repository.listar() : repository.buscar(texto.trim());
    }

    public Usuario registrar(Usuario u, String contrasena, String confirmacion) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        validarDatos(u);
        if (contrasena == null || contrasena.isEmpty()) {
            throw new BusinessException("La contraseña es obligatoria para un usuario nuevo.");
        }
        u.setContrasenaHash(hashValidado(contrasena, confirmacion));
        validarUnicos(u);
        // El nombre de usuario NO se escribe: lo genera la base con el nombre completo
        // (trigger TR_Usuario_GenerarNombreUsuario). El repositorio lo devuelve en u.
        u.setNombreUsuario(null);
        repository.insertar(u);
        auditoria.registrar(AccionAuditoria.USUARIO_CREADO, "Usuario", u.getIdUsuario(),
                "Usuario " + u.getNombreUsuario() + " creado con rol " + u.getRol());
        new CorreoService().notificarRegistro(u);
        return u;
    }

    // Si la contraseña se deja vacía, se conserva la actual
    public void actualizar(Usuario u, String contrasena, String confirmacion) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (u.getIdUsuario() <= 0) {
            throw new BusinessException("Seleccione en la tabla el usuario que desea actualizar.");
        }
        validarDatos(u);
        boolean cambiaContrasena = contrasena != null && !contrasena.isEmpty();
        u.setContrasenaHash(cambiaContrasena ? hashValidado(contrasena, confirmacion) : null);
        if (u.getIdUsuario() == Sesion.getIdUsuario() && u.getEstado() == EstadoRegistro.INACTIVO) {
            throw new BusinessException("No puede desactivar su propio usuario.");
        }
        validarUnicos(u);
        repository.actualizar(u);
        auditoria.registrar(u.getEstado() == EstadoRegistro.INACTIVO
                        ? AccionAuditoria.USUARIO_DESACTIVADO : AccionAuditoria.USUARIO_MODIFICADO,
                "Usuario", u.getIdUsuario(), "Usuario " + u.getNombreUsuario() + " actualizado"
                        + (cambiaContrasena ? " (con cambio de contraseña)" : ""));
    }

    // Desactivación lógica para conservar el historial (sección 6.2)
    public void desactivar(int idUsuario) {
        Sesion.exigirRol(RolUsuario.ADMIN);
        if (idUsuario <= 0) {
            throw new BusinessException("Seleccione en la tabla el usuario que desea desactivar.");
        }
        if (idUsuario == Sesion.getIdUsuario()) {
            throw new BusinessException("No puede desactivar su propio usuario.");
        }
        repository.cambiarEstado(idUsuario, EstadoRegistro.INACTIVO);
        auditoria.registrar(AccionAuditoria.USUARIO_DESACTIVADO, "Usuario", idUsuario, "Usuario desactivado");
    }

    private void validarDatos(Usuario u) {
        u.setNombre(limpio(u.getNombre()));
        u.setSegundoNombre(limpio(u.getSegundoNombre()));       // vacío -> NULL en la base
        u.setApellido(limpio(u.getApellido()));
        u.setSegundoApellido(limpio(u.getSegundoApellido()));
        u.setCorreo(limpio(u.getCorreo()));
        u.setRutaFotoRostro(limpio(u.getRutaFotoRostro()));

        if (u.getNombre() == null || u.getApellido() == null) {
            throw new BusinessException("El primer nombre y el primer apellido son obligatorios.");
        }
        // Mismas letras que acepta dbo.GenerarNombreUsuario; así el usuario ve un mensaje claro
        // en lugar de un error técnico de la base al generar el nombre de usuario
        validarLetras(u.getNombre(), "primer nombre");
        validarLetras(u.getSegundoNombre(), "segundo nombre");
        validarLetras(u.getApellido(), "primer apellido");
        validarLetras(u.getSegundoApellido(), "segundo apellido");
        if (u.getNombre().length() > 60 || u.getApellido().length() > 60) {
            throw new BusinessException("El primer nombre y el primer apellido no pueden tener más de 60 caracteres.");
        }
        // Segundo nombre y segundo apellido son opcionales, pero con límite de largo
        if ((u.getSegundoNombre() != null && u.getSegundoNombre().length() > 100)
                || (u.getSegundoApellido() != null && u.getSegundoApellido().length() > 100)) {
            throw new BusinessException("El segundo nombre y el segundo apellido no pueden tener más de 100 caracteres.");
        }
        if (u.getCorreo() == null || !u.getCorreo().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("Ingrese un correo electrónico válido.");
        }
        if (u.getRol() == null) {
            throw new BusinessException("Seleccione un rol válido.");
        }
        if (u.getEstado() == null) {
            throw new BusinessException("Seleccione un estado válido.");
        }
    }

    private String hashValidado(String contrasena, String confirmacion) {
        if (contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            throw new BusinessException("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
        if (!contrasena.equals(confirmacion)) {
            throw new BusinessException("La contraseña y su confirmación no coinciden.");
        }
        return Seguridad.sha256(contrasena);
    }

    // Solo letras (con tildes y ñ), espacios y guiones; null = campo opcional vacío
    private void validarLetras(String texto, String campo) {
        if (texto != null && !texto.matches("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ \\-]+$")) {
            throw new BusinessException("El " + campo + " solo puede contener letras, espacios y guiones.");
        }
    }

    // El nombre de usuario no se revisa aquí: la base genera uno que no se repite
    private void validarUnicos(Usuario u) {
        Optional<Usuario> mismoCorreo = repository.buscarPorCorreo(u.getCorreo());
        if (mismoCorreo.isPresent() && mismoCorreo.get().getIdUsuario() != u.getIdUsuario()) {
            throw new BusinessException("Ya existe un usuario con el correo " + u.getCorreo() + ".");
        }
    }

    private String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
