package us.dit.ueba.openc2consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiBlockingStub;

@Configuration
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
    }*/

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
}
