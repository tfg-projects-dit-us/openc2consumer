package us.dit.ueba.openc2consumer.core.capabilities;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.oasis.openc2.lycan.types.ActionType;

/**
 * Guarda las combinaciones de accion y target que se quieren anunciar,
 * por ejemplo query con th.huntflows. Builder permite añadirlas y valida
 * los nombres y algunas combinaciones antes de crear el mapa definitivo.
 * El mapa no admite cambios ni pares duplicados. No ejecuta comandos ni
 * comprueba que las operaciones declaradas tengan una implementacion.
 */
public final class PairsMap {


    // Targets reconocidos por esta utilidad, incluidos los del perfil TH.
    private static final Set<String> ALLOWED_TARGETS = Set.of(
            "features", "file", "ip_addr", "domain_name", "process", "artifact", "device",
            "th.huntflows", "th.datasources", "th.hunt");

    private final Map<String, List<String>> internalMap;

    private PairsMap(Map<String, Set<String>> map) {
        // Guardamos una copia inmutable del mapa
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String action = entry.getKey();
            List<String> targets = List.copyOf(entry.getValue());
            copy.put(action, targets);
        }
        this.internalMap = Collections.unmodifiableMap(copy);
    }

    /**
     * Retorna una vista inmutable del mapa (Action -> List<Target>).
     */
    public Map<String, List<String>> asMap() {
        return internalMap;
    }

    /**
     * Comprueba si la acción está incluida en este mapa de capacidades.
     */
    public boolean supportsAction(String action) {
        return internalMap.containsKey(action);
    }

    /**
     * Comprueba si el par acción-target está incluido en este mapa.
     */
    public boolean supports(String action, String target) {
        List<String> targets = internalMap.get(action);
        return targets != null && targets.contains(target);
    }

    /**
     * Devuelve los targets de la acción; una lista vacía si no está registrada.
     */
    public List<String> getTargetsFor(String action) {
        return internalMap.getOrDefault(action, List.of());
    }


    /** Crea un constructor de mapas al que se pueden añadir pares. */
    public static Builder builder() {
        return new Builder();
    }

    /** Acumula pares con addPair y crea una copia inmutable con build. */
    public static class Builder {
        private final Map<String, Set<String>> mapBuilder = new LinkedHashMap<>();

        /** Añade un par utilizando una acción definida por Lycan. */
        public Builder addPairAction(ActionType action, String target) {
            if (action == null) {
                throw new IllegalArgumentException("La acción no puede ser null");
            }
            return addPair(action.toString(), target);
        }

        public Builder addPair(String action, String target) {
            validateAction(action);
            validateTarget(action, target);
            validatePair(action, target);

            // Crear la coleccion la primera vez que aparece una accion.
            if (!mapBuilder.containsKey(action)) {
                mapBuilder.put(action, new LinkedHashSet<>());
            }

            // El conjunto conserva el orden y evita targets duplicados.
            Set<String> targets = mapBuilder.get(action);
            targets.add(target);
            return this;
        }

        private void validateAction(String action) {
            for (ActionType knownAction : ActionType.values()) {
                if (knownAction.toString().equals(action)) {
                    return;
                }
            }
            throw new IllegalArgumentException("Acción OpenC2 no permitida o inválida: " + action);
        }

        private void validateTarget(String action, String target) {
            if (target == null || !ALLOWED_TARGETS.contains(target)) {
                throw new IllegalArgumentException(
                        "Target OpenC2 no permitido o inválido: '" + target + "' para la acción: " + action);
            }
        }

        // Restringe features y los targets TH; no valida todas las combinaciones OpenC2.
        private void validatePair(String action, String target) {
            boolean requiresQuery = "features".equals(target)
                    || "th.huntflows".equals(target)
                    || "th.datasources".equals(target);

            if (requiresQuery && !ActionType.QUERY.toString().equals(action)) {
                throw new IllegalArgumentException("Par no permitido: " + action + ": " + target);
            }

            if ("th.hunt".equals(target) && !ActionType.INVESTIGATE.toString().equals(action)) {
                throw new IllegalArgumentException("Par no permitido: " + action + ": " + target);
            }
        }

        /** Crea un mapa independiente de los pares que contiene el Builder en este momento. */
        public PairsMap build() {
            return new PairsMap(mapBuilder);
        }
    }

    @Override
    public String toString() {
        return "PairsMap" + internalMap;
    }
}
