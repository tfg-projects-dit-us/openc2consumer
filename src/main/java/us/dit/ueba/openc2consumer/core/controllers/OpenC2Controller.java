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
package us.dit.ueba.openc2consumer.core.controllers;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;

/**
 * Recibe comandos OpenC2 por HTTP en POST /openc2/command.
 * Lee el JSON, rechaza claves duplicadas y comprueba que haya un solo target.
 * Convierte el mensaje a OpenC2Message y lo entrega a OpenC2Consumer.
 * Devuelve su respuesta como JSON usando el estado OpenC2 como estado HTTP.
 * Los errores de lectura o conversion capturados producen 400; los fallos
 * de procesamiento o serializacion capturados producen 500.
 */
@RestController
@RequestMapping("/openc2")
public class OpenC2Controller {
    private static final Logger log = LoggerFactory.getLogger(OpenC2Controller.class);
    private static final String OPENC2_CONTENT_TYPE = "application/openc2+json;version=1.0";
    private static final MediaType OPENC2_MEDIA_TYPE = MediaType.parseMediaType(OPENC2_CONTENT_TYPE);

    private final OpenC2Consumer openC2Consumer;
    private final ObjectMapper objectMapper;

    public OpenC2Controller(OpenC2Consumer consumer, ObjectMapper objectMapper) {
        this.openC2Consumer = consumer;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/command", consumes = OPENC2_CONTENT_TYPE)
    public ResponseEntity<String> receiveCommand(@RequestBody String rawJson) {
        // 1. Leer y validar el comando recibido.
        OpenC2Message command;
        try {
            JsonNode json = objectMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                    .readTree(rawJson);
            if (json == null || !hasSingleTarget(json.get("target"))) {
                return errorResponse(HttpStatus.BAD_REQUEST,
                        "Bad Request: target must contain exactly one non-null option");
            }
            command = objectMapper.treeToValue(json, OpenC2Message.class);
        } catch (JsonProcessingException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Bad Request: invalid OpenC2 JSON");
        }

        // 2. Entregar el comando al Consumer y devolver su respuesta por HTTP.
        try {
            OpenC2Response response = openC2Consumer.solve(command);
            return toHttpResponse(response);
        } catch (Exception e) {
            log.error("Error executing OpenC2 command", e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error executing OpenC2 command");
        }
    }

    /** Exige una unica opcion de target, tambien dentro del objeto th. */
    private boolean hasSingleTarget(JsonNode target) {
        if (!hasOneNonNullField(target)) {
            return false;
        }
        // Dentro de TH tambien debe elegirse solo una opcion.
        if (target.has("th")) {
            return hasOneNonNullField(target.get("th"));
        }
        return true;
    }

    /** Comprueba que sea un objeto con un unico campo cuyo valor no sea null. */
    private boolean hasOneNonNullField(JsonNode node) {
        if (node == null || !node.isObject() || node.size() != 1) {
            return false;
        }
        // Solo queda un campo: comprobar que su valor no sea null.
        JsonNode value = node.elements().next();
        return !value.isNull();
    }

    /** Construye una respuesta OpenC2 de error y la convierte a respuesta HTTP. */
    private ResponseEntity<String> errorResponse(HttpStatus status, String message) {
        OpenC2Response response = createError(status, message);
        return toHttpResponse(response);
    }

    /** Crea el objeto OpenC2 que contiene el codigo y la explicacion del error. */
    private OpenC2Response createError(HttpStatus status, String message) {
        OpenC2Response response = new OpenC2Response();
        response.setStatus(status.value());
        response.setStatusText(message);
        return response;
    }

    /** Usa el estado OpenC2 como estado HTTP y convierte el objeto de respuesta a JSON. */
    private ResponseEntity<String> toHttpResponse(OpenC2Response response) {
        // Comprobar que el Consumer haya devuelto un estado HTTP reconocido.
        HttpStatus status = null;
        if (response != null && response.getStatus() != null) {
            status = HttpStatus.resolve(response.getStatus());
        }

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            response = createError(status, "Invalid OpenC2 response status");
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(response);
            return ResponseEntity.status(status)
                    .contentType(OPENC2_MEDIA_TYPE)
                    .body(jsonBody);
        } catch (JsonProcessingException e) {
            log.error("Error serializing OpenC2 response", e);
            // Respuesta fija para no volver a llamar al serializador que ha fallado.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":500,\"status_text\":\"Error serializando respuesta OpenC2\"}");
        }
    }
}