package us.dit.ueba.openc2consumer.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import us.dit.ueba.openc2consumer.core.actuators.Actuator;
import us.dit.ueba.openc2consumer.core.services.OpenC2Consumer;

/** Prueba la politica local de agregacion usando actuadores simulados, incluidos errores y claves repetidas. */
class ResponseAggregationTest {
    private OpenC2Message command() throws Exception {
        return new ObjectMapper().convertValue(Map.of("action", "query", "target", Map.of("th", Map.of("huntflows", Map.of()))), OpenC2Message.class);
    }

    private Actuator actuator(OpenC2Response response) {
        Actuator actuator = mock(Actuator.class);
        when(actuator.supports(any())).thenReturn(true);
        when(actuator.getProfileName()).thenReturn("th");
        when(actuator.solve(any())).thenReturn(response);
        return actuator;
    }

    private OpenC2Response response(int status) {
        return new OpenC2Response().setStatus(status);
    }

    @Test
    void combinesResultsWithoutAddingAnotherProfileLayer() throws Exception {
        Map<String, Object> th = Map.of("huntflow_info", List.of());
        Actuator first = actuator(response(200).addResults("th", th));
        Actuator second = actuator(response(200).addResults("x-other", Map.of("value", 1)));
        OpenC2Response result = new OpenC2Consumer(List.of(first, second)).solve(command());
        assertEquals(200, result.getStatus());
        assertEquals(Map.of("th", th, "x-other", Map.of("value", 1)), result.getResults());
    }

    @Test
    void conflictingResultsAreNotSilentlyOverwritten() throws Exception {
        Actuator first = actuator(response(200).addResults("th", Map.of("value", 1)));
        Actuator second = actuator(response(200).addResults("th", Map.of("value", 2)));
        for (List<Actuator> order : List.of(List.of(first, second), List.of(second, first))) {
            OpenC2Response result = new OpenC2Consumer(order).solve(command());
            assertEquals(500, result.getStatus());
            assertTrue(result.getResults() == null || result.getResults().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {102, 400, 401, 403, 404, 500, 501, 503})
    void preservesAgreedStatusInsteadOfInventing207(int status) throws Exception {
        OpenC2Response result = new OpenC2Consumer(List.of(
                actuator(response(status)), actuator(response(status)))).solve(command());
        assertEquals(status, result.getStatus());
    }

    @Test
    void mixedStatesHaveSameOutcomeRegardlessOfOrder() throws Exception {
        Actuator success = actuator(response(200).addResults("th", Map.of("value", 1)));
        Actuator failure = actuator(response(404));
        for (List<Actuator> order : List.of(List.of(success, failure), List.of(failure, success))) {
            OpenC2Response result = new OpenC2Consumer(order).solve(command());
            assertEquals(500, result.getStatus());
            assertTrue(result.getResults() == null || result.getResults().isEmpty());
        }
    }

    @Test
    void exceptionDoesNotPreventOtherActuatorsFromExecuting() throws Exception {
        Actuator failing = actuator(null);
        when(failing.solve(any())).thenThrow(new IllegalStateException());
        Actuator other = actuator(response(200));
        assertEquals(500, new OpenC2Consumer(List.of(failing, other)).solve(command()).getStatus());
        verify(other).solve(any());
    }

    @Test
    void rejectsNullMissingAndUnknownStatuses() throws Exception {
        for (OpenC2Response invalid : new OpenC2Response[] {null, new OpenC2Response(), response(207)}) {
            assertEquals(500, new OpenC2Consumer(List.of(
                    actuator(invalid), actuator(response(200)))).solve(command()).getStatus());
        }
    }

    @Test
    void singleActuatorResponseIsUnchanged() throws Exception {
        OpenC2Response original = response(404).setStatusText("Not found");
        assertSame(original, new OpenC2Consumer(List.of(actuator(original))).solve(command()));
    }
}