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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dit.ueba.openc2consumer.actuators.Actuator;

/**
 * Recibe comandos OpenC2 en JSON mediante POST /openc2/command
 * y devuelve las respuestas preparadas por ThreatHuntingService.
 *
 * Estado actual del perfil Threat Hunting (sin llamadas a Velociraptor):
 * - query features: acepta únicamente ["pairs"], sin args ni actuator.
 * - query th.huntflows: acepta {} y devuelve la definición estática de
 * userlogon.
 * - query th.datasources: acepta "" y devuelve la fuente estática
 * endpoint_logs.
 * - investigate th.hunt: valida userlogon y sus argumentos, pero no lo ejecuta.
 * Las consultas devuelven 200; las peticiones inválidas, 400; las opciones no
 * implementadas y las investigaciones válidas pendientes de ejecución, 501.
 *
 * Los demás targets siguen la ruta anterior: se deserializan con Lycan y se
 * llama a addUser() con target.user_account.username, target.evidence_type
 * (userlogon por defecto) y args.x-ueba-vigilance (STANDARD por defecto).
 * Esta ruta no selecciona operaciones según action ni registra o inicia
 * artefactos.
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
 * 
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
 * investigate no se anuncia en features.pairs mientras su ejecución esté
 * pendiente.
 */
@RestController
@RequestMapping("/openc2")
public class OpenC2Controller {

    private static final Logger log = LoggerFactory.getLogger(OpenC2Controller.class);

    private final List<Actuator> registeredActuators;
    private ObjectMapper objectMapper;

    // Spring inyecta automáticamente todas las clases que implementen 'Actuator'
    public OpenC2Controller(List<Actuator> registeredActuators, ObjectMapper objectMapper) {
        this.registeredActuators = registeredActuators;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/command", consumes = "application/openc2+json;version=1.0")
    public ResponseEntity<String> receiveCommand(@RequestBody String rawJson) {
        ResponseEntity<String> restResponse = null;
        try {
            // 1. Deserializar con Lycan los comandos que no atiende ThreatHuntingService.
            // Los detalles de una OpenC2Message se pueden consultar en
            // lycanHOME/openc2-lycan-java/doc/org/oasis/openc2/lycan/OpenC2Message.html
            OpenC2Message command = objectMapper.readValue(rawJson, OpenC2Message.class);
            OpenC2Response openC2Response = new OpenC2Response();

            // 1. Filtrar los actuadores que deben responder
            List<Actuator> matchingActuators = registeredActuators.stream()
                    .filter(actuator -> actuator.supports(command))
                    .collect(Collectors.toList());
            // Si ningún actuador puede procesarlo
            if (matchingActuators.isEmpty()) {
                openC2Response.setStatus(501);
                openC2Response.setStatusText("Not Implemented: No actuator registered for this target/action");
            } else {
                openC2Response = aggregateResponses(matchingActuators, command);
            }
            restResponse = toResponseEntity(openC2Response);
        } catch (Exception e) {
            restResponse = ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\": 500, \"status_text\": \"Error executing OpenC2 command\"}");

        }
        return restResponse;
    }

    /**
     * Construcción de la respuesta OpenC2 a partir de la respuesta de cada uno de
     * los actuadores.
     * Esto está sin revisar, es sólo un esqueleto para que sirva de base
     * 
     * @param actuators
     * @param command
     * @return
     */
    private OpenC2Response aggregateResponses(List<Actuator> actuators, OpenC2Message command) {
        // Estado si todo va bien
        int finalStatusCode = 200;
        OpenC2Response aggregatedResponse = new OpenC2Response();
        aggregatedResponse.setStatus(200);

        Map<String, Object> aggregatedResults = new HashMap<>();
        // La combinación de respuestas de los actuadores no está bien trabajada, hay
        // que pensarla bien
        for (Actuator actuator : actuators) {
            try {
                OpenC2Response response = actuator.solve(command);

                // Si alguno falla, el estado general deja de ser 200
                if (response.getStatus() >= 400) {
                    aggregatedResponse.setStatus(207); // Exito parcial alguno tiene problemas
                    aggregatedResponse.setStatusText(actuator.getProfileName() + ":" + response.getStatusText() + ";"
                            + aggregatedResponse.getStatusText());
                }

                // Agrupamos el resultado bajo la clave del actuador
                if (response.getResults() != null) {
                    aggregatedResults.put(actuator.getProfileName(), response.getResults());
                }

            } catch (Exception e) {
                aggregatedResponse.setStatus(500);
                aggregatedResponse.setStatusText("General error: " + e.getMessage());
                aggregatedResults.put(actuator.getProfileName(), Map.of("error", e.getMessage()));
            }
        }
        aggregatedResponse.setResults(aggregatedResults);
        return aggregatedResponse;
    }

    private ResponseEntity<String> toResponseEntity(OpenC2Response openC2Response) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            // 1. Convertir la respuesta OpenC2 a JSON String
            String jsonBody = mapper.writeValueAsString(openC2Response);

            // 2. Extraer el estado HTTP del atributo status de OpenC2
            int statusCode = openC2Response.getStatus();
            HttpStatus status = HttpStatus.resolve(statusCode) != null
                    ? HttpStatus.valueOf(statusCode)
                    : HttpStatus.INTERNAL_SERVER_ERROR;

            // 3. Configurar la cabecera Media Type
            HttpHeaders headers = new HttpHeaders();
            // Puedes usar MediaType.APPLICATION_JSON o la cabecera oficial de OpenC2:
            headers.setContentType(MediaType.parseMediaType("application/openc2+json;version=1.0"));

            // 4. Retornar el ResponseEntity
            return new ResponseEntity<>(jsonBody, headers, status);

        } catch (JsonProcessingException e) {
            // Manejo en caso de fallo de serialización
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\": 500, \"status_text\": \"Error serializando respuesta OpenC2\"}");
        }

    }
}
