package SIGELAB.service;
import SIGELAB.enums.*;
import SIGELAB.model.Usuario;
import SIGELAB.repository.UsuarioRepository;
import SIGELAB.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RegistroCorreoTest {
    @AfterEach void cleanup() { Sesion.cerrar(); }
    private Usuario account() {
        Usuario u = new Usuario(); u.setNombre("Ana"); u.setApellido("Perez"); u.setCorreo("ana@example.invalid"); u.setRol(RolUsuario.ADMIN); u.setEstado(EstadoRegistro.ACTIVO); return u;
    }
    @Test void savedAccountNotifiesAfterDatabaseGeneratedUsername() {
        Sesion.iniciar(account()); Usuario u = account();
        try (var repo = mockConstruction(UsuarioRepository.class, (m,c) -> when(m.insertar(any())).thenAnswer(inv -> { u.setIdUsuario(9); u.setNombreUsuario("aperez"); return 9; }));
             var audit = mockConstruction(AuditoriaService.class); var mail = mockConstruction(CorreoService.class)) {
            assertSame(u, new UsuarioService().registrar(u, "secret123", "secret123"));
            var order = inOrder(repo.constructed().getFirst(), mail.constructed().getFirst());
            order.verify(repo.constructed().getFirst()).insertar(u);
            order.verify(mail.constructed().getFirst()).notificarRegistro(u);
            assertEquals("aperez", u.getNombreUsuario());
        }
    }
    @Test void failedInsertDoesNotNotify() {
        Sesion.iniciar(account());
        try (var repo = mockConstruction(UsuarioRepository.class, (m,c) -> when(m.insertar(any())).thenThrow(new BusinessException("No guardado")));
             var audit = mockConstruction(AuditoriaService.class); var mail = mockConstruction(CorreoService.class)) {
            assertThrows(BusinessException.class, () -> new UsuarioService().registrar(account(), "secret123", "secret123"));
            assertTrue(mail.constructed().isEmpty());
        }
    }
}
