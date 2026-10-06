package us.dit.ueba.openc2consumer.profiles.th;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import us.dit.ueba.openc2consumer.core.controllers.OpenC2Controller;
import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HuntflowFiltersTest {
    private MockMvc endpoint() {
        var service = mock(ThreatHuntingService.class);
        when(service.listHuntflows()).thenReturn(List.of(
                new HuntflowInfo("/auth", null),
                new HuntflowInfo("/auth/login", Map.of("username", "string", "limit", "integer"), null, null, Map.of("events", "string"), null, List.of("auth", "linux")),
                new HuntflowInfo("/authentication", Map.of("username", "string")),
                new HuntflowInfo("userlogon", Map.of("username", "string"))));
        return MockMvcBuilders.standaloneSetup(new OpenC2Controller(
                new OpenC2Consumer(List.of(new ThreatHuntingProfile(service))), new ObjectMapper())).build();
    }

    private String command(String filters) {
        return "{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":" + filters + "}}}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth", "/auth/"})
    void pathSelectsLocationAndDescendantsOnly(String path) throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command("{\"path\":\"" + path + "\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results.th.huntflow_info.length()").value(2))
                .andExpect(jsonPath("$.results.th.huntflow_info[1].path").value("/auth/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"arg_names\":[\"username\",\"limit\"]}",
            "{\"arg_types\":[\"string\",\"integer\"]}",
            "{\"path\":\"/auth\",\"arg_names\":[\"username\"],\"arg_types\":[\"integer\"]}"})
    void requiresAllCriteria(String filters) throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command(filters))).andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info.length()").value(1))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].path").value("/auth/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"path\":\"/missing\"}", "{\"arg_names\":[\"missing\"]}",
            "{\"arg_types\":[\"process\"]}", "{\"path\":\"/authentication\",\"arg_names\":[\"limit\"]}"})
    void unmatchedFiltersReturnEmptyList(String filters) throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command(filters))).andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info").isEmpty());
    }

    @Test
    void emptyListsDoNotRestrictCatalog() throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command("{\"arg_names\":[],\"arg_types\":[]}"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info.length()").value(4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"path\":\" \"}", "{\"arg_names\":[null]}", "{\"arg_types\":[\"\"]}"})
    void rejectsInvalidFilters(String filters) throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command(filters))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest
    @ValueSource(strings = {"{\"tags\":[\"auth\",\"linux\"]}",
        "{\"format_types\":{\"var_name\":\"events\",\"var_type\":\"string\"}}",
        "{\"path\":\"/auth\",\"tags\":[\"auth\"],\"format_types\":{\"var_name\":\"events\",\"var_type\":\"string\"}}"})
    void filtersMetadataWithoutExposingTags(String filters) throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command(filters))).andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info.length()").value(1))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].path").value("/auth/login"))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].tags").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"tags\":[\"missing\"]}",
        "{\"format_types\":{\"var_name\":\"events\",\"var_type\":\"integer\"}}",
        "{\"format_types\":{\"var_name\":\"missing\",\"var_type\":\"string\"}}"})
    void unmatchedMetadataReturnsEmptyCatalog(String filters) throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command(filters))).andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"tags\":[null]}", "{\"tags\":[\" \" ]}",
        "{\"format_types\":{}}", "{\"format_types\":{\"var_name\":\"events\"}}"})
    void rejectsInvalidMetadataFilters(String filters) throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command(filters))).andExpect(status().isBadRequest());
    }

    @Test
    void combinesFilterAndFieldSelectionOverHttp() throws Exception {
        endpoint().perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command("{\"tags\":[\"auth\"],\"return_format\":[\"expected_returns\"]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.th.huntflow_info[0].expected_returns.events").value("string"))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].path").doesNotExist());
    }
}