package us.dit.ueba.openc2consumer.profiles.th.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.oasis.openc2.lycan.profiles.th.targets.HuntflowSection;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;

/**
 * Prepara los campos de cada huntflow que se incluirán en la respuesta.
 * Usa las secciones solicitadas en return_format; si no se indican, incluye todas las disponibles.
 * Crea un mapa para cada huntflow y omite los campos cuyo valor sea null.
 */
public final class HuntflowPresentation {
    private HuntflowPresentation() {
    }

    /** Devuelve una lista de mapas con los campos elegidos para cada huntflow. */
    public static List<Map<String, Object>> selectFields(List<HuntflowInfo> huntflows,
            List<HuntflowSection> requested) {
        // Sin una selección de campos, utilizar todas las secciones definidas.
        List<HuntflowSection> sections = requested;
        if (sections == null || sections.isEmpty()) {
            sections = List.of(HuntflowSection.values());
        }
        for (HuntflowSection section : sections) {
            if (section == null) {
                throw new IllegalArgumentException("return_format must not contain null");
            }
        }
        // Crear una entrada de respuesta por cada huntflow recibido.
        List<Map<String, Object>> results = new ArrayList<>();
        for (HuntflowInfo huntflow : huntflows) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (HuntflowSection section : sections) {
                switch (section) {
                    case PATH -> add(fields, "path", huntflow.path());
                    case UNIQUE_ID -> add(fields, "uniqueId", huntflow.uniqueId());
                    case VERSION -> add(fields, "version", huntflow.version());
                    case ARGS_REQUIRED -> add(fields, "args_required", huntflow.argsRequired());
                    case EXPECTED_RETURNS -> add(fields, "expected_returns", huntflow.expectedReturns());
                    case SCRIPT -> add(fields, "script", huntflow.script());
                }
            }
            // Rechazar la selección si ninguno de los campos pedidos tiene un valor disponible.
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("Requested sections are unavailable for a selected huntflow");
            }
            results.add(fields);
        }
        return results;
    }

    /** Añade el campo al mapa únicamente si su valor no es null. */
    private static void add(Map<String, Object> fields, String name, Object value) {
        if (value != null) {
            fields.put(name, value);
        }
    }
}