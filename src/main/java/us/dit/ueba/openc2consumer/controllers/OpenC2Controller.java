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



import us.dit.ueba.openc2consumer.services.vql.VqlInterface;
import us.dit.ueba.openc2consumer.services.ThreatHuntingService;

/**
 * Recibe comandos OpenC2 en JSON mediante POST /openc2/command
 * y devuelve las respuestas preparadas por ThreatHuntingService.
 *
 * Estado actual del perfil Threat Hunting (sin llamadas a Velociraptor):
 * - query features: acepta únicamente ["pairs"], sin args ni actuator.
 * - query th.huntflows: acepta {} y devuelve la definición estática de userlogon.
 * - query th.datasources: acepta "" y devuelve la fuente estática endpoint_logs.
 * - investigate th.hunt: valida userlogon y sus argumentos, pero no lo ejecuta.
 * Las consultas devuelven 200; las peticiones inválidas, 400; las opciones no
 * implementadas y las investigaciones válidas pendientes de ejecución, 501.
 *
 * Los demás targets siguen la ruta anterior: se deserializan con Lycan y se
 * llama a addUser() con target.user_account.username, target.evidence_type
 * (userlogon por defecto) y args.x-ueba-vigilance (STANDARD por defecto).
 * Esta ruta no selecciona operaciones según action ni registra o inicia artefactos.
 * x-ocsf-class se lee, pero no se utiliza. La adaptación de estas operaciones
 * y de deleteUser() al perfil Threat Hunting sigue pendiente.
 *
 * userlogon exige username no vacío y admite vigilance_level opcional:
 * STANDARD (por defecto), SUSPICIOUS o CRITICAL. Se rechazan parámetros
 * desconocidos, duplicados o mal formados. Otros huntflows, timeranges,
 * datasources, native_oc2 y actuator no están implementados.
 *
 * Ejemplo de userlogon que supera la validación y devuelve 501 (sin ejecución).
 * El formato nombre=valor en string_args es una convención de este huntflow:
 * <pre>
 * {
 *   "action": "investigate",
 *   "target": {
 *     "th": {
 *       "hunt": "userlogon"
 *     }
 *   },
 *   "args": {
 *     "th": {
 *       "huntargs": {
 *         "string_args": [
 *           "username=jdoe",
 *           "vigilance_level=SUSPICIOUS"
 *         ]
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * El estado de la respuesta del servicio se utiliza también como estado HTTP.
 * investigate no se anuncia en features.pairs mientras su ejecución esté pendiente.
 */
@RestController
@RequestMapping("/openc2")
public class OpenC2Controller {

    private static final Logger log = LoggerFactory.getLogger(OpenC2Controller.class);

    @Autowired
    private VqlInterface vqlService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ThreatHuntingService threatHuntingService;

    @PostMapping(value = "/command", consumes = "application/openc2+json;version=1.0")
    public ResponseEntity<String> receiveCommand(@RequestBody String rawJson) {
        try {
            // El servicio atiende consultas del perfil y valida investigate sin ejecutarlo.
            JsonNode rootNode = objectMapper.readTree(rawJson);
            if (threatHuntingService.supports(rootNode)) {
                var response = threatHuntingService.handle(rootNode);
                return ResponseEntity.status(response.path("status").asInt())
                        .header("Content-Type", "application/openc2+json;version=1.0")
                        .body(response.toString());
            }

            // 1. Deserializar con Lycan los comandos que no atiende ThreatHuntingService.
            OpenC2Message openC2Command = objectMapper.readValue(rawJson, OpenC2Message.class);

            // 2. Leer vigilancia y clase OCSF; la clase no se utiliza en la operación VQL.

            JsonNode argsNode = rootNode.path("args");

            String vigilance = argsNode.path("x-ueba-vigilance").asText("STANDARD");
            int ocsfClass = argsNode.path("x-ocsf-class").asInt(3001);

            // 3. Leer el tipo de evidencia y el usuario de la ruta anterior.
            JsonNode targetNode = rootNode.path("target");
            String evidenceType = targetNode.path("evidence_type").asText(null);
            String username = targetNode.path("user_account").path("username").asText(null);

            if (username == null || username.isEmpty()) {
                // Fallback: intentar leer desde el objeto Lycan si la estructura lo soporta
                try {
                    JsonNode lycanTarget = objectMapper.valueToTree(openC2Command.getTarget());
                    username = lycanTarget.path("username").asText(username);
                } catch (Exception ex) {
                    // Se conserva el valor anterior de username si Lycan no permite extraerlo.
                }
            }

            // 4. Solicitar el alta del usuario, sin seleccionar la operación según action.
            if (evidenceType == null || evidenceType.isEmpty()) {
                evidenceType = "userlogon"; // tipo de evidencia por defecto
            }
            if (username != null && !username.isEmpty()) {
                vqlService.addUser(evidenceType, username, vigilance);
            } else {
                throw new IllegalArgumentException("Username not found in OpenC2 target");
            }

            // 5. Responder 200 si addUser retorna; sus errores gRPC se registran internamente.
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
        // Método auxiliar sin endpoint: deserializa el Core con Lycan.
        OpenC2Message command = objectMapper.readValue(jsonCrudo, OpenC2Message.class);

        // Lee los argumentos propietarios directamente del JSON.
        JsonNode rootNode = objectMapper.readTree(jsonCrudo);
        JsonNode argsNode = rootNode.path("args");

        if (!argsNode.isMissingNode()) {
            // Aplica valores por defecto si faltan las propiedades.
            String vigilance = argsNode.path("x-ueba-vigilance").asText("STANDARD");
            int ocsfClass = argsNode.path("x-ocsf-class").asInt(3001);

            // Solo imprime los valores; no construye ni envía consultas VQL.
            System.out.println("Vigilancia: " + vigilance);
            System.out.println("Clase OCSF: " + ocsfClass);
        }
    }
}
