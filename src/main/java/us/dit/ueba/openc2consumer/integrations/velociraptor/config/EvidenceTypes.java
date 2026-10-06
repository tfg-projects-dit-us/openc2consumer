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
package us.dit.ueba.openc2consumer.integrations.velociraptor.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Guarda los tipos de evidencia indicados en ueba.evidences de application.properties.
 * Convierte los nombres a minúsculas y quita los espacios de los extremos.
 * VqlService consulta esta lista para comprobar si un tipo de evidencia está permitido.
 * @author Isabel Román
 */
@Configuration
@ConfigurationProperties(prefix = "ueba")
public class EvidenceTypes {

    private Logger log = LoggerFactory.getLogger(EvidenceTypes.class);

    // Spring carga aquí los nombres separados por comas en ueba.evidences.
    private List<String> evidences;

    public EvidenceTypes() {
    }

    /** Devuelve la lista de tipos de evidencia configurados. */
    public List<String> getEvidences() {
        return this.evidences;
    }

    /** Guarda los nombres en minúsculas y sin espacios en los extremos; omite los valores null. */
    public void setEvidences(List<String> evidences) {
        // Si no se recibe una lista, conservar null.
        if (evidences == null) {
            this.evidences = null;
            return;
        }

        List<String> lowercaseEvidences = new ArrayList<>();

        for (String evidence : evidences) {
            if (evidence != null) {
                lowercaseEvidences.add(evidence.toLowerCase().trim());
            }
        }
        log.debug("Evidences after normalization: {}", lowercaseEvidences);
        this.evidences = lowercaseEvidences;
    }
}
