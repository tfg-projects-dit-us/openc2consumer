package us.dit.ueba.openc2consumer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.proto.Api.VQLRequest;
import us.dit.ueba.openc2consumer.proto.VqlApiGrpc.VqlApiBlockingStub;

@Service
public class LogonService {

    @Autowired
    private VqlApiBlockingStub stub;

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
        stub.query(args).forEachRemaining(response -> {
            System.out.println("Respuesta: " + response.getResponse());
        });
    }
}
