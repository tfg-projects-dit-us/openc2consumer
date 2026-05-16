package us.dit.ueba.openc2consumer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.proto.Api.VQLRequest;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiBlockingStub;

@Service
public class VelociraptorClient {

    @Autowired
    private VqlApiBlockingStub stub;

    public void queryClients() {
        VQLRequest request = VQLRequest.newBuilder()
                .setQuery("SELECT * FROM info()")
                .build();

        // El resultado suele venir en un stream de respuestas
        stub.query(request).forEachRemaining(response -> {
            System.out.println("Respuesta: " + response.getResponse());
        });
    }
}
