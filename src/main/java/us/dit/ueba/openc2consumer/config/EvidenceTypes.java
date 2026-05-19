package us.dit.ueba.openc2consumer.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ueba")
public class EvidenceTypes {

    private Logger log = LoggerFactory.getLogger(EvidenceTypes.class);

    // Spring convertirá "userlogon,usersession" en esta lista
    private List<String> evidences;

    // 1. Constructor vacío (Obligatorio para que Spring pueda instanciar la clase)
    public EvidenceTypes() {
    }

    // 2. Getter (Para que tu VqlService pueda hacer .getEvidences())
    public List<String> getEvidences() {
        return this.evidences;
    }

    // 3. Setter con transformación a minúsculas(Obligatorio para que Spring Boot inyecte los datos del application.properties)
    public void setEvidences(List<String> evidences) {
        if (evidences == null) {
            this.evidences = null;
            return;
        }

        // Creamos una nueva lista para almacenar los valores normalizados
        List<String> lowercaseEvidences = new ArrayList<>();

        for (String evidence : evidences) {
            if (evidence != null) {
                // Pasamos a minúsculas y eliminamos espacios en blanco accidentales
                lowercaseEvidences.add(evidence.toLowerCase().trim());
            }
        }
        log.debug("Evidences after normalization: {}", lowercaseEvidences);
        this.evidences = lowercaseEvidences;
    }
}
