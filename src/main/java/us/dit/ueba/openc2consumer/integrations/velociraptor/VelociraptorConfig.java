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
package us.dit.ueba.openc2consumer.integrations.velociraptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.VqlApiGrpc;

/**
 * Prepara el canal gRPC y los clientes que permiten enviar peticiones a Velociraptor.
 * Obtiene la dirección y el puerto de la configuración y crea un canal sin cifrado.
 * Esta configuración se utiliza cuando el perfil de Spring test no está activo.
 * @author Isabel Román
 */
@Configuration
@Profile("!test")
public class VelociraptorConfig {

    // Dirección del servidor; si no se configura, se utiliza localhost.
    @Value("${velociraptor.host:localhost}")
    private String host;

    // Puerto del servidor; si no se configura, se utiliza 8001.
    @Value("${velociraptor.port:8001}")
    private int port;

    /** Crea el canal hacia el servidor usando host y port, sin TLS. */
    @Bean
    public ManagedChannel velociraptorChannel() {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    /** Crea el cliente que utiliza VqlService para enviar consultas y leer sus respuestas. */
    @Bean
    public VqlApiGrpc.VqlApiBlockingStub velociraptorBlockingStub(ManagedChannel channel) {
        return VqlApiGrpc.newBlockingStub(channel);
    }

    /** Crea un cliente que recibe las respuestas mediante callbacks; VqlService no lo utiliza. */
    @Bean
    public VqlApiGrpc.VqlApiStub velociraptorAsyncStub(ManagedChannel channel) {
        return VqlApiGrpc.newStub(channel);
    }
}
