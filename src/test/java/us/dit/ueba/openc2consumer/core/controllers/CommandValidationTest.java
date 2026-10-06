package us.dit.ueba.openc2consumer.core.controllers;

import us.dit.ueba.openc2consumer.core.controllers.OpenC2Controller;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import us.dit.ueba.openc2consumer.core.actuators.Actuator;
import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Comprueba que el endpoint rechaza JSON ambiguo o invalido antes de ejecutar el comando. */
class CommandValidationTest {
    private MockMvc endpoint(OpenC2Consumer consumer) {
        return MockMvcBuilders.standaloneSetup(new OpenC2Controller(consumer, new ObjectMapper())).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "[]", "null", "{\"features\":null}",
            "{\"features\":[\"pairs\"],\"th\":{\"huntflows\":{}}}",
            "{\"features\":[\"pairs\"],\"file\":null}",
            "{\"features\":[],\"features\":[\"pairs\"]}",
            "{\"file\":{},\"device\":{}}",
            "{\"th\":{}}",
            "{\"th\":{\"huntflows\":{},\"hunt\":null}}"
    })
    void rejectsEmptyOrMultipleTargetsBeforeCallingConsumer(String target) throws Exception {
        var consumer = mock(OpenC2Consumer.class);
        endpoint(consumer).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":" + target + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(consumer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"features\":[]}", "{\"features\":[\"pairs\",\"profiles\"]}",
            "{\"th\":{\"huntflows\":{}}}", "{\"th\":{\"datasources\":\"\"}}",
            "{\"file\":{\"name\":\"example.txt\",\"path\":\"/tmp\"}}"})
    void allowsSingleTargetWithItsSpecifiers(String target) throws Exception {
        var consumer = mock(OpenC2Consumer.class);
        when(consumer.solve(any())).thenReturn(new org.oasis.openc2.lycan.OpenC2Response().setStatus(200));
        endpoint(consumer).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":" + target + "}"))
                .andExpect(status().isOk());
        verify(consumer).solve(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "{\"action\":\"not-an-action\"}",
            "{\"action\":\"query\",\"target\":42}"})
    void rejectsDeserializationErrorsBeforeCallingConsumer(String json) throws Exception {
        var consumer = mock(OpenC2Consumer.class);
        endpoint(consumer).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0").content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(consumer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"action\":\"query\"}",
            "{\"target\":{\"features\":[\"pairs\"]}}",
            "{\"action\":null,\"target\":{\"features\":[\"pairs\"]}}",
            "{\"action\":\"query\",\"target\":null}"})
    void rejectsMissingFieldsBeforeRouting(String json) throws Exception {
        var actuator = mock(Actuator.class);
        endpoint(new OpenC2Consumer(List.of(actuator))).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0").content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(actuator);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"action\":\"query\",\"target\":{\"features\":\"pairs\"}}",
            "{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":[]}}}",
            "{\"action\":\"query\",\"target\":{\"th\":{\"huntflows\":{},\"hunt\":\"userlogon\"}}}",
            "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":{}}}}",
            "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":null}}}",
            "{\"action\":\"query\",\"target\":{\"th\":{\"datasources\":\"\",\"huntflows\":{}}}}"
    })
    void rejectsMalformedTargets(String json) throws Exception {
        var consumer = mock(OpenC2Consumer.class);
        endpoint(consumer).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0").content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(consumer);
    }
    @Test
    void preservesInternalServerError() throws Exception {
        var consumer = mock(OpenC2Consumer.class);
        when(consumer.solve(any())).thenThrow(new IllegalStateException("internal failure"));
        endpoint(consumer).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]}}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.status_text").value("Error executing OpenC2 command"));
    }

    @Test
    void preservesUnsupportedCommandStatus() throws Exception {
        endpoint(new OpenC2Consumer(List.of())).perform(post("/openc2/command")
                .contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"investigate\",\"target\":{\"th\":{\"hunt\":\"userlogon\"}}}"))
                .andExpect(status().isNotImplemented()).andExpect(jsonPath("$.status").value(501));
    }
}