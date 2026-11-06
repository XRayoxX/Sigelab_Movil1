package gt.edu.sigelab.api;

import SIGELAB.enums.RolUsuario;
import SIGELAB.model.Usuario;
import SIGELAB.service.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FacialTest {
    @Test void livenessRejectionSendsOneNoticeAndDoesNotAuthorize() {
        ApiController auth = mock(ApiController.class); Usuario user = new Usuario(); user.setIdUsuario(5);
        when(auth.pendingUser(any(), eq(true))).thenReturn(user);
        FacialController facial = new FacialController(auth, "https://facial.example.invalid", "server-secret");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(facial, "http");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://facial.example.invalid/api/face/verify"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"Prueba de vida rechazada\"}"));
        try (var audit = mockConstruction(AuthService.class); var mail = mockConstruction(CorreoService.class)) {
            assertThrows(ResponseStatusException.class, () -> facial.verify(new MockHttpServletRequest(), "challenge", false,
                List.of(new MockMultipartFile("images", "frame.jpg", "image/jpeg", new byte[]{1,2,3}))));
            verify(mail.constructed().getFirst(), times(1)).notificarFalloFacial(user);
            verify(auth, never()).complete(any()); server.verify();
        }
    }
    @Test void serviceOutageDoesNotSendFalseFacialRejectionNotice() {
        ApiController auth = mock(ApiController.class); Usuario user = new Usuario(); user.setIdUsuario(5);
        when(auth.pendingUser(any(), eq(true))).thenReturn(user);
        FacialController facial = new FacialController(auth, "https://facial.example.invalid", "server-secret");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(facial, "http");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://facial.example.invalid/api/face/verify"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"Servicio no disponible\"}"));
        try (var mail = mockConstruction(CorreoService.class)) {
            assertThrows(ResponseStatusException.class, () -> facial.verify(new MockHttpServletRequest(), "challenge", false,
                List.of(new MockMultipartFile("images", "frame.jpg", "image/jpeg", new byte[]{1,2,3}))));
            assertTrue(mail.constructed().isEmpty()); verify(auth, never()).complete(any()); server.verify();
        }
    }
    @Test void existingServiceReceivesApiKeyOnlyFromServer() {
        FacialController facial = new FacialController(mock(ApiController.class), "https://facial.example.invalid/", "server-secret");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(facial, "http");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://facial.example.invalid/health"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("X-API-Key", "server-secret"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));
        assertEquals(Map.of("status", "ok"), facial.health());
        server.verify();
    }
    @Test void missingProfileStopsCaptureBeforeIssuingChallenge() {
        ApiController auth = mock(ApiController.class); Usuario user = new Usuario(); user.setIdUsuario(5);
        when(auth.pendingUser(any(), eq(false))).thenReturn(user);
        FacialController facial = new FacialController(auth, "https://facial.example.invalid", "server-secret");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(facial, "http");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://facial.example.invalid/api/face/status/5"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"enrolled\":false}", MediaType.APPLICATION_JSON));
        var failure = assertThrows(ResponseStatusException.class, () -> facial.challenge(new MockHttpServletRequest(), 5));
        assertEquals(409, failure.getStatusCode().value());
        server.verify(); verify(auth, never()).complete(any());
    }
    @Test void originalPoseErrorIsPreservedForAndroid() {
        FacialController facial = new FacialController(mock(ApiController.class), "https://facial.example.invalid", "server-secret");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(facial, "http");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://facial.example.invalid/health"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"Se detectó el giro hacia el lado contrario\",\"code\":\"POSE\"}"));
        var failure = assertThrows(ResponseStatusException.class, facial::health);
        assertEquals(422, failure.getStatusCode().value());
        assertEquals("Se detectó el giro hacia el lado contrario", failure.getReason());
        server.verify();
    }
    @Test void onlySuccessfulRemoteVerificationCompletesLogin() throws Exception {
        ApiController auth = mock(ApiController.class); Usuario user = new Usuario(); user.setIdUsuario(5);
        when(auth.pendingUser(any(), eq(true))).thenReturn(user);
        when(auth.complete(user)).thenReturn(Map.of("token", "authorized-token", "matched", true));
        FacialController facial = new FacialController(auth, "https://facial.example.invalid", "server-secret");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(facial, "http");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://facial.example.invalid/api/face/verify"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("X-API-Key", "server-secret"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().string(org.hamcrest.Matchers.containsString("name=\"userId\"")))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().string(org.hamcrest.Matchers.containsString("challenge-value")))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"matched\":true}", MediaType.APPLICATION_JSON));
        Object result = facial.verify(new MockHttpServletRequest(), "challenge-value", false,
            List.of(new MockMultipartFile("images", "frame.jpg", "image/jpeg", new byte[]{1,2,3})));
        assertEquals(Map.of("token", "authorized-token", "matched", true), result);
        verify(auth).complete(user); server.verify();
    }
    @Test void mismatchCannotCreateSession() throws Exception {
        ApiController auth = mock(ApiController.class); Usuario user = new Usuario(); user.setIdUsuario(5);
        when(auth.pendingUser(any(), eq(true))).thenReturn(user);
        FacialController facial = new FacialController(auth, "https://facial.example.invalid", "server-secret");
        RestTemplate http = mock(RestTemplate.class);
        when(http.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("matched", false)));
        ReflectionTestUtils.setField(facial, "http", http);
        try (var audit = mockConstruction(AuthService.class); var mail = mockConstruction(CorreoService.class)) {
            assertThrows(ResponseStatusException.class, () -> facial.verify(new MockHttpServletRequest(), "challenge", false,
                List.of(new MockMultipartFile("images", "frame.jpg", "image/jpeg", new byte[]{1,2,3}))));
            verify(auth, never()).complete(any());
            verify(audit.constructed().getFirst()).registrarFalloFacial(eq(user), anyString());
        }
    }
    @Test void operatorCannotEnrollOrDeleteFaces() {
        ApiController auth = mock(ApiController.class); Usuario user = new Usuario(); user.setRol(RolUsuario.ENCARGADO);
        when(auth.authorize(any())).thenReturn(user);
        FacialController facial = new FacialController(auth, "", "");
        assertThrows(ResponseStatusException.class, () -> facial.enroll(new MockHttpServletRequest(), 7, true, false, List.of()));
        assertThrows(ResponseStatusException.class, () -> facial.delete(new MockHttpServletRequest(), 7));
    }
    @Test void missingConfigurationNeverApprovesLogin() {
        ApiController auth = mock(ApiController.class); Usuario user = new Usuario(); user.setIdUsuario(5);
        when(auth.pendingUser(any(), eq(false))).thenReturn(user);
        FacialController facial = new FacialController(auth, "", "");
        assertThrows(ResponseStatusException.class, () -> facial.challenge(new MockHttpServletRequest(), 5));
        verify(auth, never()).complete(any());
    }
}
