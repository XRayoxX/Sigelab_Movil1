package SIGELAB.service;

import SIGELAB.model.ResumenDashboard;
import SIGELAB.repository.ReporteRepository;

/**
 * Contadores del formulario principal (sección 6.11), obtenidos de la base de datos.
 */
public class DashboardService {

    private final ReporteRepository repository = new ReporteRepository();

    public ResumenDashboard resumen() {
        return repository.resumenDashboard();
    }
}
