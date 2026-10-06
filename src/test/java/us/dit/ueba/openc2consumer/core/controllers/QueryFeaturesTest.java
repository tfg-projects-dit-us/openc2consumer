package us.dit.ueba.openc2consumer.core.controllers;

import us.dit.ueba.openc2consumer.core.controllers.OpenC2Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import us.dit.ueba.openc2consumer.core.actuators.Actuator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import us.dit.ueba.openc2consumer.profiles.th.ThreatHuntingProfile;
import us.dit.ueba.openc2consumer.profiles.th.catalog.JsonThreatHuntingService;
import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Comprobamos el endpoint sin iniciar Spring ni conectar con Velociraptor. */
class QueryFeaturesTest {
    private MockMvc mvc;

    @BeforeEach
    void prepare() {

        var objectMapper = new ObjectMapper();
        var consumer = new OpenC2Consumer(List.of(new ThreatHuntingProfile(new JsonThreatHuntingService(objectMapper))));
        var controller = new OpenC2Controller(consumer, objectMapper);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"response_requested\":\"complete\"}"})
    void acceptsAllowedArguments(String args) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]},\"args\":" + args + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.pairs.query[0]").value("features"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"start_time\":1", "\"stop_time\":2", "\"duration\":60", "\"th\":{}"})
    void rejectsAdditionalArgumentsWithOrWithoutComplete(String argument) throws Exception {
        for (String prefix : List.of("", "\"response_requested\":\"complete\",")) {
            mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                    .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]},\"args\":{"
                            + prefix + argument + "}}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.results.pairs").doesNotHaveJsonPath());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "ack", "status"})
    void rejectsOtherResponseModes(String mode) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]},"
                        + "\"args\":{\"response_requested\":\"" + mode + "\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsVersionsAsArray() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"versions\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.versions").isArray())
                .andExpect(jsonPath("$.results.versions.length()").value(1))
                .andExpect(jsonPath("$.results.versions[0]").value("1.0"));
    }

    @Test
    void omitsUndefinedRateLimit() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"rate_limit\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.rate_limit").doesNotHaveJsonPath());
    }

    @Test
    void returnsOtherFeaturesWhenRateLimitIsUndefined() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"rate_limit\",\"pairs\",\"profiles\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results.rate_limit").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.results.pairs.query[0]").value("features"))
                .andExpect(jsonPath("$.results.profiles[0]").value("th"));
    }

    @Test
    void includesConsumerPairsWithoutActuators() throws Exception {
        var controller = new OpenC2Controller(new OpenC2Consumer(List.of()), new ObjectMapper());
        var endpoint = MockMvcBuilders.standaloneSetup(controller).build();
        endpoint.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.pairs.query.length()").value(1))
                .andExpect(jsonPath("$.results.pairs.query[0]").value("features"));
    }

    @Test
    void mergesPairsWithoutDuplicatesOrChangingActuators() throws Exception {
        var first = mock(Actuator.class);
        var second = mock(Actuator.class);
        var mutableTargets = new ArrayList<>(List.of("th.hunt", "th.hunt"));
        var firstPairs = Map.of("query", List.of("features", "th.huntflows"),
                "investigate", mutableTargets);
        var secondPairs = Map.of("query", List.of("th.huntflows", "th.datasources"),
                "investigate", List.of("th.hunt"));
        when(first.getSupportedPairs()).thenReturn(firstPairs);
        when(second.getSupportedPairs()).thenReturn(secondPairs);
        var consumer = new OpenC2Consumer(List.of(first, second));
        var endpoint = MockMvcBuilders.standaloneSetup(
                new OpenC2Controller(consumer, new ObjectMapper())).build();

        for (int request = 0; request < 2; request++) {
            endpoint.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                    .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\"]}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.results.pairs.query.length()").value(3))
                    .andExpect(jsonPath("$.results.pairs.query[0]").value("features"))
                    .andExpect(jsonPath("$.results.pairs.query[1]").value("th.huntflows"))
                    .andExpect(jsonPath("$.results.pairs.query[2]").value("th.datasources"))
                    .andExpect(jsonPath("$.results.pairs.investigate.length()").value(1))
                    .andExpect(jsonPath("$.results.pairs.investigate[0]").value("th.hunt"));
        }
        assertEquals(List.of("features", "th.huntflows"), firstPairs.get("query"));
        assertEquals(List.of("th.hunt", "th.hunt"), mutableTargets);
        assertEquals(List.of("th.huntflows", "th.datasources"), secondPairs.get("query"));
        verify(first, never()).solve(any());
        verify(second, never()).solve(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pairs", "versions", "profiles"})
    void acceptsProtocolFeatureNames(String feature) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"" + feature + "\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.results." + feature).exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "PAIRS", "", "rate_limits"})
    void rejectsUnknownFeatureNames(String feature) throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"pairs\",\"" + feature + "\"]}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.results.pairs").doesNotExist());
    }

    @Test
    void listsRegisteredProfiles() throws Exception {
        mvc.perform(post("/openc2/command").contentType("application/openc2+json;version=1.0")
                .content("{\"action\":\"query\",\"target\":{\"features\":[\"profiles\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.profiles.length()").value(1))
                .andExpect(jsonPath("$.results.profiles[0]").value("th"));
    }
}