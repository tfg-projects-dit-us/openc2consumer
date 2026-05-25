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
 * This file is part of OpenC2Consumer, a SOAR4FUEBA component
 */
package us.dit.ueba.openc2consumer.controllers;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.targets.Target;
import org.oasis.openc2.lycan.args.Args;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import us.dit.ueba.openc2consumer.services.vql.VqlInterface;

/**
 * Controlador REST que actúa como consumidor OpenC2. Recibe comandos OpenC2 en
 * formato JSON y los traduce en operaciones VQL sobre Velociraptor.
 *
 * Endpoint principal: POST /openc2/command
 *
 * Acciones soportadas: - start + target.evidence_type → sendNewArtefact +
 * startMonitoring - add + target.evidence_type + target.username [+
 * args.vigilance_level] → addUser - delete + target.evidence_type +
 * target.username → deleteUser
 *
 * Ejemplo de cuerpo JSON para añadir un usuario:
 * <pre>
 * {
 *   "action": "add",
 *   "target": {
 *     "evidence_type": "userlogon",
 *     "username": "jdoe"
 *   },
 *   "args": {
 *     "vigilance_level": "HIGH"
 *   }
 * }
 * </pre>
 */
@RestController
@RequestMapping("/openc2")
public class OpenC2Controller {

    private static final Logger log = LoggerFactory.getLogger(OpenC2Controller.class);

    @Autowired
    private VqlInterface vqlService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(value = "/command", consumes = "application/openc2+json;version=1.0")
    public ResponseEntity<String> receiveCommand(@RequestBody String rawJson) {
        try {
            // 1. Validar e interpretar el Core de OpenC2 usando Lycan
            OpenC2Message openC2Command = objectMapper.readValue(rawJson, OpenC2Message.class);

            // 2. Extraer los argumentos propietarios de forma segura
            JsonNode rootNode = objectMapper.readTree(rawJson);
            JsonNode argsNode = rootNode.path("args");

            String vigilance = argsNode.path("x-ueba-vigilance").asText("STANDARD");
            int ocsfClass = argsNode.path("x-ocsf-class").asInt(3001);

            // 3. Extraer el Target (ej. el nombre de usuario "pepito")
            // Nota: Adapta este método según cómo exponga Lycan el Target de la cuenta
            String username = openC2Command.getTarget().getUserAccount().getUsername();

            // 4. Ejecutar la acción en Velociraptor a través de gRPC
            velociraptorService.updateVigilanceTable(username, vigilance);

            // 5. Responder al SOAR siguiendo el estándar OpenC2 Response
            String openC2Response = "{\"status\": 200, \"status_text\": \"Command executed successfully\"}";
            return ResponseEntity.ok()
                    .header("Content-Type", "application/openc2+json;version=1.0")
                    .body(openC2Response);

        } catch (Exception e) {
            // Si algo falla, respondemos con la estructura de error de OpenC2
            String errorResponse = String.format("{\"status\": 400, \"status_text\": \"Bad Request: %s\"}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/openc2+json;version=1.0")
                    .body(errorResponse);
        }
    }

    public void procesarComando(String jsonCrudo) throws Exception {
        // 1. Dejas que Lycan haga su magia con la estructura core (Action, Target...)
        OpenC2Message command = mapper.readValue(jsonCrudo, OpenC2Message.class);

        // 2. Para tus argumentos propietarios, navegas el JSON de forma genérica
        JsonNode rootNode = mapper.readTree(jsonCrudo);
        JsonNode argsNode = rootNode.path("args");

        if (!argsNode.isMissingNode()) {
            // Extraes tus propiedades personalizadas de forma directa y segura
            String vigilance = argsNode.path("x-ueba-vigilance").asText("STANDARD");
            int ocsfClass = argsNode.path("x-ocsf-class").asInt(3001);

            // Ya tienes tus datos listos para armar el VQL de Velociraptor
            System.out.println("Vigilancia: " + vigilance);
            System.out.println("Clase OCSF: " + ocsfClass);
        }
    }
}
