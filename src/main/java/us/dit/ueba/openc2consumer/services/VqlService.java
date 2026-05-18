package us.dit.ueba.openc2consumer.services;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.proto.Api.VQLResponse;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiBlockingStub;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiStub;

@Service("vqlService")
public class VqlService {

    private VqlApiStub asyncStub;
    private VqlApiBlockingStub blockingStub;
    @Value("${artifacts.path}")
    private String artifactsPath;
    private static Logger log = LoggerFactory.getLogger(VqlService.class);

    public static enum EvidenceType {
        USERLOGON,
        USERSESSION,
        USERLOGOUT,
        USERSESSIONDURATION
    }

    /**
     * Inyectamos los stubs de gRPC para poder comunicarnos con Velociraptor.
     *
     * @param stub
     * @param blockingStub
     */
    VqlService(VqlApiStub stub, VqlApiBlockingStub blockingStub) {
        this.asyncStub = stub;
        this.blockingStub = blockingStub;
    }

    public String getServiceDescriptor() {
        return VqlApiGrpc.getServiceDescriptor().toString();
    }

    public void sendQuery(EvidenceType artefact, String name) {
        try {
            VQLCollectorArgs args = new ArgsBuilder(artefact, artifactsPath)
                    .setName(name)
                    .buildArgs();
            log.debug("\n Enviando artefacto {} a Velociraptor, con argumentos {}", artefact, args);
            Iterator<VQLResponse> responseStream = blockingStub.query(args);

            // 4. Consumimos la respuesta (aunque upsert_client_artifact no suele devolver filas,
            // es obligatorio iterar el stream gRPC en Java para que la petición se complete)
            while (responseStream.hasNext()) {
                VQLResponse response = responseStream.next();
                if (response.getLog() != null && !response.getLog().isEmpty()) {
                    log.info("Log del servidor: " + response.getLog());
                }
            }

            log.info("¡Artefacto registrado con éxito en el servidor!");

        } catch (Exception e) {
            log.error("Error al registrar el artefacto por gRPC: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
