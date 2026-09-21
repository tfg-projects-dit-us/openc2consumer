package us.dit.ueba.openc2consumer.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * Responde las consultas estáticas del perfil y valida investigate userlogon.
 * Una investigación válida devuelve 501 porque su ejecución está pendiente.
 * El controlador se encarga del HTTP; este servicio no depende de Velociraptor.
 */
@Service
public class ThreatHuntingService implements ActuatorProfile{
    private final ObjectMapper objectMapper;

    public ThreatHuntingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public String getProfileName(){
        String profileName="th";
        return profileName;
    }

    /** Reconoce targets del perfil; la acción y los argumentos se validan en handle. */
    public boolean supports(JsonNode command) {
        boolean isThreatHunting = false;
        if (command!=null) {
            isThreatHunting = command.path("target").has("features")
                    || command.path("target").path("th").has("huntflows")
                    || command.path("target").path("th").has("datasources")
                    || command.path("target").path("th").has("hunt");
        }
        return isThreatHunting;
    }
    public OpenC2Response solve(OpenC2Message message){
        OpenC2Response response=null;
        return response;
    }

    /** Enruta por target a la consulta o a la validación de investigate. */
    private ObjectNode handle(JsonNode command) {
        if (!supports(command)) {
            throw new IllegalArgumentException("Unsupported Threat Hunting command");
        }
        if (command.path("target").has("features")) {
            return queryFeatures(command);
        }
        if (command.path("target").path("th").has("huntflows")) {
            return queryHuntflows(command);
        }
        if (command.path("target").path("th").has("datasources")) {
            return queryDatasources(command);
        }
        return investigateHunt(command);
    }


    /**
     * Primer paso: consultar las parejas action/target disponibles.
     * Por ahora solo admitimos features: ["pairs"].
     */
    private ObjectNode queryFeatures(JsonNode command) {
        JsonNode target = command.path("target");
        JsonNode features = target.path("features");
        int status = 200;
        var response = objectMapper.createObjectNode();

        // Solo query puede consultar features y el objetivo debe ser único.
        if (!"query".equals(command.path("action").asText()) || target.size() != 1
                || !features.isArray()) {
            status = 400;
            response.put("status_text", "Query features command invalid.");
        } else if (command.has("args") || command.has("actuator")
                || features.size() != 1 || !features.get(0).isTextual()
                || !"pairs".equals(features.get(0).asText())) {
            // Ampliamos las capacidades paso a paso; no ignoramos opciones sin implementar.
            status = 501;
            response.put("status_text", "Only features: [pairs] without args or actuator is supported");
        } else {
            response.putObject("results").putObject("pairs")
                    .putArray("query").add("features").add("/huntflows").add("/datasources");
        }

        response.put("status", status);
        return response;
    }


    /**
     * Consultamos la definición inicial de userlogon, sin ejecutar la investigación.
     * Más adelante añadiremos otras definiciones y filtros de búsqueda.
     */
    private ObjectNode queryHuntflows(JsonNode command) {
        JsonNode target = command.path("target");
        JsonNode th = target.path("th");
        JsonNode huntflows = th.path("huntflows");
        int status = 200;
        var response = objectMapper.createObjectNode();

        if (!"query".equals(command.path("action").asText()) || target.size() != 1
                || !th.isObject() || th.size() != 1 || !huntflows.isObject()) {
            status = 400;
            response.put("status_text", "Invalid query huntflows command");
        } else if (!huntflows.isEmpty() || command.has("args") || command.has("actuator")) {
            status = 501;
            response.put("status_text", "Huntflow filters, args and actuator are not supported yet");
        } else {
            // Devolvemos información del huntflow, sin nombres de artefactos ni consultas VQL.
            var info = response.putObject("results").putObject("th")
                    .putArray("huntflow_info").addObject();
            info.put("path", "userlogon");
            info.put("version", "1.0");
            info.putObject("args_required").put("username", "string");
            // investigate ya valida vigilance_level opcional con STANDARD por defecto;
            // esta respuesta solo publica username como argumento obligatorio.
        }

        response.put("status", status);
        return response;
    }


    /**
     * Devuelve la definición inicial de la fuente de registros de los equipos.
     * Esta lista es estática: no comprueba la disponibilidad real de Velociraptor.
     * Por ahora solo admitimos datasources: "" para consultar todas las fuentes.
     */
    private ObjectNode queryDatasources(JsonNode command) {
        JsonNode target = command.path("target");
        JsonNode th = target.path("th");
        JsonNode datasources = th.path("datasources");
        int status = 200;
        var response = objectMapper.createObjectNode();

        if (!"query".equals(command.path("action").asText()) || target.size() != 1
                || !th.isObject() || th.size() != 1 || !datasources.isTextual()) {
            status = 400;
            response.put("status_text", "Invalid query datasources command");
        } else if (!datasources.asText().isEmpty() || command.has("args") || command.has("actuator")) {
            status = 501;
            response.put("status_text", "Datasource filters, args and actuator are not supported yet");
        } else {
            // Nombre público de la fuente; su correspondencia con VQL quedará en la capa interna.
            var source = response.putObject("results").putObject("th")
                    .putArray("datasources").addObject();
            source.put("ds_name", "endpoint_logs");
            source.putArray("ds_tags").add("authentication").add("windows").add("linux");
        }

        response.put("status", status);
        return response;
    }

    /**
     * Validamos la petición de userlogon sin ejecutarla todavía.
     * string_args utiliza la convención propia nombre=valor.
     */
    private ObjectNode investigateHunt(JsonNode command) {
        JsonNode target = command.path("target");
        JsonNode th = target.path("th");
        if (!"investigate".equals(command.path("action").asText())
                || target.size() != 1 || !th.isObject() || th.size() != 1
                || !th.path("hunt").isTextual() || th.path("hunt").asText().isBlank()) {
            return huntResponse(400, "Invalid investigate hunt command");
        }
        if (!"userlogon".equals(th.path("hunt").asText())) {
            return huntResponse(501, "Huntflow not supported");
        }

        JsonNode args = command.path("args");
        JsonNode thArgs = args.path("th");
        JsonNode huntargs = thArgs.path("huntargs");
        JsonNode strings = huntargs.path("string_args");
        if (!args.isObject() || !thArgs.isObject() || !huntargs.isObject() || !strings.isArray()) {
            return huntResponse(400, "args.th.huntargs.string_args must be an array");
        }
        // No ignoramos filtros ni opciones que aún no sabemos aplicar.
        if (command.has("actuator") || args.size() != 1 || thArgs.size() != 1 || huntargs.size() != 1) {
            return huntResponse(501, "Additional hunt options are not supported yet");
        }

        String username = null;
        String vigilance = null;
        for (JsonNode item : strings) {
            if (!item.isTextual()) {
                return huntResponse(400, "Each string_args entry must be a string");
            }
            String argument = item.asText();
            int separator = argument.indexOf('=');
            if (separator < 1 || argument.substring(separator + 1).isBlank()) {
                return huntResponse(400, "Expected name=value with a non-blank value");
            }
            String name = argument.substring(0, separator);
            String value = argument.substring(separator + 1);
            // Conservamos el valor tal como llega, incluidos otros signos igual.
            if ("username".equals(name) && username == null) {
                username = value;
            } else if ("vigilance_level".equals(name) && vigilance == null) {
                vigilance = value;
            } else {
                return huntResponse(400, "Unknown or duplicate userlogon parameter");
            }
        }

        if (username == null) {
            return huntResponse(400, "username is required");
        }
        if (vigilance == null) vigilance = "STANDARD";
        if (!java.util.Set.of("STANDARD", "SUSPICIOUS", "CRITICAL").contains(vigilance)) {
            return huntResponse(400, "Invalid vigilance_level");
        }

        // Una validación correcta no significa que la investigación se haya ejecutado.
        return huntResponse(501, "Valid userlogon request (vigilance_level=" + vigilance
                + "). Execution is not implemented yet");
    }

    /** Construimos la respuesta OpenC2; el controlador se ocupa del HTTP. */
    private ObjectNode huntResponse(int status, String text) {
        return objectMapper.createObjectNode().put("status", status).put("status_text", text);
    }
}
