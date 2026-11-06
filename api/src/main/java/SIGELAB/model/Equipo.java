package SIGELAB.model;

import SIGELAB.enums.EstadoEquipo;

/**
 * Activo principal (dbo.Equipo). Se relaciona con un TipoEquipo y un Laboratorio (objetos, no textos).
 */
public class Equipo {

    private int idEquipo;
    private String codigoInventario;   // es el valor que guarda el código QR
    private String nombre;
    private TipoEquipo tipoEquipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private Laboratorio laboratorio;
    private EstadoEquipo estado;
    private String observaciones;

    public Equipo() {
        this.estado = EstadoEquipo.DISPONIBLE;
    }

    public int getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(int idEquipo) {
        this.idEquipo = idEquipo;
    }

    public String getCodigoInventario() {
        return codigoInventario;
    }

    public void setCodigoInventario(String codigoInventario) {
        this.codigoInventario = codigoInventario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoEquipo getTipoEquipo() {
        return tipoEquipo;
    }

    public void setTipoEquipo(TipoEquipo tipoEquipo) {
        this.tipoEquipo = tipoEquipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public Laboratorio getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(Laboratorio laboratorio) {
        this.laboratorio = laboratorio;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipo estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    // Así se ve en los ComboBox (por ejemplo en Mantenimiento)
    @Override
    public String toString() {
        return codigoInventario + " - " + nombre + " (" + estado + ")";
    }
}
