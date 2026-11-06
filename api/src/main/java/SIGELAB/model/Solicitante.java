package SIGELAB.model;

import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.TipoSolicitante;

/**
 * Persona que recibe un préstamo (dbo.Solicitante). No necesita iniciar sesión.
 */
public class Solicitante {

    private int idSolicitante;
    private String carne;
    private String nombre;
    private String apellido;
    private TipoSolicitante tipo;
    private String correo;
    private String telefono;
    private EstadoRegistro estado;

    public Solicitante() {
        this.estado = EstadoRegistro.ACTIVO;
    }

    public int getIdSolicitante() {
        return idSolicitante;
    }

    public void setIdSolicitante(int idSolicitante) {
        this.idSolicitante = idSolicitante;
    }

    public String getCarne() {
        return carne;
    }

    public void setCarne(String carne) {
        this.carne = carne;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public TipoSolicitante getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitante tipo) {
        this.tipo = tipo;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public EstadoRegistro getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistro estado) {
        this.estado = estado;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    // Así se ve en el ComboBox de Préstamos
    @Override
    public String toString() {
        return carne + " - " + getNombreCompleto();
    }
}
