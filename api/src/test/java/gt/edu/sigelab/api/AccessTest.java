package gt.edu.sigelab.api;

import SIGELAB.enums.*;
import SIGELAB.model.Usuario;
import SIGELAB.service.AuthService;
import SIGELAB.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.sql.DataSource;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccessTest {
    @AfterEach void cleanup() { Sesion.cerrar(); }
    private Usuario user(RolUsuario role) {
        Usuario u = new Usuario(); u.setIdUsuario(7); u.setNombre("Ana"); u.setNombreUsuario("ana"); u.setRol(role); return u;
    }
    @Test void passwordAloneDoesNotAuthorizeModules() throws Exception {
        Usuario u = user(RolUsuario.ADMIN);
        try (var authMock = mockConstruction(AuthService.class, (mock, context) ->
                when(mock.validarCredenciales("ana", "secret")).thenReturn(u))) {
            ApiController auth = new ApiController(mock(DataSource.class));
            var response = auth.login(new ApiController.LoginRequest("ana", "secret"), new MockHttpServletRequest());
            assertFalse(response.containsKey("token"));
            assertTrue(response.containsKey("pendingToken"));
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new ModulesController()).addFilters(new SessionFilter(auth)).setControllerAdvice(new ApiErrors()).build();
            mvc.perform(get("/api/usuarios").servletPath("/api/usuarios").header("Authorization", "Bearer " + response.get("pendingToken"))).andExpect(status().isUnauthorized());
        }
    }
    @Test void successfulFaceLoginSchedulesOneNoticeAfterAuthentication() {
        Usuario u = user(RolUsuario.ADMIN);
        try (var authMock = mockConstruction(AuthService.class, (m,c) -> doAnswer(inv -> { Sesion.iniciar(u); return null; }).when(m).completarLogin(any(), any()));
             var mail = mockConstruction(SIGELAB.service.CorreoService.class)) {
            ApiController controller = new ApiController(mock(DataSource.class));
            assertEquals(true, controller.complete(u).get("matched"));
            var order = inOrder(authMock.constructed().getFirst(), mail.constructed().getFirst());
            order.verify(authMock.constructed().getFirst()).completarLogin(eq(u), any());
            order.verify(mail.constructed().getFirst()).notificarInicioSesion(u);
            assertNull(Sesion.getUsuarioActual());
        }
    }
    @Test void failedAuthenticationNeverSchedulesSuccessfulLoginNotice() {
        Usuario u = user(RolUsuario.ADMIN);
        try (var authMock = mockConstruction(AuthService.class, (m,c) -> doThrow(new BusinessException("Cuenta inactiva")).when(m).completarLogin(any(), any()));
             var mail = mockConstruction(SIGELAB.service.CorreoService.class)) {
            ApiController controller = new ApiController(mock(DataSource.class));
            assertThrows(BusinessException.class, () -> controller.complete(u));
            assertTrue(mail.constructed().isEmpty());
        }
    }
    @Test void teacherCannotAccessAdministrativeOrOperationalModules() throws Exception {
        ApiController auth = mock(ApiController.class);
        when(auth.authorize(any())).thenReturn(user(RolUsuario.DOCENTE));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ModulesController()).addFilters(new SessionFilter(auth)).setControllerAdvice(new ApiErrors()).build();
        for (String path : new String[]{"/api/usuarios", "/api/auditoria", "/api/equipos", "/api/prestamos", "/api/mantenimiento"}) {
            mvc.perform(get(path).servletPath(path).header("Authorization", "Bearer test")).andExpect(status().isForbidden());
            assertNull(Sesion.getUsuarioActual(), "La identidad debe limpiarse al terminar cada solicitud");
        }
    }
    @Test void operatorCannotEditCatalogs() throws Exception {
        ApiController auth = mock(ApiController.class);
        when(auth.authorize(any())).thenReturn(user(RolUsuario.ENCARGADO));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ModulesController()).addFilters(new SessionFilter(auth)).setControllerAdvice(new ApiErrors()).build();
        mvc.perform(post("/api/laboratorios").servletPath("/api/laboratorios").contentType("application/json")
                .content("{\"codigo\":\"LAB-1\",\"nombre\":\"Prueba\",\"ubicacion\":\"Edificio\",\"capacidad\":10}")).andExpect(status().isForbidden());
    }
    @Test void concurrentRequestsNeverShareIdentity() throws Exception {
        Usuario main = user(RolUsuario.ADMIN);
        Sesion.iniciar(main);
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            worker.submit(() -> {
                assertNull(Sesion.getUsuarioActual());
                Usuario other = user(RolUsuario.DOCENTE); Sesion.iniciar(other);
                assertEquals(RolUsuario.DOCENTE, Sesion.getUsuarioActual().getRol());
                Sesion.cerrar(); assertNull(Sesion.getUsuarioActual());
            }).get(5, TimeUnit.SECONDS);
        }
        assertSame(main, Sesion.getUsuarioActual());
    }
    @Test void passwordsAreNeverSerialized() throws Exception {
        Usuario user = user(RolUsuario.ADMIN); user.setContrasenaHash("secret");
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(user);
        assertFalse(json.contains("contrasenaHash")); assertFalse(json.contains("secret"));
    }
}
