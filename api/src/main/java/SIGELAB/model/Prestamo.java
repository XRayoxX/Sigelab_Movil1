package SIGELAB.model;

import SIGELAB.enums.EstadoPrestamo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encabezado del préstamo (dbo.Prestamo). Los equipos van en la lista de detalles:
 * Prestamo 1 ---- N DetallePrestamo N ---- 1 Equipo.
 */
public class Prestamo {

    private int idPrestamo;
    private Solicitante solicitante;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaEsperadaDevolucion;
    private LocalDateTime fechaFinalizacion;
    private String usuarioRegistra;
    private EstadoPrestamo estado;
    private String observaciones;
    private final List<DetallePrestamo> detalles = new ArrayList<>();

    // Datos calculados por la vista vw_PrestamoResumen
    private int totalEquipos;
    private int equiposPendientes;
    private boolean vencido;

    public Prestamo() {
        this.estado = EstadoPrestamo.ACTIVO;
    }

    public int getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(int idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Solicitante getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Solicitante solicitante) {
        this.solicitante = solicitante;
    }

    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDateTime getFechaEsperadaDevolucion() {
        return fechaEsperadaDevolucion;
    }

    public void setFechaEsperadaDevolucion(LocalDateTime fechaEsperadaDevolucion) {
        this.fechaEsperadaDevolucion = fechaEsperadaDevolucion;
    }

    public LocalDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(LocalDateTime fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public String getUsuarioRegistra() {
        return usuarioRegistra;
    }

    public void setUsuarioRegistra(String usuarioRegistra) {
        this.usuarioRegistra = usuarioRegistra;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<DetallePrestamo> getDetalles() {
        return detalles;
    }

    public int getTotalEquipos() {
        return totalEquipos;
    }

    public void setTotalEquipos(int totalEquipos) {
        this.totalEquipos = totalEquipos;
    }

    public int getEquiposPendientes() {
        return equiposPendientes;
    }

    public void setEquiposPendientes(int equiposPendientes) {
        this.equiposPendientes = equiposPendientes;
    }

    public boolean isVencido() {
        return vencido;
    }

    public void setVencido(boolean vencido) {
        this.vencido = vencido;
    }
}
