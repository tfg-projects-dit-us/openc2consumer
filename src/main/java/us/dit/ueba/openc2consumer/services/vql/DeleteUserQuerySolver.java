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
package us.dit.ueba.openc2consumer.services.vql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dit.ueba.openc2consumer.services.vql.VqlInterface.EvidenceType;

public class DeleteUserQuerySolver implements QuerySolver {

    EvidenceType evidenceType;
    String artifactsPath;
    private final static Logger log = LoggerFactory.getLogger(DeleteUserQuerySolver.class);

    DeleteUserQuerySolver(EvidenceType evidenceType, String artifactsPath) {
        this.evidenceType = evidenceType;
        this.artifactsPath = artifactsPath;
    }

    @Override
    public String getQuery() {
        log.debug("Obteniendo la consulta VQL");
        String filename = artifactsPath + File.separator + evidenceType.toString().toLowerCase() + ".delete";
        log.debug("Archivo de consulta VQL: " + filename);
        File file = new File(filename);
        String query = null;
        if (!file.exists()) {
            log.error("Archivo de consulta no encontrado: " + filename);
            throw new IllegalArgumentException("Archivo de consulta no encontrado: " + filename);
        }
        try {
            query = Files.readString(file.toPath());
        } catch (IOException e) {
            log.error("Error reading query file: " + filename, e);
            throw new IllegalArgumentException("Error reading query file: " + filename, e);
        }
        return query;
    }

}
