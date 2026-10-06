package us.dit.ueba.openc2consumer.core.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;
import org.oasis.openc2.lycan.types.ActionType;
import org.oasis.openc2.lycan.types.StatusCodeType;


import org.oasis.openc2.lycan.targets.Features;

import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.core.actuators.Actuator;
import us.dit.ueba.openc2consumer.core.capabilities.ConsumerCapabilities;

/**
 * Decide quién atiende los comandos que llegan desde el controlador.
 * Responde directamente a query features, que consulta las capacidades del Consumer.
 * Los demás comandos se envían a los actuadores de los perfiles correspondientes.
 * Cuando intervienen varios actuadores, combina sus respuestas si son compatibles.
 */
@Service
public class OpenC2Consumer {
    private final List<Actuator> registeredActuators;
    private final ConsumerCapabilities capabilities;

    // Spring entrega la lista de actuadores disponibles al crear este servicio.
    public OpenC2Consumer(List<Actuator> registeredActuators) {
        this.registeredActuators = registeredActuators;
        this.capabilities = new ConsumerCapabilities(registeredActuators);
    }

    /** Comprueba que haya acción y target, y busca quién puede atender el comando. */
    public OpenC2Response solve(OpenC2Message command) {
        if (command == null || command.getAction() == null || command.getAction().isBlank()
                || command.getTarget() == null) {
            return new OpenC2Response().setStatusCode(StatusCodeType.BAD_REQUEST)
                    .setStatusText("Bad Request: action and target are required");
        }

        // La consulta de capacidades la responde el propio Consumer.
        Features features = command.getTarget().getFeatures();
        if (ActionType.QUERY.toString().equals(command.getAction()) && features != null && features.getFeatures() != null) {
            return capabilities.queryFeatures(command);
        }

        // Preguntar a cada actuador si reconoce la acción y el target recibidos.
        List<Actuator> matchingActuators = new ArrayList<>();
        for (Actuator actuator : registeredActuators) {
            if (actuator.supports(command)) {
                matchingActuators.add(actuator);
            }
        }

        if (matchingActuators.isEmpty()) {
            return new OpenC2Response().setStatusCode(StatusCodeType.NOT_IMPLEMENTED)
                    .setStatusText("Not Implemented: No actuator registered for this target/action");
        }
        // Con un solo actuador, devolver su respuesta tal como la ha generado.
        if (matchingActuators.size() == 1) {
            Actuator actuator = matchingActuators.get(0);
            return actuator.solve(command);
        }
        return aggregateResponses(matchingActuators, command);
    }

    /**
     * Envía el comando a cada actuador seleccionado y devuelve una única respuesta.
     * Si todos devuelven el mismo estado, conserva ese estado.
     * Si todos responden con 200, reúne sus resultados sin permitir claves repetidas.
     * Si los estados son distintos o se repiten claves en los resultados, devuelve 500.
     * Una respuesta inválida o una excepción se cuenta como un estado 500.
     */
    private OpenC2Response aggregateResponses(List<Actuator> actuators, OpenC2Message command) {
        Set<Integer> statuses = new LinkedHashSet<>();
        List<String> details = new ArrayList<>();
        Map<String, Object> combinedResults = new LinkedHashMap<>();
        boolean conflictingResults = false;

        for (Actuator actuator : actuators) {
            try {
                OpenC2Response response = actuator.solve(command);
                if (response == null || !isDefinedStatus(response.getStatus())) {
                    statuses.add(StatusCodeType.INTERNAL_ERROR.getValue());
                    details.add("Invalid response from actuator");
                    continue;
                }

                statuses.add(response.getStatus());
                if (response.getStatusText() != null) {
                    details.add(actuator.getProfileName() + ": " + response.getStatusText());
                }

                // Copiar las claves de results directamente a la respuesta conjunta.
                if (response.getStatus() == StatusCodeType.OK.getValue() && response.getResults() != null) {
                    for (Map.Entry<String, Object> result : response.getResults().entrySet()) {
                        if (combinedResults.containsKey(result.getKey())) {
                            conflictingResults = true;
                        } else {
                            combinedResults.put(result.getKey(), result.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                // Registrar el fallo y continuar con los demás actuadores.
                statuses.add(StatusCodeType.INTERNAL_ERROR.getValue());
                details.add("Actuator execution failed");
            }
        }

        if (statuses.size() != 1 || conflictingResults) {
            return new OpenC2Response().setStatusCode(StatusCodeType.INTERNAL_ERROR)
                    .setStatusText("Cannot combine actuator responses: different statuses or conflicting result fields");
        }

        int status = statuses.iterator().next();
        OpenC2Response response = new OpenC2Response().setStatus(status);
        if (!details.isEmpty()) {
            response.setStatusText(String.join("; ", details));
        }
        if (status == StatusCodeType.OK.getValue() && !combinedResults.isEmpty()) {
            response.setResults(combinedResults);
        }
        return response;
    }

    /** Comprueba si el código pertenece a la lista de estados de OpenC2 1.0. */
    private boolean isDefinedStatus(Integer status) {
        if (status == null) {
            return false;
        }
        for (StatusCodeType code : StatusCodeType.values()) {
            if (code.getValue() == status) {
                return true;
            }
        }
        return false;
    }
}