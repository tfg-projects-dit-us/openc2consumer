package us.dit.ueba.openc2consumer.profiles.th.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Guarda la información de un huntflow del catálogo TH.
 * La ruta lo identifica; los demás datos pueden estar ausentes.
 * Copia los mapas y las etiquetas para que no se puedan modificar después.
 * Las etiquetas se leen del catálogo y sirven para filtrar, pero no se incluyen en el JSON de respuesta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HuntflowInfo(
        String path,

        // Nombre y tipo de cada argumento que necesita el huntflow.
        @JsonProperty("args_required") Map<String, String> argsRequired,

        Long uniqueId,
        String version,

        // Nombre y tipo de cada resultado que se espera obtener.
        @JsonProperty("expected_returns") Map<String, String> expectedReturns,

        String script,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) List<String> tags) {

    /** Crea un huntflow con su ruta y sus argumentos; deja los demás datos en null. */
    public HuntflowInfo(String path, Map<String, String> argsRequired) {
        this(path, argsRequired, null, null, null, null, null);
    }

    /** Crea un huntflow con los datos recibidos y sin etiquetas. */
    public HuntflowInfo(String path, Map<String, String> argsRequired, Long uniqueId,
            String version, Map<String, String> expectedReturns, String script) {
        this(path, argsRequired, uniqueId, version, expectedReturns, script, null);
    }

    /** Se ejecuta al crear el objeto, también cuando se carga desde JSON. */
    public HuntflowInfo {
        tags = copyTags(tags);

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Huntflow path must not be blank");
        }
        if (uniqueId != null && uniqueId < 0) {
            throw new IllegalArgumentException("Huntflow uniqueId must not be negative");
        }

        // Mantener null si no hay datos; si los hay, guardar copias que no admiten cambios.
        if (argsRequired != null) {
            argsRequired = Map.copyOf(argsRequired);
        }
        if (expectedReturns != null) {
            expectedReturns = Map.copyOf(expectedReturns);
        }
    }

    /** Copia las etiquetas y rechaza valores null o textos en blanco. */
    private static List<String> copyTags(List<String> tags) {
        if (tags == null) {
            return null;
        }
        List<String> copiedTags = List.copyOf(tags);
        for (String tag : copiedTags) {
            if (tag.isBlank()) {
                throw new IllegalArgumentException("Huntflow tags must not be blank");
            }
        }
        return copiedTags;
    }
}