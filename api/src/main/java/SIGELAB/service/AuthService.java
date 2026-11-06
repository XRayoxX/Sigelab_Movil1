package SIGELAB.service;

import SIGELAB.enums.AccionAuditoria;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.facial.VerificacionFacial;
import SIGELAB.model.Usuario;
import SIGELAB.repository.AuditoriaRepository;
import SIGELAB.repository.UsuarioRepository;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Seguridad;
import SIGELAB.util.Sesion;

import java.util.Optional;

public class AuthService {

    private static final String MENSAJE_CREDENCIALES =
            "Usuario o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository =
            new UsuarioRepository();

    private final AuditoriaRepository auditoriaRepository =
            new AuditoriaRepository();

    // RN-01: validar usuario, contraseña y estado.
    public Usuario validarCredenciales(
            String nombreUsuario,
            String contrasena
    ) {
        String nombre = nombreUsuario == null
                ? ""
                : nombreUsuario.trim();

        if (nombre.isEmpty()
                || contrasena == null
                || contrasena.isEmpty()) {
            throw new BusinessException(
                    "Ingrese su usuario y contraseña."
            );
        }

        Optional<Usuario> encontrado =
                usuarioRepository.buscarPorNombreUsuario(nombre);

        if (encontrado.isEmpty()) {
            auditoriaRepository.registrar(
                    null,
                    recortar(nombre),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    null,
                    "Usuario no registrado"
            );

            throw new BusinessException(MENSAJE_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();

        String hashAlmacenado = usuario.getContrasenaHash();
        String hashIngresado = Seguridad.sha256(contrasena);

        if (hashAlmacenado == null
                || !hashAlmacenado.equalsIgnoreCase(hashIngresado)) {

            auditoriaRepository.registrar(
                    usuario.getIdUsuario(),
                    usuario.getNombreUsuario(),
                    AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario",
                    usuario.getIdUsuario(),
                    "Contraseña incorrecta"
            );

            throw new BusinessException(MENSAJE_CREDENCIALES);
        }

        validarUsuarioActivo(usuario);

        return usuario;
    }

    // RN-01 + RN-02:
    // No abre sesión sin aprobación facial.
    public void completarLogin(
            Usuario usuario,
            VerificacionFacial.Resultado resultadoFacial
    ) {
        if (usuario == null) {
            throw new BusinessException(
                    "No existe un usuario válido para iniciar sesión."
            );
        }

        if (resultadoFacial != VerificacionFacial.Resultado.APROBADO) {
            registrarFalloFacial(
                    usuario,
                    "Intento de completar sesión sin aprobación facial"
            );

            throw new BusinessException(
                    "El reconocimiento facial es obligatorio."
            );
        }

        // Consultar nuevamente el estado actual en la base de datos.
        Optional<Usuario> encontrado =
                usuarioRepository.buscarPorNombreUsuario(
                        usuario.getNombreUsuario()
                );

        if (encontrado.isEmpty()
                || encontrado.get().getIdUsuario() != usuario.getIdUsuario()) {

            throw new BusinessException(
                    "La cuenta ya no se encuentra disponible."
            );
        }

        Usuario usuarioActualizado = encontrado.get();

        validarUsuarioActivo(usuarioActualizado);

        Sesion.iniciar(usuarioActualizado);

        auditoriaRepository.registrar(
                usuarioActualizado.getIdUsuario(),
                usuarioActualizado.getNombreUsuario(),
                AccionAuditoria.LOGIN_EXITOSO,
                "Usuario",
                usuarioActualizado.getIdUsuario(),
                "Inicio de sesión con validación facial aprobada"
        );
    }

    private void validarUsuarioActivo(Usuario usuario) {
        if (usuario.getEstado() == EstadoRegistro.ACTIVO) {
            return;
        }

        auditoriaRepository.registrar(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                AccionAuditoria.LOGIN_FALLIDO,
                "Usuario",
                usuario.getIdUsuario(),
                "Intento de inicio de sesión con cuenta inactiva"
        );

        throw new BusinessException(
                "El usuario " + usuario.getNombreUsuario()
                        + " está INACTIVO. "
                        + "Comuníquese con el administrador."
        );
    }

    public void registrarFalloFacial(
            Usuario usuario,
            String detalle
    ) {
        if (usuario == null) {
            return;
        }

        auditoriaRepository.registrar(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                AccionAuditoria.LOGIN_FACIAL_FALLIDO,
                "Usuario",
                usuario.getIdUsuario(),
                detalle == null || detalle.isBlank()
                        ? "Validación facial rechazada"
                        : detalle
        );
    }

    public void cerrarSesion() {
        Sesion.cerrar();
    }

    private String recortar(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.length() > 30
                ? texto.substring(0, 30)
                : texto;
    }
}