package us.dit.ueba.openc2consumer.profiles.th;

import java.util.List;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationRequest;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationResult;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;
import us.dit.ueba.openc2consumer.profiles.th.model.DatasourceInfo;

/**
 * Define las operaciones que ThreatHuntingProfile necesita para consultar catálogos e investigar.
 * Utiliza los modelos del perfil TH, sin incluir consultas VQL ni conexiones con Velociraptor.
 * Cada implementación decide cómo obtiene los datos y ejecuta las investigaciones.
 */
public interface ThreatHuntingService {
    /** Devuelve todos los huntflows disponibles para que el perfil aplique los filtros. */
    List<HuntflowInfo> listHuntflows();

    /** Devuelve todas las fuentes si el nombre está vacío, o las que coincidan exactamente con él. */
    List<DatasourceInfo> listDatasources(String name);

    /**
     * Recibe los datos de la investigacion y devuelve sus hallazgos finales.
     * Ejemplo: investigar "userlogon" para el usuario "alice".
     * Lanza InvestigationException si no puede completar la investigacion.
     */
    InvestigationResult investigate(InvestigationRequest request);

    /**
     * Indica si esta implementación permite ejecutar investigaciones.
     * Devuelve false por defecto; una implementación que las permita debe devolver true.
     * El perfil utiliza este valor para anunciarlas y admitir su ejecución.
     */
    default boolean supportsInvestigations() {
        return false;
    }
}