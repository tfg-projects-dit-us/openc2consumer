package us.dit.ueba.openc2consumer.profiles.th;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import us.dit.ueba.openc2consumer.core.controllers.OpenC2Controller;
import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;
import us.dit.ueba.openc2consumer.profiles.th.model.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InvestigateTest {
    private ThreatHuntingService service;
    private MockMvc mvc;
    private final InvestigationRequest request = new InvestigationRequest("userlogon", Map.of("username", "alice"));

    @BeforeEach
    void setUp() {
        service = mock(ThreatHuntingService.class);
        when(service.supportsInvestigations()).thenReturn(true);
        when(service.listHuntflows()).thenReturn(List.of(new HuntflowInfo("userlogon", Map.of("username", "string"))));
        mvc = endpoint();
    }

    private MockMvc endpoint() {
        return MockMvcBuilders.standaloneSetup(new OpenC2Controller(
                new OpenC2Consumer(List.of(new ThreatHuntingProfile(service))), new ObjectMapper())).build();
    }

    private String command(String values) {
        return "{\"action\":\"investigate\",\"target\":{\"th\":{\"hunt\":\"userlogon\"}},"
                + "\"args\":{\"th\":{\"huntargs\":{\"string_args\":" + values + "}}}}";
    }

    @Test
    void translatesArgumentsAndReturnsTextResults() throws Exception {
        when(service.investigate(request)).thenReturn(new InvestigationResult(List.of(Map.of("username", "alice"))));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(command("[\"alice\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.inv_returns[0].string_returns[0]").value("{\"username\":\"alice\"}"));
        verify(service).investigate(request);
    }

    @Test
    void emptyFindingsAreSuccessful() throws Exception {
        when(service.investigate(request)).thenReturn(new InvestigationResult(List.of()));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(command("[\"alice\"]")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results.th.inv_returns[0].string_returns").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[\" \"]", "[null]", "[\"alice\",\"bob\"]"})
    void invalidUsernameNeverExecutes(String values) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(command(values)))
                .andExpect(status().isBadRequest());
        verify(service, never()).investigate(any());
    }

    @Test
    void unknownHuntflowNeverExecutes() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command("[\"alice\"]").replace("userlogon", "unknown")))
                .andExpect(status().isNotFound());
        verify(service, never()).investigate(any());
    }

    @ParameterizedTest
    @EnumSource(InvestigationException.Reason.class)
    void translatesServiceErrors(InvestigationException.Reason reason) throws Exception {
        when(service.investigate(request)).thenThrow(new InvestigationException(reason, "internal details"));
        int expected = switch (reason) {
            case UNSUPPORTED -> 501;
            case UNKNOWN_HUNTFLOW -> 404;
            case INVALID_ARGUMENTS -> 400;
            case UNAVAILABLE -> 503;
            case EXECUTION_FAILED -> 500;
        };
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(command("[\"alice\"]")))
                .andExpect(status().is(expected)).andExpect(jsonPath("$.status").value(expected));
    }

    @Test
    void advertisesInvestigationWhenServiceOffersIt() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results.pairs.investigate[0]").value("th.hunt"));
    }

    @Test
    void catalogOnlyServiceDoesNotAdvertiseOrExecute() throws Exception {
        when(service.supportsInvestigations()).thenReturn(false);
        mvc = endpoint();
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(command("[\"alice\"]")))
                .andExpect(status().isNotImplemented());
        verify(service, never()).investigate(any());
    }
    @Test
    void rejectsAmbiguousStringMapping() throws Exception {
        var required = new java.util.LinkedHashMap<String, String>();
        required.put("username", "string");
        required.put("hostname", "string");
        when(service.listHuntflows()).thenReturn(List.of(new HuntflowInfo("other", required)));
        var expected = new InvestigationRequest("other", Map.of("hostname", "pc01", "username", "alice"));
        when(service.investigate(expected)).thenReturn(new InvestigationResult(List.of()));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command("[\"pc01\",\"alice\"]").replace("userlogon", "other")))
                .andExpect(status().isNotImplemented());
        verify(service, never()).investigate(any());
    }

    @Test
    void acceptsHuntflowWithoutArguments() throws Exception {
        when(service.listHuntflows()).thenReturn(List.of(new HuntflowInfo("other", null)));
        var expected = new InvestigationRequest("other", Map.of());
        when(service.investigate(expected)).thenReturn(new InvestigationResult(List.of()));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"investigate\",\"target\":{\"th\":{\"hunt\":\"other\"}}}"))
                .andExpect(status().isOk());
        verify(service).investigate(expected);
    }

    @Test
    void rejectsTypesNotImplementedWithoutExecuting() throws Exception {
        when(service.listHuntflows()).thenReturn(List.of(new HuntflowInfo("userlogon", Map.of("process", "process"))));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command("[\"alice\"]"))).andExpect(status().isNotImplemented());
        verify(service, never()).investigate(any());
    }
    @Test
    void rejectsAmbiguousMixedMapping() throws Exception {
        when(service.listHuntflows()).thenReturn(List.of(new HuntflowInfo("mixed",
                Map.of("username", "string", "hostname", "string", "limit", "integer", "attempts", "integer"))));
        var expected = new InvestigationRequest("mixed", Map.of("username", "alice", "hostname", "pc01",
                "attempts", 2L, "limit", 10L));
        when(service.investigate(expected)).thenReturn(new InvestigationResult(List.of()));
        String json = command("[\"pc01\",\"alice\"]").replace("userlogon", "mixed")
                .replace("\"string_args\":", "\"integer_args\":[2,10],\"string_args\":");
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(json))
                .andExpect(status().isNotImplemented());
        verify(service, never()).investigate(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[1,2]"})
    void invalidIntegerCardinalityOrNullNeverExecutes(String values) throws Exception {
        when(service.listHuntflows()).thenReturn(List.of(new HuntflowInfo("count", Map.of("limit", "integer"))));
        String json = command(values).replace("userlogon", "count").replace("string_args", "integer_args");
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(json))
                .andExpect(status().isBadRequest());
        verify(service, never()).investigate(any());
    }
}