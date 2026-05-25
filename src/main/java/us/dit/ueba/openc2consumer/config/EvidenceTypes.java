/*
 * Copyright 2026 Universidad de Sevilla/Departamento de Ingeniería Telemática
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is part of OpenC2Consumer, a SOAR4FUEBA (SOAR solution based on BPM paradigm) component
 */
package us.dit.ueba.openc2consumer.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ueba")
/**
 * @author Isabel Román Esta clase se encarga de mapear la propiedad
 * "ueba.evidences" del application.properties a una lista de strings. Spring
 * Boot se encargará de inyectar el valor de "ueba.evidences" en esta clase, y
 * luego podrás acceder a esa lista desde VqlService para construir consultas
 * vql
 */
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
