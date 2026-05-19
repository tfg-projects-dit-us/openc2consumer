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
package us.dit.ueba.openc2consumer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc;

@Configuration
@Profile("!test")
public class VelociraptorConfig {

    /* 
    @Bean
    public ManagedChannel velociraptorChannel() throws SSLException {
        SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(new File("ca.crt"))
                .keyManager(new File("api.crt"), new File("api.key"))
                .build();

        return NettyChannelBuilder.forAddress("localhost", 8000)
                .sslContext(sslContext)
                .build();
    }
    @Bean

    public ManagedChannel velociraptorChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 8000)
                .usePlaintext()
                .build();
    }

    @Bean
    public VqlApiBlockingStub velociraptorBlockingStub(ManagedChannel channel) {
        return VqlApiGrpc.newBlockingStub(channel);
    }

    @Bean
    public VqlApiGrpc.VqlApiStub velociraptorAsyncStub(ManagedChannel channel) {
        return VqlApiGrpc.newStub(channel);
    }
     */
    // 1. Leemos el host y el puerto desde el archivo properties
    @Value("${velociraptor.host:localhost}")
    private String host;

    @Value("${velociraptor.port:8001}")
    private int port;

    @Bean
    public ManagedChannel velociraptorChannel() {
        // 2. Usamos las variables en lugar de los textos fijos
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    public VqlApiGrpc.VqlApiBlockingStub velociraptorBlockingStub(ManagedChannel channel) {
        return VqlApiGrpc.newBlockingStub(channel);
    }

    @Bean
    public VqlApiGrpc.VqlApiStub velociraptorAsyncStub(ManagedChannel channel) {
        return VqlApiGrpc.newStub(channel);
    }
}
