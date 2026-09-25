package us.dit.ueba.openc2consumer.controllers;

import java.util.Set;

/**
 * esto es sólo un aidea.... tener un tipo específico para guardar los pares de cada actuador o del consumidor completo
 * y que se verifique que son válidos al meterlos
 * NO ESTÁ VERIFICADO NI NADA, SÓLO LA IDEA
 * Representa la asociación válida entre Acciones OpenC2 (Actions) y sus Targets permitidos.
 * Garantiza la integridad e inmutabilidad de la estructura de pares.
 */
public final class PairsMap {

    // Conjunto de Acciones (Actions) válidas en OpenC2
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
        "scan", "locate", "query", "investigate", "remediate", 
        "deny", "allow", "contain", "cancel", "delete", "update"
    );

    // Conjunto de Targets válidos en OpenC2
    private static final Set<String> ALLOWED_TARGETS = Set.of(
        "features", "file", "ip_addr", "domain_name", "process", "artifact", "device"
    );

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

    public static class Builder {
        private final Map<String, List<String>> mapBuilder = new HashMap<>();

        public Builder addPair(String action, String... targets) {
            return addPair(action, Arrays.asList(targets));
        }

        public Builder addPair(String action, List<String> targets) {
            // 1. Validar acción
            if (!ALLOWED_ACTIONS.contains(action)) {
                throw new IllegalArgumentException(
                    "Acción OpenC2 no permitida o inválida: " + action
                );
            }

            // 2. Validar targets
            for (String target : targets) {
                if (!ALLOWED_TARGETS.contains(target)) {
                    throw new IllegalArgumentException(
                        "Target OpenC2 no permitido o inválido: '" + target + "' para la acción: " + action
                    );
                }
            }

            // 3. Insertar o acumular sin duplicados
            mapBuilder.computeIfAbsent(action, k -> new ArrayList<>())
                      .addAll(targets);

            return this;
        }

        public PairsMap build() {
            return new PairsMap(mapBuilder);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairsMap pairsMap = (PairsMap) o;
        return Objects.equals(internalMap, pairsMap.internalMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalMap);
    }

    @Override
    public String toString() {
        return "PairsMap" + internalMap;
    }
}
