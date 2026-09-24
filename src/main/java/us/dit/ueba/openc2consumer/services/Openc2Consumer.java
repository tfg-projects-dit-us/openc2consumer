package us.dit.ueba.openc2consumer.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;
import org.oasis.openc2.lycan.args.Args;
import org.oasis.openc2.lycan.targets.Features;
import org.oasis.openc2.lycan.types.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import us.dit.ueba.openc2consumer.actuators.Actuator;
/**
 * Servicio con las capacidades ofrecidas por un consumidro openc2 a cualquier controlador que permita
 * la recepción de comandos y el envío de respuestas.
 * Debe abstraerse de los detalles de red y centrarse en las funciones de consumidor
 * El consumidor incluye una lista de actuadores en los que delega la ejecución de comandos de perfiles concretos
 * El consumidor debe ejecutar los comandos obligatorios: query/features (por ejemplo), aunque para ello puede que tenga que interaccionar con los actuadores
 */
@Service
public class Openc2Consumer {
    private static final Logger log = LoggerFactory.getLogger(Openc2Consumer.class);
    private final String openC2Version = "1.0"; // La versión de openC2 que soporta este consumidor

    private final List<Actuator> registeredActuators;

 public Openc2Consumer(List<Actuator> registeredActuators) {
        this.registeredActuators = registeredActuators;
    }
    //la respuesta al par query:features es obligatorio en todos los perfiles
    // Todos los consumidores openC2 tienen que implemenar este par action:target

    //Tengo que preguntar a cada actuador y componer la respuesta de forma covneniente, según sea el tipo de objetivo
    //    
    public OpenC2Response solve(OpenC2Message command) {
         OpenC2Response openC2Response = new OpenC2Response();
         if (command.getAction().equals("query") && command.getTarget().getFeatures() != null) {
            openC2Response = queryFeatures(command);
        } else {

            // 1. Filtrar los actuadores que deben responder
            List<Actuator> matchingActuators = registeredActuators.stream()
                    .filter(actuator -> actuator.supports(command))
                    .collect(Collectors.toList());
            // Si ningún actuador puede procesarlo
            if (matchingActuators.isEmpty()) {
                openC2Response.setStatus(501);
                openC2Response.setStatusText("Not Implemented: No actuator registered for this target/action");
            } else {
                openC2Response = aggregateResponses(matchingActuators, command);
            }
        }
            return openC2Response;
    }
    /**
     * Este método está desarrollado conforme al apartado 4.1 Query Command, del estándar openC2 v1.0
     * The 'query features' Command is REQUIRED for all Producers and Consumers implementing OpenC2.
     */
    public OpenC2Response queryFeatures(OpenC2Message command) {
        OpenC2Response response = new OpenC2Response();
        Features features=command.getTarget().getFeatures();
        Args args=command.getArgs();
        boolean responseRequested=args.getResponseRequested().toLowerCase().equals("complete");
        //El estándar dice que el comando query con el target Features sólo acepta como argumento "response_requested" con valor "complete"
        if (args!=null && !responseRequested) {
            //If the command has a response_requested argument and it is not "complete", we return 400          
            response.setStatus(400);
            response.setStatusText("Bad Request: with the pair query:features argument must be omitted or only 'response_requested' with value 'complete' is allowed");           
        }
        // Implement the logic to handle the query features command
        //Depende de los specificadores que se hayan puesto en features... versions, profiles, pairs o rate_limits
        List<String> requiredFeatures = features.getFeatures();
        List<FeatureType> requiredFeatureTypes = new java.util.ArrayList<>();      
       
        for (String feature : requiredFeatures) {
            requiredFeatureTypes.add(FeatureType.valueOf(feature));
        }
         for (FeatureType featureType : requiredFeatureTypes) {
            switch(featureType) {
                case VERSIONS:
                    // Add supported versions to the response
                    //Es necesario averiguar si cada actuador podría tener una versión y esto debería ser una lista de versiones
                    //Entonces el actuador tiene que tener un cmapo versión
                    response.addResults(featureType.toString(), "1.0");
                    break;
                case PROFILES:
                    // Add supported profiles to the response
                    List<String> profileNames = registeredActuators.stream()
                            .map(Actuator::getProfileName)
                            .filter(name -> name != null && !name.isEmpty())
                            .collect(Collectors.toList());
                    response.addResults(featureType.toString(), profileNames);
                    break;
                case PAIRS:
                    // Add supported action/target pairs to the response
                  
                    break;
                case RATE_LIMIT:
                    //Esto no se lo que debe devolver
                    response.addResults(featureType.toString(), Map.of("default", 100));
                    break;
                default:
                    // Handle unknown feature types if necessary
                    log.warn("Unknown feature type requested: {}", featureType);
            }
            switch (featureType) {
                case VERSIONS:
                    // Add supported versions to the response
                    //Es necesario averiguar si cada actuador podría tener una versión y esto debería ser una lista de versiones
                    //Entonces el actuador tiene que tener un cmapo versión
                    response.addResults(featureType, "1.0");
                    break;
                case PROFILES:
                    // Add supported profiles to the response
                    List<String> profileNames = registeredActuators.stream()
                            .map(Actuator::getProfileName)
                            .filter(name -> name != null && !name.isEmpty())
                            .collect(Collectors.toList());
                    response.addResults(featureType, args);
                    break;
                case "pairs":
                    // Add supported action/target pairs to the response
                    Map<String, List<String>> pairs = registeredActuators.stream()
                            .flatMap(actuator -> actuator.getSupportedPairs().stream())
                            .collect(Collectors.groupingBy(Map.Entry::getKey,
                                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
                    response.addFeature("pairs", pairs);
                    break;
                case "rate-limits":
                    // Add rate limits to the response if applicable
                    // This is a placeholder; implement rate limit logic as needed
                    response.addFeature("rate-limits", Map.of("default", 100));
                    break;
                default:
                    // Handle unknown feature types if necessary
                    log.warn("Unknown feature type requested: {}", featureType);
            }
        }
        //Tengo una lista con los tipos de feature solicitados, podrían ser
        // versions, profiles, pairs y rate-limits
        
        response.setStatus(200);
        response.setStatusText("Query features executed successfully");
        return response;
    }
      /**
     * Construcción de la respuesta OpenC2 a partir de la respuesta de cada uno de
     * los actuadores.
     * Esto está sin revisar, es sólo un esqueleto para que sirva de base
     * 
     * @param actuators
     * @param command
     * @return
     */
    private OpenC2Response aggregateResponses(List<Actuator> actuators, OpenC2Message command) {
        // Estado si todo va bien
        int finalStatusCode = 200;
        OpenC2Response aggregatedResponse = new OpenC2Response();
        aggregatedResponse.setStatus(200);

        Map<String, Object> aggregatedResults = new HashMap<>();
        // La combinación de respuestas de los actuadores no está bien trabajada, hay
        // que pensarla bien
        for (Actuator actuator : actuators) {
            try {
                OpenC2Response response = actuator.solve(command);

                // Si alguno falla, el estado general deja de ser 200
                if (response.getStatus() >= 400) {
                    aggregatedResponse.setStatus(207); // Exito parcial alguno tiene problemas
                    aggregatedResponse.setStatusText(actuator.getProfileName() + ":" + response.getStatusText() + ";"
                            + aggregatedResponse.getStatusText());
                }

                // Agrupamos el resultado bajo la clave del actuador
                if (response.getResults() != null) {
                    aggregatedResults.put(actuator.getProfileName(), response.getResults());
                }

            } catch (Exception e) {
                aggregatedResponse.setStatus(500);
                aggregatedResponse.setStatusText("General error: " + e.getMessage());
                aggregatedResults.put(actuator.getProfileName(), Map.of("error", e.getMessage()));
            }
        }
        aggregatedResponse.setResults(aggregatedResults);
        return aggregatedResponse;
    }

}
