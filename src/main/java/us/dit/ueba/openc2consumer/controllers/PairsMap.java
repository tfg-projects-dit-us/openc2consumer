package us.dit.ueba.openc2consumer.controllers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * esto es sólo un aidea.... tener un tipo específico para guardar los pares de
 * cada actuador o del consumidor completo
 * y que se verifique que son válidos al meterlos
 * NO ESTÁ VERIFICADO NI NADA, SÓLO LA IDEA
 * Representa la asociación válida entre Acciones OpenC2 (Actions) y sus Targets
 * permitidos.
 * Garantiza la integridad e inmutabilidad de la estructura de pares.
 */
public final class PairsMap {

    // Conjunto de Acciones (Actions) válidas en OpenC2
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "scan", "locate", "query", "investigate", "remediate",
            "deny", "allow", "contain", "cancel", "delete", "update");

    // Conjunto de Targets válidos en OpenC2
    private static final Set<String> ALLOWED_TARGETS = Set.of(
            "features", "file", "ip_addr", "domain_name", "process", "artifact", "device");

    private final Map<String, List<String>> internalMap;

    private PairsMap(Map<String, List<String>> map) {
        // Guardamos una copia inmutable del mapa
        Map<String, List<String>> copy = new HashMap<>();
        map.forEach((action, targets) -> copy.put(action, List.copyOf(targets)));
        this.internalMap = Collections.unmodifiableMap(copy);
    }

    /**
     * Retorna una vista inmutable del mapa (Action -> List<Target>).
     */
    public Map<String, List<String>> asMap() {
        return internalMap;
    }

    /**
     * Comprueba si una acción específica está soportada.
     */
    public boolean supportsAction(String action) {
        return internalMap.containsKey(action);
    }

    /**
     * Comprueba si la combinación Acción + Target es atendida.
     */
    public boolean supports(String action, String target) {
        List<String> targets = internalMap.get(action);
        return targets != null && targets.contains(target);
    }

    /**
     * Obtiene los targets asociados a una acción.
     */
    public List<String> getTargetsFor(String action) {
        return internalMap.getOrDefault(action, List.of());
    }

    // =========================================================================
    // Builder para construir instancias validando reglas de dominio
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    /**
     * PAra construir un nuevo PairsMap tengo que llamar a addPair para cada par del
     * mapa
     * y luego llamo a build para obtener el objeto PairsMap
     */
    public static class Builder {
        private final Map<String, List<String>> mapBuilder = new HashMap<>();

        public Builder addPair(String action, String target) {
            // 1. Validar acción
            if (!ALLOWED_ACTIONS.contains(action)) {
                throw new IllegalArgumentException(
                        "Acción OpenC2 no permitida o inválida: " + action);
            }
            if (!ALLOWED_TARGETS.contains(target)) {
                throw new IllegalArgumentException(
                        "Target OpenC2 no permitido o inválido: '" + target + "' para la acción: " + action);
            }
            // Si la acción ya está pero el target
            if (mapBuilder.containsKey(action) && !mapBuilder.get(action).contains(target)) {
                mapBuilder.get(action).add(target);
            } else {
                mapBuilder.put(action, List.of(target));
            }
            return this;
        }

        public PairsMap build() {
            return new PairsMap(mapBuilder);
        }
    }

    @Override
    public String toString() {
        return "PairsMap" + internalMap;
    }
}
