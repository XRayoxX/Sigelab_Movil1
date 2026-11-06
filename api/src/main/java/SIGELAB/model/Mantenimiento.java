package SIGELAB.model;

import SIGELAB.enums.EstadoMantenimiento;
import SIGELAB.enums.ResultadoMantenimiento;
import SIGELAB.enums.TipoMantenimiento;

import java.time.LocalDateTime;

/**
 * Intervención de un equipo (dbo.Mantenimiento). Equipo 1 ---- N Mantenimiento.
 */
public class Mantenimiento {

    private int idMantenimiento;
    private Equipo equipo;
    private LocalDateTime fechaIngreso;
    private TipoMantenimiento tipo;
    private String descripcion;
    private String responsable;
    private LocalDateTime fechaSalida;
    private ResultadoMantenimiento resultado;
    private EstadoMantenimiento estado;
    private String observaciones;

    public Mantenimiento() {
        this.estado = EstadoMantenimiento.EN_PROCESO;
    }

    public int getIdMantenimiento() {
        return idMantenimiento;
    }

    public void setIdMantenimiento(int idMantenimiento) {
        this.idMantenimiento = idMantenimiento;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public void setEquipo(Equipo equipo) {
        this.equipo = equipo;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public TipoMantenimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMantenimiento tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public LocalDateTime getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public ResultadoMantenimiento getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoMantenimiento resultado) {
        this.resultado = resultado;
    }

    public EstadoMantenimiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoMantenimiento estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
