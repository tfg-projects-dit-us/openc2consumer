package us.dit.ueba.openc2consumer.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ueba.user")
public class VigilanceLevels {

    /**
     * Esta clase se encarga de mapear la propiedad "ueba.user.vigilancelevels"
     * del application.properties a una lista de strings. Spring Boot se
     * encargará de inyectar el valor de "ueba.user.vigilancelevels" en esta
     * clase, y luego podrás acceder a esa lista desde VqlService para construir
     * consultas vql
     */

    private Logger log = LoggerFactory.getLogger(VigilanceLevels.class);

    // Spring convertirá "userlogon,usersession" en esta lista
    private List<String> vigilanceLevels;

    // 1. Constructor vacío (Obligatorio para que Spring pueda instanciar la clase)
    public VigilanceLevels() {
    }

    // 2. Getter (Para que tu VqlService pueda hacer .getVigilanceLevels())
    public List<String> getVigilanceLevels() {
        return this.vigilanceLevels;
    }

    // 3. Setter con transformación a minúsculas(Obligatorio para que Spring Boot inyecte los datos del application.properties)
    public void setVigilanceLevels(List<String> vigilanceLevels) {
        if (vigilanceLevels == null) {
            this.vigilanceLevels = null;
            return;
        }

        // Creamos una nueva lista para almacenar los valores normalizados
        List<String> uppercaseVigilanceLevels = new ArrayList<>();

        for (String vigilanceLevel : vigilanceLevels) {
            if (vigilanceLevel != null) {
                // Pasamos a mayúsculas y eliminamos espacios en blanco accidentales
                uppercaseVigilanceLevels.add(vigilanceLevel.toUpperCase().trim());
            }
        }
        log.debug("Vigilance levels after normalization: {}", uppercaseVigilanceLevels);
        this.vigilanceLevels = uppercaseVigilanceLevels;
    }
}
