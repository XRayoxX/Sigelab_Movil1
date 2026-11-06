package gt.edu.sigelab.api;

import SIGELAB.enums.*;
import SIGELAB.model.*;
import SIGELAB.service.*;
import SIGELAB.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ModulesController {
    private final EquipoService equipos = new EquipoService();
    private final LaboratorioService laboratorios = new LaboratorioService();
    private final TipoEquipoService tipos = new TipoEquipoService();
    private final SolicitanteService solicitantes = new SolicitanteService();
    private final UsuarioService usuarios = new UsuarioService();
    private final PrestamoService prestamos = new PrestamoService();
    private final DevolucionService devoluciones = new DevolucionService();
    private final MantenimientoService mantenimientos = new MantenimientoService();
    private final ReporteService reportes = new ReporteService();

    private void operator() { require(RolUsuario.ADMIN, RolUsuario.ENCARGADO); }
    private void require(RolUsuario... roles) {
        if (!Sesion.tieneRol(roles)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Su rol no permite esta operación.");
    }
    private String blankNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private Map<String, Object> items(Object value) { return Map.of("items", value); }

    @GetMapping("/dashboard") public ResumenDashboard dashboard() { return new DashboardService().resumen(); }

    @GetMapping("/equipos") public Object equipos(@RequestParam(defaultValue="") String q,
            @RequestParam(required=false) EstadoEquipo estado, @RequestParam(required=false) Integer laboratorio) {
        operator(); return items(equipos.listar(q, estado, laboratorio));
    }
    @GetMapping("/equipos/codigo") public Equipo codigo(@RequestParam String codigo) { operator(); return equipos.identificarPorCodigo(codigo); }
    @GetMapping("/equipos/historial") public ResultadoReporte historial(@RequestParam String codigo) { operator(); return equipos.historial(codigo); }
    @PostMapping("/equipos") public Equipo guardarEquipo(@RequestBody Equipo value) {
        operator();
        if (value.getIdEquipo() > 0) equipos.actualizar(value); else equipos.registrar(value);
        return equipos.identificarPorCodigo(value.getCodigoInventario());
    }

    @GetMapping("/laboratorios") public Object laboratorios(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="false") boolean activos) {
        return items(activos ? laboratorios.listarActivos() : laboratorios.buscar(q));
    }
    @PostMapping("/laboratorios") public Laboratorio laboratorio(@RequestBody Laboratorio value) {
        require(RolUsuario.ADMIN);
        if (value.getIdLaboratorio() > 0) laboratorios.actualizar(value); else laboratorios.registrar(value);
        return value;
    }
    @GetMapping("/tipos") public Object tipos(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="false") boolean activos) {
        return items(activos ? tipos.listarActivos() : tipos.buscar(q));
    }
    @PostMapping("/tipos") public TipoEquipo tipo(@RequestBody TipoEquipo value) {
        require(RolUsuario.ADMIN);
        if (value.getIdTipoEquipo() > 0) tipos.actualizar(value); else tipos.registrar(value);
        return value;
    }
    @GetMapping("/solicitantes") public Object solicitantes(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="false") boolean activos) {
        operator(); return items(activos ? solicitantes.listarActivos() : solicitantes.buscar(q));
    }
    @PostMapping("/solicitantes") public Solicitante solicitante(@RequestBody Solicitante value) {
        operator();
        if (value.getIdSolicitante() > 0) solicitantes.actualizar(value); else solicitantes.registrar(value);
        return value;
    }
    @GetMapping("/usuarios") public Object usuarios(@RequestParam(defaultValue="") String q) {
        require(RolUsuario.ADMIN); return items(usuarios.buscar(q));
    }
    record UsuarioInput(Usuario usuario, String password, String confirmation) {}
    @PostMapping("/usuarios") public Usuario usuario(@RequestBody UsuarioInput input) {
        require(RolUsuario.ADMIN);
        if (input.usuario() == null) throw new BusinessException("Ingrese los datos del usuario.");
        if (input.usuario().getIdUsuario() > 0) {
            Usuario previous = new SIGELAB.repository.UsuarioRepository().buscarPorId(input.usuario().getIdUsuario())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
            input.usuario().setNombreUsuario(previous.getNombreUsuario());
            input.usuario().setRutaFotoRostro(previous.getRutaFotoRostro());
            usuarios.actualizar(input.usuario(), input.password(), input.confirmation());
        } else usuarios.registrar(input.usuario(), input.password(), input.confirmation());
        return input.usuario();
    }
    @PostMapping("/{module}/{id}/desactivar") public Object desactivar(@PathVariable String module, @PathVariable int id) {
        switch (module) {
            case "laboratorios" -> { require(RolUsuario.ADMIN); laboratorios.desactivar(id); }
            case "tipos" -> { require(RolUsuario.ADMIN); tipos.desactivar(id); }
            case "usuarios" -> { require(RolUsuario.ADMIN); usuarios.desactivar(id); }
            case "solicitantes" -> { operator(); solicitantes.desactivar(id); }
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Map.of("ok", true);
    }

    @GetMapping("/prestamos") public Object prestamos(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="TODOS") String estado) {
        operator(); return items(prestamos.listar(q, estado));
    }
    @GetMapping("/prestamos/{id}/detalles") public Object detalles(@PathVariable int id) { operator(); return items(prestamos.listarDetalles(id)); }
    record PrestamoInput(int solicitanteId, LocalDateTime fechaEsperadaDevolucion, String observaciones, List<Integer> equipos) {}
    @PostMapping("/prestamos") public Object prestamo(@RequestBody PrestamoInput input) {
        operator();
        if (input.fechaEsperadaDevolucion() == null || !input.fechaEsperadaDevolucion().isAfter(LocalDateTime.now(ZoneId.of("America/Guatemala"))))
            throw new BusinessException("La devolución debe ser posterior a la fecha y hora actual de Guatemala.");
        Prestamo value = new Prestamo();
        Solicitante s = new Solicitante(); s.setIdSolicitante(input.solicitanteId()); value.setSolicitante(s);
        value.setFechaEsperadaDevolucion(input.fechaEsperadaDevolucion()); value.setObservaciones(input.observaciones());
        List<Equipo> selected = input.equipos() == null ? List.of() : input.equipos().stream().map(id -> { Equipo e = new Equipo(); e.setIdEquipo(id); return e; }).toList();
        if (selected.size() > 100) throw new BusinessException("Máximo 100 equipos por préstamo.");
        return Map.of("idPrestamo", prestamos.confirmar(value, selected));
    }
    @GetMapping("/devoluciones") public Object devoluciones(@RequestParam(defaultValue="") String q) { operator(); return items(devoluciones.prestamosActivos(q)); }
    record DevolucionInput(int detalleId, CondicionDevolucion condicion, String observaciones) {}
    @PostMapping("/devoluciones") public Object devolver(@RequestBody DevolucionInput input) {
        operator(); return Map.of("pendientes", devoluciones.registrar(input.detalleId(), input.condicion(), input.observaciones()));
    }

    @GetMapping("/mantenimiento") public Object mantenimiento(@RequestParam(defaultValue="") String q, @RequestParam(required=false) EstadoMantenimiento estado) {
        operator(); return items(mantenimientos.listar(q, estado));
    }
    @GetMapping("/mantenimiento/equipos") public Object equiposMantenimiento() { operator(); return items(mantenimientos.equiposParaMantenimiento()); }
    @PostMapping("/mantenimiento") public Object mantenimiento(@RequestBody Mantenimiento value) { operator(); return Map.of("idMantenimiento", mantenimientos.enviarAMantenimiento(value)); }
    record CierreInput(ResultadoMantenimiento resultado, String observaciones) {}
    @PostMapping("/mantenimiento/{id}/finalizar") public Object finalizar(@PathVariable int id, @RequestBody CierreInput input) {
        operator(); return Map.of("estado", mantenimientos.finalizar(id, input.resultado(), input.observaciones()));
    }

    @GetMapping("/reportes") public ResultadoReporte reporte(@RequestParam String tipo,
            @RequestParam(required=false) Integer laboratorio, @RequestParam(defaultValue="") String codigo,
            @RequestParam(defaultValue="") String estado, @RequestParam(defaultValue="") String desde, @RequestParam(defaultValue="") String hasta) {
        return reportes.generar(tipo, laboratorio, codigo, estado, desde, hasta);
    }
    @GetMapping("/auditoria") public Object auditoria(@RequestParam(defaultValue="") String usuario,
            @RequestParam(defaultValue="") String entidad, @RequestParam(defaultValue="") String accion,
            @RequestParam(defaultValue="") String desde, @RequestParam(defaultValue="") String hasta) {
        require(RolUsuario.ADMIN);
        return items(new AuditoriaService().consultar(blankNull(usuario), blankNull(entidad), blankNull(accion), Fechas.leerOpcional(desde, "fecha desde"), Fechas.leerOpcional(hasta, "fecha hasta")));
    }
}

