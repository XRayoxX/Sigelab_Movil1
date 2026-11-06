package SIGELAB.model;

import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.enums.EstadoDetalle;
import SIGELAB.enums.EstadoPrestamo;

import java.time.LocalDateTime;

/**
 * Una línea del préstamo: QUÉ equipo se prestó y si ya regresó (dbo.DetallePrestamo).
 * Si ya fue devuelto, también trae los datos de su Devolucion.
 */
public class DetallePrestamo {

    private int idDetallePrestamo;
    private int idPrestamo;
    private EstadoPrestamo estadoPrestamo;
    private Equipo equipo;
    private EstadoDetalle estado;

    // Datos de dbo.Devolucion (null mientras esté PENDIENTE)
    private CondicionDevolucion condicion;
    private LocalDateTime fechaDevolucion;
    private String observacionDevolucion;

    public int getIdDetallePrestamo() {
        return idDetallePrestamo;
    }

    public void setIdDetallePrestamo(int idDetallePrestamo) {
        this.idDetallePrestamo = idDetallePrestamo;
    }

    public int getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(int idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public EstadoPrestamo getEstadoPrestamo() {
        return estadoPrestamo;
    }

    public void setEstadoPrestamo(EstadoPrestamo estadoPrestamo) {
        this.estadoPrestamo = estadoPrestamo;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public void setEquipo(Equipo equipo) {
        this.equipo = equipo;
    }

    public EstadoDetalle getEstado() {
        return estado;
    }

    public void setEstado(EstadoDetalle estado) {
        this.estado = estado;
    }

    public CondicionDevolucion getCondicion() {
        return condicion;
    }

    public void setCondicion(CondicionDevolucion condicion) {
        this.condicion = condicion;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public String getObservacionDevolucion() {
        return observacionDevolucion;
    }

    public void setObservacionDevolucion(String observacionDevolucion) {
        this.observacionDevolucion = observacionDevolucion;
    }
}
