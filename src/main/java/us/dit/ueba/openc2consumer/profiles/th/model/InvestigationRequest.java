package us.dit.ueba.openc2consumer.profiles.th.model;

import java.util.Map;

/**
 * Guarda el huntflow que se quiere ejecutar y los valores de sus argumentos por nombre.
 * ThreatHuntingProfile crea esta petición y la entrega a ThreatHuntingService.investigate().
 * Por ejemplo: huntflow "userlogon" y argumentos {username: "ana"}.
 */
public record InvestigationRequest(String huntflow, Map<String, Object> arguments) {

    /** Comprueba el huntflow, copia el mapa de argumentos y rechaza textos en blanco. */
    public InvestigationRequest {
        if (huntflow == null || huntflow.isBlank()) {
            throw new IllegalArgumentException("Huntflow must not be blank");
        }

        // Copiar el mapa sin permitir cambios, claves null ni valores null.
        arguments = Map.copyOf(arguments);

        // Si un valor es texto, debe contener algo más que espacios.
        for (Object value : arguments.values()) {
            if (value instanceof String text && text.isBlank()) {
                throw new IllegalArgumentException("Argument values must not be blank");
            }
        }
    }
}