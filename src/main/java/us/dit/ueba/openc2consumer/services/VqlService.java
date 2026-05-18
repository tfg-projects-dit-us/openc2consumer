package us.dit.ueba.openc2consumer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.proto.Api.VQLRequest;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiBlockingStub;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiStub;

@Service("vqlService")
public class VqlService {

    private VqlApiStub asyncStub;
    private VqlApiBlockingStub blockingStub;
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

    private String getVqlString() {
        return "SELECT upsert_client_artifact(\n"
                + //
                "    artifact='''{\n"
                + //
                "        \"name\": \"Custom.MultiOS.EventLogs.Logons\",\n"
                + //
                "        \"type\": \"CLIENT_EVENT\",\n"
                + //
                "        \"sources\": [\n"
                + //
                "            {\n"
                + //
                "                \"name\": \"WindowsLogons\",\n"
                + //
                "                \"precondition\": \"SELECT Os FROM info() WHERE Os = 'windows'\",\n"
                + //
                "                \"query\": \"SELECT System.TimeCreated.SystemTime AS Timestamp, EventData.TargetUserName AS Username, 'windows' AS OS FROM watch_evtx(log=\\\\\"Security\\\\\") WHERE System.EventID.Value = 4624\"\n"
                + //
                "            },\n"
                + //
                "            {\n"
                + //
                "                \"name\": \"LinuxLogons\",\n"
                + //
                "                \"precondition\": \"SELECT Os FROM info() WHERE Os = 'linux'\",\n"
                + //
                "                \"query\": \"SELECT timestamp, grep(reg=\\\\\"Accepted\\\\\", string=Line) AS LogonData, 'linux' AS OS FROM watch_syslog(filename=\\\\\"/var/log/auth.log\\\\\") WHERE LogonData\"\n"
                + //
                "            }\n"
                + //
                "        ]\n"
                + //
                "    }'''\n"
                + //
                ") FROM scope()";
    }

    public void queryClients() {

        VQLRequest request = VQLRequest.newBuilder()
                .setVQL(getVqlString())
                .setName("LogonCollector")
                .build();
        VQLCollectorArgs args = VQLCollectorArgs.newBuilder()
                .addQuery(request)
                .build();

        // El resultado suele venir en un stream de respuestas
        /*  stub.query(args).forEachRemaining(response -> {
             System.out.println("Respuesta: " + response.getResponse());
        });*/
    }
}
