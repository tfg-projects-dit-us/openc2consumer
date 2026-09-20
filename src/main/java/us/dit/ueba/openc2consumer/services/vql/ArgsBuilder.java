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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dit.ueba.openc2consumer.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.proto.Api.VQLEnv;
import us.dit.ueba.openc2consumer.proto.Api.VQLRequest;

/**
 * @author Isabel Román El objetivo de esta clase es construir las consultas VQL
 * que se enviarán a Velociraptor.
 */
public class ArgsBuilder {

    private String name;
    private QuerySolver solver;

    Map<String, VQLEnv> variables = null;

    private final static Logger log = LoggerFactory.getLogger(ArgsBuilder.class);

    ArgsBuilder(QuerySolver solver) {
        this.solver = solver;
    }

    ArgsBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Establece una variable de entorno para la consulta VQL. Estas variables
     * pueden ser utilizadas dentro de la consulta VQL para personalizar su
     * comportamiento. Son los prámetros de la consulta VQL p.e.
     * TargetUser={{username}}
     *
     * @param key nombre de la variable VQL
     * @param value valor enviado en el entorno de la consulta
     * @return este constructor para encadenar llamadas
     */
    public ArgsBuilder setVariable(String key, String value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, VQLEnv.newBuilder().setKey(key).setValue(value).build());
        return this;
    }

    /**
     * Construye los argumentos para enviar una consulta a Velociraptor
     *
     * Requiere la consulta del QuerySolver y el nombre establecido con setName.
     * Incluye las variables de entorno cuando se han configurado.
     *
     * @return los argumentos formateados para la consulta gRPC
     * @throws IllegalArgumentException si la consulta o el nombre son nulos
     */
    public VQLCollectorArgs buildArgs() {
        VQLRequest request = null;
        VQLCollectorArgs args = null;

        String vqlQuery = solver.getQuery();
        // Tanto la consulta como su nombre son obligatorios.
        if (vqlQuery != null && name != null) {
            request = VQLRequest.newBuilder()
                    .setVQL(vqlQuery)
                    .setName(name)
                    .build();

        } else {
            log.error("La consulta VQL o el nombre del artefacto no se localizan");
            throw new IllegalArgumentException("VQL query and name must be provided to build arguments for Velociraptor.");
        }
        if (variables != null) {
            log.debug("Construyendo argumentos con variables: {}", variables.keySet());
            args = VQLCollectorArgs.newBuilder()
                    .addQuery(request)
                    .addAllEnv(variables.values())
                    .build();

        } else {
            log.debug("Construyendo argumentos sin variables");
            args = VQLCollectorArgs.newBuilder()
                    .addQuery(request)
                    .build();
        }

        return args;
    }

}
