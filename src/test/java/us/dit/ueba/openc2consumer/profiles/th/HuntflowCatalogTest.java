package us.dit.ueba.openc2consumer.profiles.th;

import java.nio.charset.StandardCharsets;
import java.util.List;
import us.dit.ueba.openc2consumer.core.controllers.OpenC2Controller;
import java.util.Map;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import us.dit.ueba.openc2consumer.profiles.th.ThreatHuntingProfile;
import us.dit.ueba.openc2consumer.profiles.th.catalog.JsonThreatHuntingService;
import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prueba la carga de huntflows desde JSON y la devolucion de la lista completa por HTTP. */
class HuntflowCatalogTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String COMMAND = "{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{}}}}";

    private MockMvc endpoint(ThreatHuntingProfile profile) {
        return MockMvcBuilders.standaloneSetup(
                new OpenC2Controller(new OpenC2Consumer(List.of(profile)), mapper)).build();
    }

    private ThreatHuntingProfile catalog(String json) {
        return new ThreatHuntingProfile(new JsonThreatHuntingService(mapper, new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void listsDefaultUserlogonCatalog() throws Exception {
        endpoint(new ThreatHuntingProfile(new JsonThreatHuntingService(mapper)))
                .perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(COMMAND))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.th.huntflow_info.length()").value(1))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].path").value("userlogon"))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].args_required.username").value("string"))
                .andExpect(jsonPath("$.results.th.th").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 150})
    void returnsEntireCatalogWithoutTruncation(int count) throws Exception {
        var entries = IntStream.range(0, count).mapToObj(i -> Map.of("path", "hunt" + i)).toList();
        var result = endpoint(catalog(mapper.writeValueAsString(entries)))
                .perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(COMMAND))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info.length()").value(count));
        for (int i = 0; i < count; i++) {
            result.andExpect(jsonPath("$.results.th.huntflow_info[" + i + "].path").value("hunt" + i));
        }
    }

    @Test
    void missingTagsReturnEmptyCatalog() throws Exception {
        endpoint(new ThreatHuntingProfile(new JsonThreatHuntingService(mapper)))
                .perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                        .content(COMMAND.replace("\"huntflows\":{}", "\"huntflows\":{\"tags\":[\"logon\"]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info").isEmpty());
    }

    @Test
    void advertisesOnlyImplementedPairs() throws Exception {
        endpoint(new ThreatHuntingProfile(new JsonThreatHuntingService(mapper)))
                .perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                        .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.pairs.query.length()").value(3))
                .andExpect(jsonPath("$.results.pairs.query[1]").value("th.huntflows"))
                .andExpect(jsonPath("$.results.pairs.query[2]").value("th.datasources"))
                .andExpect(jsonPath("$.results.pairs.investigate").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[{\"path\":\"\"}]", "[{\"path\":\"x\"},{\"path\":\"x\"}]", "{}"})
    void rejectsInvalidCatalogAtStartup(String json) {
        assertThrows(IllegalStateException.class, () -> catalog(json));
    }
}