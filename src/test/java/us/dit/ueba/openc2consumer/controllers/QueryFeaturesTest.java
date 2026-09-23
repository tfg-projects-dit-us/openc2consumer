package us.dit.ueba.openc2consumer.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import us.dit.ueba.openc2consumer.profiles.ThreatHuntingService;
import us.dit.ueba.openc2consumer.services.vql.VqlInterface;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Comprobamos el endpoint sin iniciar Spring ni conectar con Velociraptor. */
class QueryFeaturesTest {
    private MockMvc mvc;
    private VqlInterface vql;

    @BeforeEach
    void prepare() {
        var controller = new OpenC2Controller();
        vql = mock(VqlInterface.class);
        ReflectionTestUtils.setField(controller, "vqlService", vql);
        var objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(controller, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(controller, "threatHuntingService",
                new ThreatHuntingService(objectMapper));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void queryPairsDoesNotExecuteVql() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.pairs.query[0]").value("features"))
                .andExpect(jsonPath("$.results.pairs.query[1]").value("/huntflows"))
                .andExpect(jsonPath("$.results.pairs.query[2]").value("/datasources"))
                .andExpect(jsonPath("$.results.pairs.investigate").doesNotExist())
                .andExpect(jsonPath("$.results.profiles").doesNotExist());
        verifyNoInteractions(vql);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"action\":\"investigate\",\"target\":{\"features\":[\"pairs\"]}}",
        "{\"action\":\"query\",\"target\":{\"features\":\"pairs\"}}",
        "{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"],\"user_account\":{\"username\":\"ana\"}}}"
    })
    void invalidFeaturesNeverExecuteVql(String command) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(vql);
    }

    @Test
    void unsupportedFeatureIsExplicit() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"profiles\"]}}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value(501));
        verifyNoInteractions(vql);
    }

    @Test
    void listsUserlogonWithoutExecutingVql() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{}}}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.th.huntflow_info.length()").value(1))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].path").value("userlogon"))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].args_required.username").value("string"))
                .andExpect(jsonPath("$.results.th.huntflow_info[0].script").doesNotExist());
        verifyNoInteractions(vql);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"action\":\"investigate\",\"target\":{\"th\":{\"huntflows\":{}}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":[]}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{},\"hunt\":\"userlogon\"}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{}},\"user_account\":{\"username\":\"ana\"}}}"
    })
    void invalidHuntflowsNeverExecuteVql(String command) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(vql);
    }

    @Test
    void rejectsUnimplementedHuntflowFilters() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{\"tags\":[\"logon\"]}}}}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value(501));
        verifyNoInteractions(vql);
    }

    @Test
    void listsDatasourcesWithoutExecutingVql() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"\"}}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.th.datasources.length()").value(1))
                .andExpect(jsonPath("$.results.th.datasources[0].ds_name").value("endpoint_logs"))
                .andExpect(jsonPath("$.results.th.datasources[0].ds_tags[0]").value("authentication"));
        verifyNoInteractions(vql);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"action\":\"investigate\",\"target\":{\"th\":{\"datasources\":\"\"}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":{}}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":null}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"\",\"huntflows\":{}}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"\"},\"user_account\":{\"username\":\"ana\"}}}"
    })
    void rejectsInvalidDatasources(String command) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(vql);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"endpoint_logs\"}}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"\"}},\"args\":{}}",
        "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"\"}},\"actuator\":{\"th\":{}}}"
    })
    void rejectsUnimplementedDatasourceOptions(String command) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command)).andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value(501));
        verifyNoInteractions(vql);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STANDARD", "SUSPICIOUS", "CRITICAL"})
    void validatesHuntWithoutExecutingIt(String level) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(huntCommand("[\"username=ana\",\"vigilance_level=" + level + "\"]")))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status_text").value(
                        "Valid userlogon request (vigilance_level=" + level + "). Execution is not implemented yet"));
        verifyNoInteractions(vql);
    }

    @Test
    void defaultsHuntLevelToStandard() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(huntCommand("[\"username=ana=admin\"]")))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status_text").value(
                        "Valid userlogon request (vigilance_level=STANDARD). Execution is not implemented yet"));
        verifyNoInteractions(vql);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[\"username= \"]", "[\"username=ana\",\"username=bob\"]",
        "[\"username=ana\",\"vigilance_level=HIGH\"]", "[\"username=ana\",\"other=x\"]",
        "[\"username=ana\",\"vigilance_level=STANDARD\",\"vigilance_level=CRITICAL\"]",
        "[42]", "[null]", "[\"ana\"]", "null", "{}"})
    void rejectsInvalidHuntParameters(String arguments) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(huntCommand(arguments))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(vql);
    }

    @Test
    void rejectsWrongHuntAction() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(huntCommand("[\"username=ana\"]").replace("investigate", "query")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(vql);
    }

    @Test
    void rejectsUnsupportedHuntflow() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(huntCommand("[\"username=ana\"]").replace("userlogon", "userlogout")))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status_text").value("Huntflow not supported"));
        verifyNoInteractions(vql);
    }

    @Test
    void doesNotIgnoreTimeFilters() throws Exception {
        String command = huntCommand("[\"username=ana\"]")
                .replace("\"string_args\":", "\"timeranges\":[],\"string_args\":");
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content(command)).andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status_text").value("Additional hunt options are not supported yet"));
        verifyNoInteractions(vql);
    }

    private String huntCommand(String arguments) {
        return "{\"action\":\"investigate\",\"target\":{\"th\":{\"hunt\":\"userlogon\"}},"
                + "\"args\":{\"th\":{\"huntargs\":{\"string_args\":" + arguments + "}}}}";
    }
}
