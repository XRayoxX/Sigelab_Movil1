package SIGELAB.model;

import SIGELAB.enums.EstadoRegistro;

/**
 * Representa una fila de la tabla dbo.Laboratorio.
 * Solo guarda datos: no se conecta a la base ni valida reglas.
 */
public class Laboratorio {

    private int idLaboratorio;
    private String codigo;
    private String nombre;
    private String ubicacion;
    private int capacidad;
    private EstadoRegistro estado;

    public Laboratorio() {
        this.estado = EstadoRegistro.ACTIVO;
    }

    public Laboratorio(int idLaboratorio, String codigo, String nombre, String ubicacion,
                       int capacidad, EstadoRegistro estado) {
        this.idLaboratorio = idLaboratorio;
        this.codigo = codigo;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
        this.estado = estado;
    }

    public int getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(int idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public EstadoRegistro getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistro estado) {
        this.estado = estado;
    }

    // Lo que se muestra cuando el laboratorio está dentro de un JComboBox (por ejemplo en Equipos)
    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
