package SIGELAB.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado genérico de un reporte: nombres de columnas + filas.
 * Permite mostrar cualquier consulta en un JTable sin crear una clase por reporte.
 */
public class ResultadoReporte {

    private final List<String> columnas;
    private final List<Object[]> filas = new ArrayList<>();

    public ResultadoReporte(List<String> columnas) {
        this.columnas = columnas;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<Object[]> getFilas() {
        return filas;
    }

    public void agregarFila(Object[] fila) {
        filas.add(fila);
    }
}
