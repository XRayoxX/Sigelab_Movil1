package SIGELAB.service;

import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.model.*;
import SIGELAB.util.Configuracion;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorreoServiceTest {
    private MockedStatic<Configuracion> config;
    private List<CorreoService.Aviso> delivered;
    private CorreoService mail;
    private Solicitante applicant;
    private Equipo equipment;

    @BeforeEach void setup() {
        config = mockStatic(Configuracion.class);
        config.when(() -> Configuracion.valor("correo.habilitado", "true")).thenReturn("true");
        config.when(() -> Configuracion.valor("correo.remitente")).thenReturn("sender@example.invalid");
        config.when(() -> Configuracion.valor("correo.contrasena")).thenReturn("test-secret");
        delivered = new ArrayList<>();
        mail = new CorreoService(Clock.fixed(Instant.parse("2026-10-30T18:30:00Z"), ZoneId.of("America/Guatemala")), delivered::add);
        applicant = new Solicitante(); applicant.setNombre("Ana"); applicant.setApellido("Pérez"); applicant.setCorreo("ana@example.invalid");
        equipment = new Equipo(); equipment.setCodigoInventario("EQ-10"); equipment.setNombre("Microscopio <A>");
    }
    @AfterEach void cleanup() { config.close(); }

    @Test void successfulLoginNoticeContainsUserAndLocalTime() {
        Usuario user = new Usuario(); user.setNombre("Ana <A>"); user.setNombreUsuario("aperez"); user.setCorreo("ana@example.invalid");
        mail.notificarInicioSesion(user);
        assertEquals(1, delivered.size());
        assertEquals("ana@example.invalid", delivered.getFirst().destinatario());
        assertTrue(delivered.getFirst().html().contains("verificación facial"));
        assertTrue(delivered.getFirst().html().contains("30/10/2026 12:30"));
        assertTrue(delivered.getFirst().html().contains("Ana &lt;A&gt;"));
    }
    @Test void registrationNoticeContainsGeneratedUsernameButNeverPasswordHash() {
        Usuario user = new Usuario(); user.setNombre("Ana"); user.setNombreUsuario("aperez"); user.setCorreo("ana@example.invalid"); user.setContrasenaHash("sensitive-hash");
        mail.notificarRegistro(user);
        assertEquals(1, delivered.size());
        assertTrue(delivered.getFirst().html().contains("aperez"));
        assertFalse(delivered.getFirst().html().contains("sensitive-hash"));
    }
    @Test void loanNoticeIncludesActualEquipmentAndGuatemalaTime() {
        Prestamo loan = new Prestamo(); loan.setSolicitante(applicant); loan.setFechaEsperadaDevolucion(LocalDateTime.of(2026,11,2,17,0));
        mail.notificarPrestamo(12, loan, List.of(equipment));
        assertEquals(1, delivered.size()); var message = delivered.getFirst();
        assertEquals("ana@example.invalid", message.destinatario());
        assertTrue(message.asunto().contains("12"));
        assertTrue(message.html().contains("EQ-10"));
        assertTrue(message.html().contains("Microscopio &lt;A&gt;"));
        assertTrue(message.html().contains("30/10/2026 12:30"));
        assertTrue(message.html().contains("02/11/2026 17:00"));
    }
    @Test void partialReturnNoticeShowsPendingEquipmentAndCondition() {
        mail.notificarDevolucion(applicant, 12, equipment, CondicionDevolucion.DANADO, 2);
        assertEquals(1, delivered.size());
        assertTrue(delivered.getFirst().html().contains("2</b> equipo(s) pendiente(s)"));
        assertTrue(delivered.getFirst().html().contains("DANADO"));
        assertFalse(delivered.getFirst().html().contains("quedó <b>FINALIZADO"));
    }
    @Test void finalReturnNoticeConfirmsLoanClosure() {
        mail.notificarDevolucion(applicant, 12, equipment, CondicionDevolucion.BUENO, 0);
        assertTrue(delivered.getFirst().html().contains("quedó <b>FINALIZADO</b>"));
    }
    @Test void rejectedFaceAlertsAccountOwner() {
        Usuario user = new Usuario(); user.setNombre("Ana <script>"); user.setNombreUsuario("aperez"); user.setCorreo("owner@example.invalid");
        mail.notificarFalloFacial(user);
        assertEquals("owner@example.invalid", delivered.getFirst().destinatario());
        assertTrue(delivered.getFirst().html().contains("aperez"));
        assertTrue(delivered.getFirst().html().contains("Ana &lt;script&gt;"));
        assertTrue(delivered.getFirst().html().contains("30/10/2026 12:30"));
    }
    @Test void missingApplicantEmailDoesNotScheduleAnything() {
        applicant.setCorreo(null);
        mail.notificarDevolucion(applicant, 12, equipment, CondicionDevolucion.BUENO, 0);
        assertTrue(delivered.isEmpty());
    }
    @Test void disabledSmtpDoesNotScheduleAnything() {
        config.when(() -> Configuracion.valor("correo.habilitado", "true")).thenReturn("false");
        mail.notificarDevolucion(applicant, 12, equipment, CondicionDevolucion.BUENO, 0);
        assertTrue(delivered.isEmpty());
    }
    @Test void schedulingFailureDoesNotUndoBusinessOperation() {
        mail = new CorreoService(Clock.systemUTC(), ignored -> { throw new IllegalStateException("SMTP unavailable"); });
        assertDoesNotThrow(() -> mail.notificarDevolucion(applicant, 12, equipment, CondicionDevolucion.BUENO, 0));
    }
}
