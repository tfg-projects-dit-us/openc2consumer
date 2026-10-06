package us.dit.ueba.openc2consumer.profiles.th.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Guarda los hallazgos que devuelve el servicio al terminar una investigación.
 * Cada hallazgo contiene campos de texto, por ejemplo {event: "login", username: "ana"}.
 * La lista puede estar vacía si no hay hallazgos.
 * ThreatHuntingProfile convierte estos datos en la respuesta OpenC2.
 */
public record InvestigationResult(List<Map<String, String>> findings) {

    /** Copia los hallazgos y la lista para impedir que se modifiquen después. */
    public InvestigationResult {
        List<Map<String, String>> copiedFindings = new ArrayList<>();

        // Crear una copia de cada mapa que no admita cambios ni claves o valores null.
        for (Map<String, String> finding : findings) {
            copiedFindings.add(Map.copyOf(finding));
        }

        // Guardar una copia de la lista que tampoco admita cambios.
        findings = List.copyOf(copiedFindings);
    }
}