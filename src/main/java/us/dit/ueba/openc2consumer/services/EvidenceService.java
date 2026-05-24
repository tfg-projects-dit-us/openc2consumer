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
package us.dit.ueba.openc2consumer.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.model.Evidence;

/**
 * @author Isabel Román Servicio para la gestión de evidencias en memoria.
 */
@Service
public class EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);

    @Value("${artifacts.path}")
    private String artifactsPath;

    private final AtomicLong idSequence = new AtomicLong(1);

    public Optional<Evidence> findByName(String evidenceType) {
        Optional<Evidence> optevidence;

        if (evidenceType == null || evidenceType.isBlank()) {
            throw new IllegalArgumentException("El tipo de evidencia no puede estar vacío");
        }
        String filename = artifactsPath + File.separator + evidenceType.toLowerCase() + ".artifact";
        File file = new File(filename);
        if (!file.exists()) {
            optevidence = Optional.empty();
            log.warn("Evidencia no encontrada para el tipo: {}", evidenceType);
        }
        String content;
        try {
            content = Files.readString(file.toPath());
        } catch (IOException e) {
            log.error("Error leyendo el fichero de artefacto: {}", filename, e);
            throw new RuntimeException("Error leyendo el fichero de artefacto: " + filename, e);
        }
        Evidence evidence = new Evidence();
        evidence.setType(evidenceType.toLowerCase());
        evidence.setArtefact(content);
        long id = idSequence.getAndIncrement();
        evidence.setId(id);

        log.debug("Evidencia localizada con id {} y tipo {}", id, evidenceType);
        optevidence = Optional.of(evidence);
        return optevidence;
    }

    public Map<String, Evidence> findAll() {
        Map<String, Evidence> all = new ConcurrentHashMap<>();
        return all;
    }

    public boolean delete(String name) {
        return true;
    }

    public Evidence create(String type) {
        Optional<Evidence> optevidence = findByName(type);
        if (optevidence.isEmpty()) {
            throw new IllegalArgumentException("No se pudo crear la evidencia, el tipo no es válido: " + type);
        }
        return optevidence.get();
    }

}
