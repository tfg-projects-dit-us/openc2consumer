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
 * Guarda los niveles de vigilancia indicados en ueba.user.vigilancelevels.
 * Convierte los nombres a mayúsculas y quita los espacios de los extremos.
 * VqlService consulta esta lista al añadir un usuario a monitorizar.
 * @author Isabel Román
 */
@Configuration
@ConfigurationProperties(prefix = "ueba.user")
public class VigilanceLevels {

    private Logger log = LoggerFactory.getLogger(VigilanceLevels.class);

    // Spring carga aquí los niveles separados por comas en la configuración.
    private List<String> vigilanceLevels;

    public VigilanceLevels() {
    }

    /** Devuelve la lista de niveles de vigilancia configurados. */
    public List<String> getVigilanceLevels() {
        return this.vigilanceLevels;
    }

    /** Guarda los niveles en mayúsculas y sin espacios en los extremos; omite los valores null. */
    public void setVigilanceLevels(List<String> vigilanceLevels) {
        // Si no se recibe una lista, guardar null.
        if (vigilanceLevels == null) {
            this.vigilanceLevels = null;
            return;
        }

        List<String> uppercaseVigilanceLevels = new ArrayList<>();

        for (String vigilanceLevel : vigilanceLevels) {
            if (vigilanceLevel != null) {
                uppercaseVigilanceLevels.add(vigilanceLevel.toUpperCase().trim());
            }
        }
        log.debug("Vigilance levels after normalization: {}", uppercaseVigilanceLevels);
        this.vigilanceLevels = uppercaseVigilanceLevels;
    }
}
