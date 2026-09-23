package us.dit.ueba.openc2consumer.actuators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;
import org.oasis.openc2.lycan.targets.Features;
import org.oasis.openc2.lycan.targets.Target;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Responde las consultas estáticas del perfil threatHunting y valida
 * investigate userlogon.
 * Una investigación válida devuelve 501 porque su ejecución está pendiente.
 * El controlador se encarga del HTTP; este servicio no depende de Velociraptor.
 */
@Service
public class ThreatHuntingProfile extends ActuatorCommons {
    private final ObjectMapper objectMapper;

    public ThreatHuntingProfile(ObjectMapper objectMapper) {
        super("th");
        this.supportedPairs = new HashMap<>();
        // Ahora mismo se pone en el código, todos los pares acción/objetivo que son
        // obligatorios para el perfil th
        // Pero lo ideal es que se lea por configuraicón
        supportedPairs.put("query", Arrays.asList("features", "th.huntflows"));
        supportedPairs.put("investigate", Arrays.asList("th.hunt"));
        this.objectMapper = objectMapper;
    }

    /**
     * Reconoce targets del perfil; la acción y los argumentos se validan en handle.
     */
    public boolean supports(OpenC2Message command) {
        boolean isThreatHunting = false;
        String action = command.getAction();
        Target target = command.getTarget();
        Features features = target.getFeatures();
        List<String> featuresList = features.getFeatures();
        // Esto ya se puede hacer usando supportedPairs, por ahora lo dejo
        isThreatHunting = action.equals("query") && featuresList.contains("ht:huntflows")
                || action.equals("query") && featuresList.contains("ht:hunt")
                || action.equals("query") && featuresList.contains("ht:datasources")
                || action.equals("investigate") && featuresList.contains("ht:hunt");

        return isThreatHunting;
    }

    public OpenC2Response solve(OpenC2Message message) {
        OpenC2Response response = null;

        return response;
    }

    /** Enruta por target a la consulta o a la validación de investigate. */
    private OpenC2Response handle(OpenC2Message command) {
        if (!supports(command)) {
            throw new IllegalArgumentException("Unsupported Threat Hunting command");
        }
        if (command.getAction().equals("query") && command.getTarget().getFeatures() != null) {
            return queryFeatures(command);
        }
        if (command.getAction().equals("query") && command.getTarget().getThHuntflow() != null) {
            return queryHuntflows(command);
        }
        if (command.getAction().equals("query") && command.getTarget().getThDatasource() != null) {
            return queryDatasources(command);
        }
        return investigateHunt(command);
    }

    /**
     * Primer paso: consultar las parejas action/target disponibles.
     * Por ahora solo admitimos features: ["pairs"].
     */
    private OpenC2Response queryFeatures(OpenC2Message command) {
        String action = command.getAction();
        Target target = command.getTarget();
        Features features = target.getFeatures();
        OpenC2Response response = new OpenC2Response();
        int status = 200;

        // Solo query puede consultar features y el objetivo debe ser único.
        if (!command.getAction().equals("query") && command.getTarget().hasFeatures()) {
            status = 400;
            response.setStatus(status);
            response.setStatusText("Query features command invalid");

        }
        return response;
    }

    /**
     * Consultamos la definición inicial de userlogon, sin ejecutar la
     * investigación.
     * Más adelante añadiremos otras definiciones y filtros de búsqueda.
     */
    private OpenC2Response queryHuntflows(OpenC2Message command) {
        String action = command.getAction();
        Target target = command.getTarget();
        Features features = target.getFeatures();
        OpenC2Response response = new OpenC2Response();
        return response;
    }

    /**
     * Devuelve la definición inicial de la fuente de registros de los equipos.
     * Esta lista es estática: no comprueba la disponibilidad real de Velociraptor.
     * Por ahora solo admitimos datasources: "" para consultar todas las fuentes.
     */
    private OpenC2Response queryDatasources(OpenC2Message command) {
        String action = command.getAction();
        Target target = command.getTarget();
        Features features = target.getFeatures();
        OpenC2Response response = new OpenC2Response();
        return response;
    }

    /**
     * Validamos la petición de userlogon sin ejecutarla todavía.
     * string_args utiliza la convención propia nombre=valor.
     */
    private OpenC2Response investigateHunt(OpenC2Message command) {
        String action = command.getAction();
        Target target = command.getTarget();
        Features features = target.getFeatures();
        OpenC2Response response = new OpenC2Response();
        return response;
    }
}
