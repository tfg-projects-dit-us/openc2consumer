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

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import us.dit.ueba.openc2consumer.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.proto.Api.VQLResponse;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiBlockingStub;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiStub;

@TestConfiguration
public class TestVelociraptorConfig {

    private static final String SERVER_NAME = "velociraptor-test";

    @Bean
    public Server grpcTestServer() throws Exception {
        return InProcessServerBuilder.forName(SERVER_NAME)
                .directExecutor()
                .addService(new VqlApiGrpc.VqlApiImplBase() {
                    @Override
                    public void query(VQLCollectorArgs request, StreamObserver<VQLResponse> responseObserver) {
                        responseObserver.onNext(VQLResponse.newBuilder()
                                .setLog("Mock response for: " + request.getQueryList())
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();
    }

    @Bean
    @Primary
    public ManagedChannel velociraptorChannel(Server grpcTestServer) {
        return InProcessChannelBuilder.forName(SERVER_NAME)
                .directExecutor()
                .build();
    }

    @Bean
    @Primary
    public VqlApiBlockingStub velociraptorBlockingStub(ManagedChannel velociraptorChannel) {
        return VqlApiGrpc.newBlockingStub(velociraptorChannel);
    }

    @Bean
    @Primary
    public VqlApiStub velociraptorAsyncStub(ManagedChannel velociraptorChannel) {
        return VqlApiGrpc.newStub(velociraptorChannel);
    }
}
