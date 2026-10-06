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
package us.dit.ueba.openc2consumer.integrations.velociraptor.vql;

import java.util.Iterator;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.integrations.velociraptor.config.EvidenceTypes;
import us.dit.ueba.openc2consumer.integrations.velociraptor.config.VigilanceLevels;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.Api.VQLResponse;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.VqlApiGrpc;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.VqlApiGrpc.VqlApiBlockingStub;

/**
 * Prepara y envía las consultas VQL para registrar artefactos, iniciar su monitorización
 * y añadir o retirar usuarios. Usa VqlQueries para obtener cada consulta,
 * ArgsBuilder para preparar la petición y el cliente gRPC para enviarla.
 * Los tipos de evidencia, niveles de vigilancia y carpeta de archivos se reciben
 * mediante la configuración de Spring.
 * @author Isabel Roman
 */
@Service("vqlService")

public class VqlService implements VqlInterface {

    private final VqlApiBlockingStub blockingStub;
    private final String artifactsPath;
    private final EvidenceTypes evidenceTypes;
    private final VigilanceLevels vigilanceLevels;
    private static final Logger log = LoggerFactory.getLogger(VqlService.class);

    /** Recibe el cliente de conexión y la configuración que necesitan las consultas. */
    public VqlService(VqlApiBlockingStub blockingStub,
            @Value("${artifacts.path}") String artifactsPath,
            EvidenceTypes evidenceTypes, VigilanceLevels vigilanceLevels) {
        this.blockingStub = blockingStub;
        this.artifactsPath = artifactsPath;
        this.evidenceTypes = evidenceTypes;
        this.vigilanceLevels = vigilanceLevels;
    }

    /** Devuelve la descripción del servicio gRPC definida en el protocolo. */
    public String getServiceDescriptor() {
        return VqlApiGrpc.getServiceDescriptor().toString();
    }

    /**
     * Lee la consulta del archivo del tipo de evidencia y la envía para registrar el artefacto.
     * Rechaza los tipos que no aparecen en ueba.evidences.
     */
    public void sendNewArtefact(String evidenceType) {
        String type = validatedEvidenceType(evidenceType);

        String name = type + "_openc2_soar_new_artefact";
        try {
            String query = VqlQueries.newArtifact(type, artifactsPath);
            VQLCollectorArgs args = new ArgsBuilder(query)
                    .setName(name)
                    .buildArgs();
            send(args);

            log.info("Petición de registro de artefacto completada");

        } catch (Exception e) {
            throw operationError("Error al registrar el artefacto por gRPC", e);
        }
    }

    /** Envía la consulta de inicio para el artefacto UEBA.SOAR.<tipo de evidencia>. */
    public void startMonitoring(String evidenceType) {
        String type = validatedEvidenceType(evidenceType);
        String name = type + "_openc2_soar_start_monitoring";

        try {
            // Obtener el texto de la consulta de inicio.
            String query = VqlQueries.startMonitoring();
            String artifactName = "UEBA.SOAR." + type; // Nombre del artefacto que recibirá la consulta.
            VQLCollectorArgs args = new ArgsBuilder(query)
                    .setName(name)
                    .setVariable("ArtifactName", artifactName)
                    .buildArgs();
            send(args);

            log.info("Starting Evidence Catch in Velociraptor!");

        } catch (Exception e) {
            throw operationError("Error in gRPC starting evidence catch", e);
        }
    }

    /** Añade el usuario con nivel STANDARD llamando a la versión de tres parámetros. */
    public void addUser(String evidenceType, String username) {
        addUser(evidenceType, username, "STANDARD");
    }

    /**
     * Envía la consulta para añadir el usuario a la tabla Server.<tipo de evidencia>.
     * Si el nivel recibido no está configurado, utiliza STANDARD.
     */
    public void addUser(String evidenceType, String username, String vigilanceLevel) {
        String type = validatedEvidenceType(evidenceType);
        // Sustituir un nivel no reconocido por STANDARD.
        if (!isValidVigilanceLevel(vigilanceLevel)) {
            log.error("Invalid vigilance level: " + vigilanceLevel + " it will be set to STANDARD");
            vigilanceLevel = "STANDARD";
        }
        String selectedLevel = vigilanceLevel.toUpperCase(Locale.ROOT);
        String name = type + "_openc2_soar_add_user";

        try {
            String query = VqlQueries.addUser();
            String tableName = "Server." + type; // Tabla asociada al tipo de evidencia.
            VQLCollectorArgs args = new ArgsBuilder(query)
                    .setName(name)
                    .setVariable("TableName", tableName)
                    .setVariable("TargetUser", username)
                    .setVariable("TargetLevel", selectedLevel)
                    .buildArgs();
            send(args);

            log.info("Adding user {} to {} monitoring in Velociraptor!", username, evidenceType);

        } catch (Exception e) {
            throw operationError("Error executing user operation by gRPC", e);
        }
    }

    /** Envía la consulta para retirar el usuario de la tabla Server.<tipo de evidencia>. */
    public void deleteUser(String evidenceType, String username) {
        String type = validatedEvidenceType(evidenceType);
        String name = type + "_openc2_soar_delete_user";

        try {
            String query = VqlQueries.deleteUser();
            String tableName = "Server." + type; // Tabla asociada al tipo de evidencia.
            VQLCollectorArgs args = new ArgsBuilder(query)
                    .setName(name)
                    .setVariable("TableName", tableName)
                    .setVariable("TargetUser", username)
                    .buildArgs();
            send(args);

            log.info("Deleting user {} from {} monitoring in Velociraptor!", username, evidenceType);

        } catch (Exception e) {
            throw operationError("Error executing user operation by gRPC", e);
        }
    }

    /** Comprueba el tipo y devuelve su nombre en minúsculas para usarlo en la petición. */
    private String validatedEvidenceType(String evidenceType) {
        if (!isValidEvidenceType(evidenceType)) {
            throw new IllegalArgumentException("Invalid evidence type: " + evidenceType);
        }
        return evidenceType.toLowerCase(Locale.ROOT);
    }

    /** Registra el fallo y crea una excepción que conserva su causa. */
    private RuntimeException operationError(String message, Exception cause) {
        log.error(message, cause);
        return new RuntimeException(message, cause);
    }

    /** Envía la petición y recorre todas las respuestas del servidor. */
    private void send(VQLCollectorArgs args) {
        log.debug("Enviando petición VQL: {}", args);
        logServerResponses(blockingStub.query(args));
    }
    /**
     * Recorre todas las respuestas y escribe sus mensajes log en el registro de la aplicación.
     * Los errores gRPC al recorrer las respuestas se propagan al método que lo ha llamado.
     */
    private void logServerResponses(Iterator<VQLResponse> responseStream) {
        while (responseStream.hasNext()) {
            VQLResponse response = responseStream.next();
            if (response.getLog() != null && !response.getLog().isEmpty()) {
                log.info("Log del servidor: " + response.getLog());
            }
        }
    }
    /** Comprueba si el tipo está configurado, sin distinguir mayúsculas de minúsculas. */
    boolean isValidEvidenceType(String evidenceType) {
        return evidenceType != null && evidenceTypes.getEvidences().contains(evidenceType.toLowerCase(Locale.ROOT));
    }

    /** Comprueba si el nivel está configurado, sin distinguir mayúsculas de minúsculas. */
    boolean isValidVigilanceLevel(String vigilanceLevel) {
        return vigilanceLevel != null && vigilanceLevels.getVigilanceLevels().contains(vigilanceLevel.toUpperCase(Locale.ROOT));
    }
}
