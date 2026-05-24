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
package us.dit.ueba.openc2consumer.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import us.dit.ueba.openc2consumer.model.Evidence;
import us.dit.ueba.openc2consumer.services.EvidenceService;

/**
 * @author Isabel Román Controlador REST para la gestión de evidencias.
 */
@RestController
@RequestMapping("/evidences")
public class EvidenceController {

    private static final Logger log = LoggerFactory.getLogger(EvidenceController.class);

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping
    public ResponseEntity<Evidence> create(@RequestParam String type) {
        log.debug("Creando evidencia de tipo: {}", type);
        try {
            Evidence created = evidenceService.create(type);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("No se pudo crear la evidencia: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{name}")
    public ResponseEntity<Evidence> getByName(@PathVariable String name) {
        log.debug("Consultando evidencia con nombre: {}", name);
        Optional<Evidence> optevidence = evidenceService.findByName(name);
        if (optevidence.isPresent()) {
            return ResponseEntity.ok(optevidence.get());
        }
        log.warn("Evidencia no encontrada para el nombre: {}", name);
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Evidence>> getAll() {
        log.debug("Consultando todas las evidencias");
        return ResponseEntity.ok(evidenceService.findAll());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        log.debug("Eliminando evidencia con nombre: {}", name);
        if (evidenceService.delete(name)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
