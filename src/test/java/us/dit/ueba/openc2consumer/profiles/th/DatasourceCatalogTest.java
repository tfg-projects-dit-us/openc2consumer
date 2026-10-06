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

/** Prueba la carga de fuentes desde JSON y su consulta por HTTP, sin servidor externo. */
class DatasourceCatalogTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private ByteArrayResource resource(String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
    }
    private ThreatHuntingProfile catalog(String json) {
        return new ThreatHuntingProfile(new JsonThreatHuntingService(mapper, resource("[]"), resource(json)));
    }
    private MockMvc endpoint(ThreatHuntingProfile profile) {
        return MockMvcBuilders.standaloneSetup(
                new OpenC2Controller(new OpenC2Consumer(List.of(profile)), mapper)).build();
    }
    private String command(String name) throws Exception {
        return mapper.writeValueAsString(Map.of("action", "query", "target", Map.of("th", Map.of("datasources", name))));
    }
    @Test
    void defaultCatalogIsEmpty() throws Exception {
        endpoint(new ThreatHuntingProfile(new JsonThreatHuntingService(mapper))).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0").content(command("")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.th.datasources").isArray())
                .andExpect(jsonPath("$.results.th.datasources.length()").value(0));
    }
    @ParameterizedTest
    @ValueSource(ints = {0, 2, 150})
    void listsAllConfiguredSources(int count) throws Exception {
        var entries = IntStream.range(0, count).mapToObj(i -> Map.of("ds_name", "source" + i)).toList();
        var result = endpoint(catalog(mapper.writeValueAsString(entries))).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0").content(command("")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.datasources.length()").value(count));
        for (int i = 0; i < count; i++) {
            result.andExpect(jsonPath("$.results.th.datasources[" + i + "].ds_name").value("source" + i));
        }
    }
    @Test
    void selectsExactNameAndReturnsTags() throws Exception {
        var mvc = endpoint(catalog("[{\"ds_name\":\"a\"},{\"ds_name\":\"b\",\"ds_tags\":[\"authentication\"]}]"));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(command("b")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results.th.datasources.length()").value(1))
                .andExpect(jsonPath("$.results.th.datasources[0].ds_name").value("b"))
                .andExpect(jsonPath("$.results.th.datasources[0].ds_tags[0]").value("authentication"));
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0").content(command("missing")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results.th.datasources.length()").value(0));
    }
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"response_requested\":\"complete\"}"})
    void acceptsCompleteArguments(String args) throws Exception {
        var json = mapper.readTree(command(""));
        ((com.fasterxml.jackson.databind.node.ObjectNode) json).set("args", mapper.readTree(args));
        endpoint(catalog("[]")).perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(json.toString())).andExpect(status().isOk());
    }
    @ParameterizedTest
    @ValueSource(strings = {"{\"duration\":60}", "{\"th\":{}}", "{\"response_requested\":\"ack\"}"})
    void rejectsUnimplementedOptions(String args) throws Exception {
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(command(""));
        json.set("args", mapper.readTree(args));
        endpoint(catalog("[]")).perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(json.toString())).andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value(501));
    }
    @ParameterizedTest
    @ValueSource(strings = {"{}", "[{\"ds_name\":\"\"}]", "[{}]",
            "[{\"ds_name\":\"x\"},{\"ds_name\":\"x\"}]"})
    void rejectsInvalidCatalog(String json) {
        assertThrows(IllegalStateException.class, () -> catalog(json));
    }
}