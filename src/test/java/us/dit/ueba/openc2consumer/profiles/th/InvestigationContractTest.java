package us.dit.ueba.openc2consumer.profiles.th;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import us.dit.ueba.openc2consumer.profiles.th.catalog.JsonThreatHuntingService;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationRequest;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigationContractTest {
    @Test
    void representsFinalResultWithoutExternalTypes() {
        var service = mock(ThreatHuntingService.class);
        var request = new InvestigationRequest("userlogon", Map.of("username", "alice"));
        var result = new InvestigationResult(List.of(Map.of("event", "login", "username", "alice")));
        when(service.investigate(request)).thenReturn(result);
        assertEquals(result, service.investigate(request));
        verify(service).investigate(request);
    }

    @Test
    void catalogImplementationCannotPretendToExecute() {
        var service = new JsonThreatHuntingService(new ObjectMapper());
        var failure = assertThrows(InvestigationException.class,
                () -> service.investigate(new InvestigationRequest("userlogon", Map.of("username", "alice"))));
        assertEquals(InvestigationException.Reason.UNSUPPORTED, failure.reason());
    }

    @Test
    void requestAndResultKeepSnapshotsOfMutableData() {
        Map<String, Object> arguments = new HashMap<>(Map.of("username", "alice"));
        var request = new InvestigationRequest("userlogon", arguments);
        arguments.put("username", "bob");
        assertEquals("alice", request.arguments().get("username"));
        assertThrows(UnsupportedOperationException.class, () -> request.arguments().clear());
        var finding = new HashMap<>(Map.of("event", "login"));
        var findings = new ArrayList<Map<String, String>>();
        findings.add(finding);
        var result = new InvestigationResult(findings);
        finding.clear();
        findings.clear();
        assertEquals(List.of(Map.of("event", "login")), result.findings());
        assertThrows(UnsupportedOperationException.class, () -> result.findings().get(0).clear());
    }

    @Test
    void rejectsBlankHuntflowAndArgumentValue() {
        assertThrows(IllegalArgumentException.class, () -> new InvestigationRequest(" ", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new InvestigationRequest("userlogon", Map.of("username", " ")));
    }
}