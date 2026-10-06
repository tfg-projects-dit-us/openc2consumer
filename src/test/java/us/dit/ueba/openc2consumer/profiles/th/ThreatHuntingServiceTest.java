package us.dit.ueba.openc2consumer.profiles.th;

import java.util.List;
import us.dit.ueba.openc2consumer.core.controllers.OpenC2Controller;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import us.dit.ueba.openc2consumer.profiles.th.ThreatHuntingProfile;
import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;
import us.dit.ueba.openc2consumer.profiles.th.ThreatHuntingService;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;
import us.dit.ueba.openc2consumer.profiles.th.model.DatasourceInfo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Verifica el perfil sin archivos ni un servidor gRPC. */
class ThreatHuntingServiceTest {
    @org.junit.jupiter.api.BeforeEach
    void verifyCapabilityDiscovery() {
        verify(threatHuntingService).supportsInvestigations();
    }

    private final ThreatHuntingService threatHuntingService = mock(ThreatHuntingService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OpenC2Controller(
            new OpenC2Consumer(List.of(new ThreatHuntingProfile(threatHuntingService))), new ObjectMapper())).build();

    @Test
    void returnsServiceHuntflows() throws Exception {
        when(threatHuntingService.listHuntflows()).thenReturn(List.of(new HuntflowInfo("custom", Map.of("user", "string"))));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{}}}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info[0].path").value("custom"))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].args_required.user").value("string"));
        verify(threatHuntingService).listHuntflows();
        verifyNoMoreInteractions(threatHuntingService);
    }

    @Test
    void passesDatasourceNameToService() throws Exception {
        when(threatHuntingService.listDatasources("logs")).thenReturn(List.of(new DatasourceInfo("logs", List.of("auth"))));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"logs\"}}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.datasources[0].ds_name").value("logs"))
                .andExpect(jsonPath("$.results.th.datasources[0].ds_tags[0]").value("auth"));
        verify(threatHuntingService).listDatasources("logs");
        verifyNoMoreInteractions(threatHuntingService);
    }

    @Test
    void rejectsUnsupportedOptionsBeforeCallingService() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{}}},\"args\":{\"duration\":60}}"))
                .andExpect(status().isNotImplemented());
        verifyNoMoreInteractions(threatHuntingService);
    }
}