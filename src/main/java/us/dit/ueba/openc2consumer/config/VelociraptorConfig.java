package us.dit.ueba.openc2consumer.config;

import javax.net.ssl.SSLException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiBlockingStub;

@Configuration
public class VelociraptorConfig {

    @Bean
    public VqlApiBlockingStub velociraptorStub() throws SSLException {
        // Aquí cargarías tus certificados .pem (api.key, api.crt, ca.crt)
        SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(new File("ca.crt"))
                .keyManager(new File("api.crt"), new File("api.key"))
                .build();

        ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", 8000)
                .sslContext(sslContext)
                .build();

        return VqlApiGrpc.newBlockingStub(channel);
    }
}
