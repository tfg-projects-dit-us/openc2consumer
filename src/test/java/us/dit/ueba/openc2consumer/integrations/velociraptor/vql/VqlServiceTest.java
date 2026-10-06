package us.dit.ueba.openc2consumer.integrations.velociraptor.vql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import io.grpc.Status;
import us.dit.ueba.openc2consumer.integrations.velociraptor.config.EvidenceTypes;
import us.dit.ueba.openc2consumer.integrations.velociraptor.config.VigilanceLevels;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.Api.VQLResponse;
import us.dit.ueba.openc2consumer.integrations.velociraptor.proto.VqlApiGrpc.VqlApiBlockingStub;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VqlServiceTest {
    @TempDir Path artifacts;
    private VqlApiBlockingStub stub;
    private VqlService service;

    @BeforeEach
    void setUp() throws Exception {
        stub = mock(VqlApiBlockingStub.class);
        var types = new EvidenceTypes();
        types.setEvidences(List.of("userlogon", "usersession"));
        var levels = new VigilanceLevels();
        levels.setVigilanceLevels(List.of("STANDARD", "SUSPICIOUS", "CRITICAL"));
        service = new VqlService(stub, artifacts.toString(), types, levels);
        Files.writeString(artifacts.resolve("userlogon.artifact"), "SELECT 1 FROM scope()");
        when(stub.query(any())).thenAnswer(call -> List.of(VQLResponse.getDefaultInstance()).iterator());
    }

    private VQLCollectorArgs request() {
        var captor = ArgumentCaptor.forClass(VQLCollectorArgs.class);
        verify(stub).query(captor.capture());
        return captor.getValue();
    }

    private Map<String, String> variables(VQLCollectorArgs request) {
        return request.getEnvList().stream().collect(Collectors.toMap(env -> env.getKey(), env -> env.getValue()));
    }

    @Test
    void sendsArtifactFileContents() {
        service.sendNewArtefact("userlogon");
        var request = request();
        assertEquals(1, request.getQueryCount());
        assertEquals("SELECT 1 FROM scope()", request.getQuery(0).getVQL());
        assertEquals("userlogon_openc2_soar_new_artefact", request.getQuery(0).getName());
        assertEquals(0, request.getEnvCount());
    }

    @Test
    void startsMonitoringWithoutReadingArtifact() {
        service.startMonitoring("usersession");
        var request = request();
        assertEquals(Map.of("ArtifactName", "UEBA.SOAR.usersession"), variables(request));
        assertEquals("SELECT upsert_client_monitoring(artifact_name=ArtifactName) FROM scope()", request.getQuery(0).getVQL());
    }

    @Test
    void sendsUserAndNormalizedLevelUnderQueryVariableNames() {
        service.addUser("USERLOGON", "alice", "critical");
        var request = request();
        assertEquals(Map.of("TableName", "Server.userlogon", "TargetUser", "alice", "TargetLevel", "CRITICAL"), variables(request));
        assertTrue(request.getQuery(0).getVQL().contains("Username = TargetUser"));
        assertTrue(request.getQuery(0).getVQL().contains("'TargetLevel'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HIGH", "STANDARD"})
    void usesStandardForDefaultOrUnknownLevel(String level) {
        service.addUser("userlogon", "alice", level);
        assertEquals("STANDARD", variables(request()).get("TargetLevel"));
    }

    @Test
    void defaultOverloadUsesStandard() {
        service.addUser("userlogon", "alice");
        assertEquals("STANDARD", variables(request()).get("TargetLevel"));
    }

    @Test
    void removesUserWithCorrectVariable() {
        service.deleteUser("userlogon", "alice");
        assertEquals(Map.of("TableName", "Server.userlogon", "TargetUser", "alice"), variables(request()));
    }

    private Consumer<VqlService> operation(String name, String type) {
        return switch (name) {
            case "artifact" -> client -> client.sendNewArtefact(type);
            case "monitor" -> client -> client.startMonitoring(type);
            case "add" -> client -> client.addUser(type, "alice");
            case "delete" -> client -> client.deleteUser(type, "alice");
            default -> throw new IllegalArgumentException(name);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"artifact", "monitor", "add", "delete"})
    void rejectsUnknownEvidenceBeforeSending(String name) {
        assertThrows(IllegalArgumentException.class, () -> operation(name, "unknown").accept(service));
        verifyNoInteractions(stub);
    }

    @Test
    void missingArtifactDoesNotSendRequest() {
        assertThrows(RuntimeException.class, () -> service.sendNewArtefact("usersession"));
        verifyNoInteractions(stub);
    }

    @ParameterizedTest
    @ValueSource(strings = {"artifact", "monitor", "add", "delete"})
    void propagatesRpcFailureWithOriginalCause(String name) {
        var failure = Status.UNAVAILABLE.asRuntimeException();
        when(stub.query(any())).thenThrow(failure);
        var thrown = assertThrows(RuntimeException.class, () -> operation(name, "userlogon").accept(service));
        assertSame(failure, thrown.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"artifact", "monitor", "add", "delete"})
    void detectsFailureWhileConsumingResponseStream(String name) {
        @SuppressWarnings("unchecked")
        Iterator<VQLResponse> stream = mock(Iterator.class);
        var failure = Status.DEADLINE_EXCEEDED.asRuntimeException();
        when(stream.hasNext()).thenReturn(true).thenThrow(failure);
        when(stream.next()).thenReturn(VQLResponse.newBuilder().setLog("first response").build());
        when(stub.query(any())).thenReturn(stream);
        var thrown = assertThrows(RuntimeException.class, () -> operation(name, "userlogon").accept(service));
        assertSame(failure, thrown.getCause());
        verify(stream).next();
    }
}