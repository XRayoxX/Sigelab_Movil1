package SIGELAB.model;

import SIGELAB.enums.EstadoRegistro;

/**
 * Catálogo dbo.TipoEquipo (Laptop, Cámara, Proyector...). Un Equipo apunta a un TipoEquipo.
 */
public class TipoEquipo {

    private int idTipoEquipo;
    private String nombre;
    private String descripcion;
    private EstadoRegistro estado;

    public TipoEquipo() {
        this.estado = EstadoRegistro.ACTIVO;
    }

    public TipoEquipo(int idTipoEquipo, String nombre, String descripcion, EstadoRegistro estado) {
        this.idTipoEquipo = idTipoEquipo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getIdTipoEquipo() {
        return idTipoEquipo;
    }

    public void setIdTipoEquipo(int idTipoEquipo) {
        this.idTipoEquipo = idTipoEquipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoRegistro getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistro estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
