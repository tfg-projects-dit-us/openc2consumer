package us.dit.ueba.openc2consumer.core.capabilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;
import org.oasis.openc2.lycan.args.Args;
import org.oasis.openc2.lycan.types.ActionType;
import org.oasis.openc2.lycan.types.ArgsResponseType;
import org.oasis.openc2.lycan.types.FeatureType;

import us.dit.ueba.openc2consumer.core.actuators.Actuator;
import org.oasis.openc2.lycan.types.StatusCodeType;

/**
 * Responde a query features con la versión, los perfiles y los pares disponibles.
 * Reúne las capacidades anunciadas por los actuadores y las del propio Consumer.
 */
public final class ConsumerCapabilities {
    private final List<Actuator> registeredActuators;

    public ConsumerCapabilities(List<Actuator> registeredActuators) {
        this.registeredActuators = registeredActuators;
    }
    /**
     * Responde con las capacidades solicitadas: versión, perfiles o pares acción-target.
     * Antes comprueba los argumentos y que los nombres solicitados sean conocidos.
     */
    public OpenC2Response queryFeatures(OpenC2Message command) {
        OpenC2Response response = new OpenC2Response();
        if (!hasValidFeatureArguments(command.getArgs())) {
            response.setStatusCode(StatusCodeType.BAD_REQUEST);
            response.setStatusText(
                    "Bad Request: with the pair query:features argument must be omitted or only 'response_requested' with value 'complete' is allowed");
            return response;
        }

        List<String> requestedNames = command.getTarget().getFeatures().getFeatures();
        List<FeatureType> requestedFeatures = new ArrayList<>();

        // Si algún nombre es desconocido, rechazar la consulta completa.
        for (String name : requestedNames) {
            FeatureType feature = findFeatureType(name);
            if (feature == null) {
                response.setStatusCode(StatusCodeType.BAD_REQUEST);
                response.setStatusText("Bad Request: unknown feature: " + name);
                return response;
            }
            requestedFeatures.add(feature);
        }

        for (FeatureType feature : requestedFeatures) {
            switch (feature) {
                case VERSIONS:
                    response.addResults("versions", List.of("1.0"));
                    break;
                case PROFILES:
                    response.addResults("profiles", getSupportedProfiles());
                    break;
                case PAIRS:
                    response.addResults("pairs", getSupportedPairs());
                    break;
                case RATE_LIMIT:
                    // Omitir rate_limit porque la aplicación no tiene un límite configurado.
                    break;
            }
        }
        response.setStatusCode(StatusCodeType.OK);
        response.setStatusText("Query features executed successfully");
        return response;
    }

    /**
     * Permite omitir args o pedir una respuesta complete.
     * Rechaza los tiempos de ejecución y los argumentos del perfil TH.
     */
    private boolean hasValidFeatureArguments(Args args) {
        if (args == null) {
            return true;
        }
        if (!ArgsResponseType.COMPLETE.toString().equalsIgnoreCase(args.getResponseRequested())) {
            return false;
        }
        if (args.getStartTime() != null || args.getStopTime() != null
                || args.getDuration() != null || args.getTh() != null) {
            return false;
        }
        return true;
    }

    /** Busca la capacidad por su nombre; devuelve null si no la encuentra. */
    private FeatureType findFeatureType(String name) {
        for (FeatureType feature : FeatureType.values()) {
            if (feature.toString().equals(name)) {
                return feature;
            }
        }
        return null;
    }

    /** Recoge los nombres de los perfiles registrados que tienen nombre informado. */
    private List<String> getSupportedProfiles() {
        List<String> profiles = new ArrayList<>();
        for (Actuator actuator : registeredActuators) {
            String profile = actuator.getProfileName();
            if (profile != null && !profile.isEmpty()) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    /**
     * Agrupa los targets anunciados por cada acción, sin repetirlos.
     * Por ejemplo: query -> [features, th.huntflows].
     */
    private Map<String, List<String>> getSupportedPairs() {
        // Usar conjuntos para guardar cada target una sola vez y mantener su orden.
        Map<String, Set<String>> uniquePairs = new LinkedHashMap<>();
        // Añadir la consulta de capacidades, que siempre atiende el Consumer.
        uniquePairs.put(ActionType.QUERY.toString(), new LinkedHashSet<>(List.of("features")));

        for (Actuator actuator : registeredActuators) {
            Map<String, List<String>> actuatorPairs = actuator.getSupportedPairs();
            for (String action : actuatorPairs.keySet()) {
                if (!uniquePairs.containsKey(action)) {
                    uniquePairs.put(action, new LinkedHashSet<>());
                }
                Set<String> targets = uniquePairs.get(action);
                targets.addAll(actuatorPairs.get(action));
            }
        }

        // Convertir los conjuntos en las listas que se enviarán en la respuesta.
        Map<String, List<String>> pairs = new LinkedHashMap<>();
        for (String action : uniquePairs.keySet()) {
            pairs.put(action, List.copyOf(uniquePairs.get(action)));
        }
        return pairs;
    }

}
