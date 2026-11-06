package gt.edu.sigelab.api;

import SIGELAB.util.Configuracion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Permite al servicio de correo leer variables de Azure o la configuración privada local. */
@Component
public class ServerConfiguration {
    public ServerConfiguration(Environment environment) { Configuracion.configurar(environment::getProperty); }
}
