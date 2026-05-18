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
