package gt.edu.sigelab.api;

import SIGELAB.enums.*;
import SIGELAB.model.*;
import SIGELAB.repository.*;
import SIGELAB.service.*;
import SIGELAB.util.*;
import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class WorkflowTest {
    private Connection connection;
    @BeforeEach void setup() throws Exception {
        var source = mock(DataSource.class); connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection); ConexionBD.configurar(source);
        Usuario user = new Usuario(); user.setIdUsuario(1); user.setNombreUsuario("admin"); user.setRol(RolUsuario.ADMIN); Sesion.iniciar(user);
    }
    @AfterEach void cleanup() { Sesion.cerrar(); }

    private Equipo equipment(EstadoEquipo state) {
        Equipo e = new Equipo(); e.setIdEquipo(4); e.setCodigoInventario("EQ-4"); e.setEstado(state); return e;
    }
    private DetallePrestamo detail(EstadoDetalle state) {
        DetallePrestamo d = new DetallePrestamo(); d.setIdDetallePrestamo(8); d.setIdPrestamo(2); d.setEstado(state);
        d.setEstadoPrestamo(EstadoPrestamo.ACTIVO); d.setEquipo(equipment(EstadoEquipo.PRESTADO)); return d;
    }
    @Test void partialReturnKeepsLoanOpenAndCommits() throws Exception {
        try (var loans = mockConstruction(PrestamoRepository.class, (m,c) -> {
                when(m.buscarDetalle(any(), eq(8))).thenReturn(Optional.of(detail(EstadoDetalle.PENDIENTE)));
                when(m.marcarDetalleDevuelto(any(), eq(8))).thenReturn(true);
                when(m.contarPendientes(any(), eq(2))).thenReturn(1);
            }); var equipment = mockConstruction(EquipoRepository.class, (m,c) -> when(m.cambiarEstado(any(), eq(4), eq(EstadoEquipo.PRESTADO), eq(EstadoEquipo.DISPONIBLE))).thenReturn(true));
             var returns = mockConstruction(DevolucionRepository.class);
             var audits = mockConstruction(AuditoriaService.class);
             var applicants = mockConstruction(SolicitanteRepository.class);
             var mail = mockConstruction(CorreoService.class)) {
            assertEquals(1, new DevolucionService().registrar(8, CondicionDevolucion.BUENO, ""));
            verify(loans.constructed().getFirst()).bloquearPrestamoPorDetalle(connection, 8);
            verify(loans.constructed().getFirst(), never()).finalizar(any(), anyInt());
            verify(connection).commit(); verify(connection, never()).rollback();
        }
    }
    @Test void finalDamagedReturnClosesLoanAndMarksEquipmentDamaged() throws Exception {
        try (var loans = mockConstruction(PrestamoRepository.class, (m,c) -> {
                when(m.buscarDetalle(any(), eq(8))).thenReturn(Optional.of(detail(EstadoDetalle.PENDIENTE)));
                when(m.marcarDetalleDevuelto(any(), eq(8))).thenReturn(true);
                when(m.contarPendientes(any(), eq(2))).thenReturn(0);
            }); var equipment = mockConstruction(EquipoRepository.class, (m,c) -> when(m.cambiarEstado(any(), eq(4), eq(EstadoEquipo.PRESTADO), eq(EstadoEquipo.DANADO))).thenReturn(true));
             var returns = mockConstruction(DevolucionRepository.class); var audits = mockConstruction(AuditoriaService.class);
             var applicants = mockConstruction(SolicitanteRepository.class); var mail = mockConstruction(CorreoService.class)) {
            assertEquals(0, new DevolucionService().registrar(8, CondicionDevolucion.DANADO, "Pantalla rota"));
            verify(loans.constructed().getFirst()).finalizar(connection, 2);
            verify(equipment.constructed().getFirst()).cambiarEstado(connection, 4, EstadoEquipo.PRESTADO, EstadoEquipo.DANADO);
            verify(connection).commit();
        }
    }
    @Test void duplicateReturnRollsBackWithoutInsertingReturn() throws Exception {
        try (var loans = mockConstruction(PrestamoRepository.class, (m,c) -> when(m.buscarDetalle(any(), eq(8))).thenReturn(Optional.of(detail(EstadoDetalle.DEVUELTO))));
             var equipment = mockConstruction(EquipoRepository.class); var returns = mockConstruction(DevolucionRepository.class);
             var audits = mockConstruction(AuditoriaService.class); var applicants = mockConstruction(SolicitanteRepository.class); var mail = mockConstruction(CorreoService.class)) {
            assertThrows(BusinessException.class, () -> new DevolucionService().registrar(8, CondicionDevolucion.BUENO, ""));
            verifyNoInteractions(returns.constructed().getFirst());
            verify(connection).rollback(); verify(connection, never()).commit();
        }
    }
    @Test void unavailableEquipmentRollsBackWholeLoan() throws Exception {
        Solicitante s = new Solicitante(); s.setIdSolicitante(3); s.setEstado(EstadoRegistro.ACTIVO);
        Prestamo loan = new Prestamo(); loan.setSolicitante(s); loan.setFechaEsperadaDevolucion(LocalDateTime.now().plusDays(2));
        try (var loans = mockConstruction(PrestamoRepository.class, (m,c) -> when(m.insertarEncabezado(any(), any(), anyInt())).thenReturn(10));
             var equipment = mockConstruction(EquipoRepository.class, (m,c) -> when(m.buscarPorId(any(), eq(4))).thenReturn(Optional.of(equipment(EstadoEquipo.PRESTADO))));
             var applicants = mockConstruction(SolicitanteRepository.class, (m,c) -> when(m.buscarPorId(3)).thenReturn(Optional.of(s)));
             var audits = mockConstruction(AuditoriaService.class); var mail = mockConstruction(CorreoService.class)) {
            assertThrows(BusinessException.class, () -> new PrestamoService().confirmar(loan, List.of(equipment(EstadoEquipo.DISPONIBLE))));
            verify(connection).rollback(); verify(connection, never()).commit();
            verify(loans.constructed().getFirst(), never()).insertarDetalle(any(), anyInt(), anyInt());
            verifyNoInteractions(mail.constructed().getFirst());
        }
    }
    @Test void loanEmailUsesDatabaseEquipmentAndRunsAfterCommit() throws Exception {
        Solicitante s = new Solicitante(); s.setIdSolicitante(3); s.setEstado(EstadoRegistro.ACTIVO); s.setCarne("A-3");
        Prestamo loan = new Prestamo(); loan.setSolicitante(s); loan.setFechaEsperadaDevolucion(LocalDateTime.now().plusDays(2));
        Equipo stored = equipment(EstadoEquipo.DISPONIBLE); stored.setNombre("Microscopio");
        Equipo idOnly = new Equipo(); idOnly.setIdEquipo(4);
        try (var loans = mockConstruction(PrestamoRepository.class, (m,c) -> when(m.insertarEncabezado(any(), any(), anyInt())).thenReturn(10));
             var equipment = mockConstruction(EquipoRepository.class, (m,c) -> {
                 when(m.buscarPorId(any(), eq(4))).thenReturn(Optional.of(stored));
                 when(m.cambiarEstado(any(), eq(4), eq(EstadoEquipo.DISPONIBLE), eq(EstadoEquipo.PRESTADO))).thenReturn(true);
             });
             var applicants = mockConstruction(SolicitanteRepository.class, (m,c) -> when(m.buscarPorId(3)).thenReturn(Optional.of(s)));
             var audits = mockConstruction(AuditoriaService.class); var mail = mockConstruction(CorreoService.class)) {
            assertEquals(10, new PrestamoService().confirmar(loan, List.of(idOnly)));
            var order = inOrder(connection, mail.constructed().getFirst());
            order.verify(connection).commit();
            order.verify(mail.constructed().getFirst()).notificarPrestamo(10, loan, List.of(stored));
            assertEquals("EQ-4", stored.getCodigoInventario());
            assertEquals("Microscopio", stored.getNombre());
        }
    }
    @Test void correctMaintenanceOfAvailableEquipmentIsRejected() throws Exception {
        Mantenimiento m = new Mantenimiento(); m.setEquipo(equipment(EstadoEquipo.DISPONIBLE));
        m.setTipo(TipoMantenimiento.CORRECTIVO); m.setResponsable("Técnico"); m.setDescripcion("Trabajo");
        try (var equipment = mockConstruction(EquipoRepository.class, (r,c) -> when(r.buscarPorId(any(), eq(4))).thenReturn(Optional.of(equipment(EstadoEquipo.DISPONIBLE))));
             var maintenance = mockConstruction(MantenimientoRepository.class); var audits = mockConstruction(AuditoriaService.class)) {
            assertThrows(BusinessException.class, () -> new MantenimientoService().enviarAMantenimiento(m));
            verifyNoInteractions(maintenance.constructed().getFirst()); verify(connection).rollback();
        }
    }
    @Test void irreparableMaintenanceMarksEquipmentRetired() throws Exception {
        Mantenimiento m = new Mantenimiento(); m.setEquipo(equipment(EstadoEquipo.MANTENIMIENTO)); m.setEstado(EstadoMantenimiento.EN_PROCESO);
        try (var equipment = mockConstruction(EquipoRepository.class, (r,c) -> when(r.cambiarEstado(any(), eq(4), eq(EstadoEquipo.MANTENIMIENTO), eq(EstadoEquipo.BAJA))).thenReturn(true));
             var maintenance = mockConstruction(MantenimientoRepository.class, (r,c) -> {
                 when(r.buscarPorId(any(), eq(1))).thenReturn(Optional.of(m));
                 when(r.finalizar(any(), eq(1), eq(ResultadoMantenimiento.NO_REPARABLE), any())).thenReturn(true);
             }); var audits = mockConstruction(AuditoriaService.class)) {
            assertEquals(EstadoEquipo.BAJA, new MantenimientoService().finalizar(1, ResultadoMantenimiento.NO_REPARABLE, "Motor destruido"));
            verify(connection).commit();
        }
    }
}
