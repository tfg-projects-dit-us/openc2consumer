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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.Api.VQLEnv;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.Api.VQLRequest;

/**
 * Reúne una consulta VQL, un nombre y sus variables en una petición para Velociraptor.
 * Recibe el texto de VqlQueries y devuelve la petición como VQLCollectorArgs.
 * VqlService se encarga de enviarla al servidor.
 * @author Isabel Román
 */
public class ArgsBuilder {

    private String name;
    private final String query;

    Map<String, VQLEnv> variables = null;

    private final static Logger log = LoggerFactory.getLogger(ArgsBuilder.class);

    /** Recibe el texto de la consulta. */
    ArgsBuilder(String query) {
        this.query = query;
    }

    /** Guarda el nombre que identificará la consulta dentro de la petición. */
    ArgsBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /** Guarda una variable y su valor; si el nombre ya existe, sustituye su valor. */
    public ArgsBuilder setVariable(String key, String value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, VQLEnv.newBuilder().setKey(key).setValue(value).build());
        return this;
    }

    /** Construye la petición; rechaza un nombre o texto de consulta null y permite omitir variables. */
    public VQLCollectorArgs buildArgs() {
        String vqlQuery = query;
        if (vqlQuery == null || name == null) {
            log.error("La consulta VQL o el nombre del artefacto no se localizan");
            throw new IllegalArgumentException("VQL query and name must be provided to build arguments for Velociraptor.");
        }

        // Crear la consulta con su nombre y su texto VQL.
        VQLRequest request = VQLRequest.newBuilder()
                .setVQL(vqlQuery)
                .setName(name)
                .build();

        // Incorporar la consulta y, si se han indicado, sus variables.
        VQLCollectorArgs.Builder args = VQLCollectorArgs.newBuilder().addQuery(request);
        if (variables != null) {
            log.debug("Construyendo argumentos con variables: {}", variables.keySet());
            args.addAllEnv(variables.values());
        } else {
            log.debug("Construyendo argumentos sin variables");
        }
        return args.build();
    }
}