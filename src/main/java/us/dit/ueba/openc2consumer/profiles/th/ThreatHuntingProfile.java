package us.dit.ueba.openc2consumer.profiles.th;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;
import org.oasis.openc2.lycan.args.Args;
import org.oasis.openc2.lycan.profiles.th.args.HuntArgs;
import org.oasis.openc2.lycan.types.ActionType;
import org.oasis.openc2.lycan.types.ArgsResponseType;
import org.oasis.openc2.lycan.types.StatusCodeType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dit.ueba.openc2consumer.core.actuators.ActuatorCommons;
import us.dit.ueba.openc2consumer.core.capabilities.PairsMap;
import us.dit.ueba.openc2consumer.profiles.th.arguments.InvestigationArguments;
import us.dit.ueba.openc2consumer.profiles.th.catalog.HuntflowFilters;
import us.dit.ueba.openc2consumer.profiles.th.catalog.HuntflowPresentation;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationRequest;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationResult;

/**
 * Atiende los comandos OpenC2 del perfil TH: consultar huntflows, consultar fuentes de datos
 * y solicitar investigaciones. Comprueba los argumentos, llama a ThreatHuntingService
 * y prepara la respuesta OpenC2 con los datos obtenidos.
 * Anuncia la investigación solo si el servicio permite ejecutarla.
 */
@Service
public class ThreatHuntingProfile extends ActuatorCommons {
    private final ThreatHuntingService threatHuntingService;

    /** Recibe el servicio y registra los pares acción-target que se anunciarán. */
    public ThreatHuntingProfile(ThreatHuntingService threatHuntingService) {
        super("th");
        this.threatHuntingService = Objects.requireNonNull(threatHuntingService);
        var pairs = PairsMap.builder()
                .addPairAction(ActionType.QUERY, "th.huntflows")
                .addPairAction(ActionType.QUERY, "th.datasources");
        if (threatHuntingService.supportsInvestigations()) {
            pairs.addPairAction(ActionType.INVESTIGATE, "th.hunt");
        }
        this.supportedPairs = pairs.build().asMap();
    }

    /**
     * Reconoce las combinaciones de acción y target del perfil TH.
     * La comprobación de argumentos y de disponibilidad de la investigación se hace en solve().
     */
    @Override
    public boolean supports(OpenC2Message command) {
        if (command == null) {
            return false;
        }
        if (command.getTarget() == null || command.getTarget().getTh() == null) {
            return false;
        }

        var target = command.getTarget().getTh();
        if (ActionType.INVESTIGATE.toString().equals(command.getAction())) {
            return target.getHunt() != null;
        }
        return ActionType.QUERY.toString().equals(command.getAction())
                && (target.getHuntflows() != null || target.getDatasources() != null);
    }

    /** Elige el método que atiende el comando; si no reconoce el comando, devuelve 501. */
    @Override
    public OpenC2Response solve(OpenC2Message command) {
        if (!supports(command)) {
            return errorResponse(StatusCodeType.NOT_IMPLEMENTED, "Command not supported");
        }
        if (ActionType.INVESTIGATE.toString().equals(command.getAction())) {
            return investigate(command);
        }
        if (command.getTarget().getTh().getDatasources() != null) {
            return queryDatasources(command);
        }
        return queryHuntflows(command);
    }

    /**
     * Obtiene el catálogo, aplica los filtros y selecciona los campos pedidos en return_format.
     * Devuelve los datos en results.th.huntflow_info, o 400 si los filtros o campos son inválidos.
     */
    private OpenC2Response queryHuntflows(OpenC2Message command) {
        var filters = command.getTarget().getTh().getHuntflows();
        if (hasUnsupportedArguments(command)) {
            return new OpenC2Response().setStatusCode(StatusCodeType.NOT_IMPLEMENTED)
                    .setStatusText("Requested huntflow query arguments are not implemented yet");
        }
        try {
            var selected = HuntflowFilters.select(threatHuntingService.listHuntflows(), filters);
            var fields = HuntflowPresentation.selectFields(selected, filters.getReturnFormat());
            return new OpenC2Response().setStatusCode(StatusCodeType.OK).setStatusText("Huntflow catalog retrieved")
                    .addResults("th", Map.of("huntflow_info", fields));
        } catch (IllegalArgumentException e) {
            return new OpenC2Response().setStatusCode(StatusCodeType.BAD_REQUEST).setStatusText(e.getMessage());
        }
    }

    /** Consulta las fuentes por nombre; un nombre vacío pide todas. Las incluye en results.th.datasources. */
    private OpenC2Response queryDatasources(OpenC2Message command) {
        if (hasUnsupportedArguments(command)) {
            return new OpenC2Response().setStatusCode(StatusCodeType.NOT_IMPLEMENTED)
                    .setStatusText("Requested datasource query arguments are not implemented yet");
        }
        String name = command.getTarget().getTh().getDatasources();
        var selected = threatHuntingService.listDatasources(name);
        // Una lista vacía también se devuelve como una consulta correcta, con estado 200.
        return new OpenC2Response().setStatusCode(StatusCodeType.OK).setStatusText("Datasource catalog retrieved")
                .addResults("th", Map.of("datasources", selected));
    }

    /** Para consultas de catálogo, rechaza tiempos, argumentos TH y respuestas distintas de complete. */
    private boolean hasUnsupportedArguments(OpenC2Message command) {
        var args = command.getArgs();
        if (args == null) {
            return false;
        }
        if (args.getStartTime() != null || args.getStopTime() != null || args.getDuration() != null) {
            return true;
        }
        if (args.getTh() != null) {
            return true;
        }
        return !ArgsResponseType.COMPLETE.toString().equals(args.getResponseRequested());
    }

    /**
     * Comprueba que el servicio permita investigar y busca el huntflow solicitado.
     * Valida sus argumentos, entrega la petición al servicio y convierte los hallazgos a texto JSON.
     * Incluye los hallazgos en results.th.inv_returns y convierte los errores a estados OpenC2.
     */
    private OpenC2Response investigate(OpenC2Message command) {
        if (!threatHuntingService.supportsInvestigations()) {
            return errorResponse(StatusCodeType.NOT_IMPLEMENTED, "Investigation is not supported by this service");
        }

        String huntflow = command.getTarget().getTh().getHunt();
        if (huntflow.isBlank()) {
            return errorResponse(StatusCodeType.BAD_REQUEST, "Huntflow must not be blank");
        }

        var args = command.getArgs();
        if (hasUnsupportedExecutionOptions(args)) {
            return errorResponse(StatusCodeType.NOT_IMPLEMENTED, "Requested investigation options are not implemented");
        }

        try {
            // 1. Buscar el huntflow y comprobar sus argumentos.
            HuntflowInfo selected = findHuntflow(huntflow);
            if (selected == null) {
                return errorResponse(StatusCodeType.NOT_FOUND, "Unknown huntflow");
            }
            HuntArgs huntArguments = null;
            if (args != null && args.getTh() != null) {
                huntArguments = args.getTh().getHuntargs();
            }
            Map<String, Object> arguments = InvestigationArguments.validate(selected, huntArguments);

            // 2. Pedir al servicio que ejecute la investigación.
            InvestigationRequest request = new InvestigationRequest(huntflow, arguments);
            InvestigationResult result = threatHuntingService.investigate(request);

            // 3. Preparar la respuesta OpenC2 con los hallazgos obtenidos.
            return investigationResponse(result);
        } catch (InvestigationException e) {
            return errorResponse(statusFor(e.reason()), e.reason().name());
        } catch (JsonProcessingException | RuntimeException e) {
            return errorResponse(StatusCodeType.INTERNAL_ERROR, "Investigation failed");
        }
    }

    /** Busca una ruta exacta en el catálogo; devuelve null si no existe. */
    private HuntflowInfo findHuntflow(String path) {
        for (HuntflowInfo huntflow : threatHuntingService.listHuntflows()) {
            if (huntflow.path().equals(path)) {
                return huntflow;
            }
        }
        return null;
    }

    /** Comprueba los tiempos y el tipo de respuesta; no comprueba los argumentos propios de TH. */
    private boolean hasUnsupportedExecutionOptions(Args args) {
        if (args == null) {
            return false;
        }
        if (args.getStartTime() != null || args.getStopTime() != null || args.getDuration() != null) {
            return true;
        }
        return !ArgsResponseType.COMPLETE.toString().equals(args.getResponseRequested());
    }

    /** Convierte los hallazgos a texto JSON y conserva una entrada aunque no haya hallazgos. */
    private OpenC2Response investigationResponse(InvestigationResult result) throws JsonProcessingException {
        List<String> findings = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Map<String, String> finding : result.findings()) {
            findings.add(mapper.writeValueAsString(finding));
        }

        Map<String, Object> entry = Map.of("string_returns", findings);
        Map<String, Object> results = Map.of("inv_returns", List.of(entry));
        return new OpenC2Response()
                .setStatusCode(StatusCodeType.OK)
                .setStatusText("Investigation completed")
                .addResults("th", results);
    }

    /** Elige el estado OpenC2 correspondiente al motivo del error. */
    private StatusCodeType statusFor(InvestigationException.Reason reason) {
        return switch (reason) {
            case UNSUPPORTED -> StatusCodeType.NOT_IMPLEMENTED;
            case UNKNOWN_HUNTFLOW -> StatusCodeType.NOT_FOUND;
            case INVALID_ARGUMENTS -> StatusCodeType.BAD_REQUEST;
            case UNAVAILABLE -> StatusCodeType.SERVICE_UNAVAILABLE;
            case EXECUTION_FAILED -> StatusCodeType.INTERNAL_ERROR;
        };
    }

    /** Crea una respuesta de error con el estado y el mensaje recibidos. */
    private OpenC2Response errorResponse(StatusCodeType status, String message) {
        return new OpenC2Response().setStatusCode(status).setStatusText(message);
    }
}