package us.dit.ueba.openc2consumer.services.vql;

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
public class VqlService implements VqlInterface {

    private VqlApiStub asyncStub;
    private VqlApiBlockingStub blockingStub;
    @Value("${artifacts.path}")
    private String artifactsPath;

    private static Logger log = LoggerFactory.getLogger(VqlService.class);

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

    /**
     * Envía un artefacto al velociraptor para que lo registre y lo deje listo
     * para ejecutar. Por ejemplo, si quieres monitorizar los logons usuarios,
     * primero tienes que registrar el artefacto "UserLogons" en el servidor, y
     * luego ya puedes mandar ejecutar ese artefacto con el usuario concreto que
     * quieres monitorizar (en otra consulta)
     *
     * @param artefact
     * @param name
     */
    public void sendNewArtefact(EvidenceType evidenceType) {
        String name = evidenceType.toString().toLowerCase() + "_openc2_soar_new_artefact";
        log.debug("\n Enviando artefacto para recogida de evidencias de tipo {} a Velociraptor, con nombre {}", evidenceType, name);
        try {
            QuerySolver solver = new NewArtifactQuerySolver(EvidenceType.USERLOGON, artifactsPath);
            VQLCollectorArgs args = new ArgsBuilder(solver)
                    .setName(name)
                    .buildArgs();
            log.debug("\n Construyendo petición de nuevo artefacto {} a Velociraptor, con argumentos {}", evidenceType, args);
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

    /**
     * Comienza a monitorizar un artefacto ya registrado en el servidor.
     *
     * @param evidenceType
     */
    public void startMonitoring(EvidenceType evidenceType) {
        String name = evidenceType.toString().toLowerCase() + "_openc2_soar_start_monitoring";

        try {
            //QuerySolver solver = new StartMonitoringQuerySolver(evidenceType, artifactsPath);
            QuerySolver solver = new StartMonitoringQuerySolver();
            String artifactName = "UEBA.SOAR." + evidenceType.toString().toLowerCase(); // El nombre del artefacto es el mismo que el del tipo de evidencia
            VQLCollectorArgs args = new ArgsBuilder(solver)
                    .setName(name)
                    .setVariable("ArtifactName", artifactName)
                    .buildArgs();
            log.debug("\n Construyendo petición para arrancar monitorización {} a Velociraptor, con argumentos {}", evidenceType, args);
            Iterator<VQLResponse> responseStream = blockingStub.query(args);

            // 4. Consumimos la respuesta (aunque upsert_client_artifact no suele devolver filas,
            // es obligatorio iterar el stream gRPC en Java para que la petición se complete)
            while (responseStream.hasNext()) {
                VQLResponse response = responseStream.next();
                if (response.getLog() != null && !response.getLog().isEmpty()) {
                    log.info("Log del servidor: " + response.getLog());
                }
            }

            log.info("Starting Evidence Catch in Velociraptor!");

        } catch (Exception e) {
            log.error("Error in gRPC starting evidence catch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     *
     * Añade un usuario a monitorizar en un artefacto ya registrado en el
     * servidor
     *
     * @param evidenceType
     * @param username
     */
    public void addUser(EvidenceType evidenceType, String username) {
        String name = evidenceType.toString().toLowerCase() + "_openc2_soar_add_user";

        try {
            QuerySolver solver = new AddUserQuerySolver();
            String tableName = "Server." + evidenceType.toString().toLowerCase(); // El nombre de la tabla es el mismo que el del tipo de evidencia, pero con el prefijo "Server."
            VQLCollectorArgs args = new ArgsBuilder(solver)
                    .setName(name)
                    .setVariable("TableName", tableName)
                    .setVariable("TargetUSer", username)
                    .buildArgs();
            log.debug("\n Construyendo petición para arrancar monitorización {} a Velociraptor, con argumentos {}", evidenceType, args);
            Iterator<VQLResponse> responseStream = blockingStub.query(args);

            // 4. Consumimos la respuesta (aunque upsert_client_artifact no suele devolver filas,
            // es obligatorio iterar el stream gRPC en Java para que la petición se complete)
            while (responseStream.hasNext()) {
                VQLResponse response = responseStream.next();
                if (response.getLog() != null && !response.getLog().isEmpty()) {
                    log.info("Log del servidor: " + response.getLog());
                }
            }

            log.info("Adding user {} to {} monitoring in Velociraptor!", username, evidenceType);

        } catch (Exception e) {
            log.error("Error in gRPC adding user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteUser(EvidenceType evidenceType, String username) {
        String name = evidenceType.toString().toLowerCase() + "_openc2_soar_delete_user";

        try {
            QuerySolver solver = new DeleteUserQuerySolver();
            String tableName = "Server." + evidenceType.toString().toLowerCase(); // El nombre de la tabla es el mismo que el del tipo de evidencia, pero con el prefijo "Server."
            VQLCollectorArgs args = new ArgsBuilder(solver)
                    .setName(name)
                    .setVariable("TableName", tableName)
                    .setVariable("TargetUSer", username)
                    .buildArgs();
            log.debug("\n Construyendo petición para eliminar usuario de monitorización {} a Velociraptor, con argumentos {}", evidenceType, args);
            Iterator<VQLResponse> responseStream = blockingStub.query(args);

            // 4. Consumimos la respuesta (aunque upsert_client_artifact no suele devolver filas,
            // es obligatorio iterar el stream gRPC en Java para que la petición se complete)
            while (responseStream.hasNext()) {
                VQLResponse response = responseStream.next();
                if (response.getLog() != null && !response.getLog().isEmpty()) {
                    log.info("Log del servidor: " + response.getLog());
                }
            }

            log.info("Deleting user {} from {} monitoring in Velociraptor!", username, evidenceType);

        } catch (Exception e) {
            log.error("Error in gRPC deleting user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
