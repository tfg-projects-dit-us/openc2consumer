package us.dit.ueba.openc2consumer.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dit.ueba.openc2consumer.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.proto.Api.VQLEnv;
import us.dit.ueba.openc2consumer.proto.Api.VQLRequest;

/**
 * El objetivo de esta clase es construir las consultas VQL que se enviarán a
 * Velociraptor.
 */
public class ArgsBuilder {

    private String name;
    private QuerySolver solver;

    Map<String, VQLEnv> variables = null;

    private final static Logger log = LoggerFactory.getLogger(ArgsBuilder.class);

    ArgsBuilder(QuerySolver solver) {
        this.solver = solver;
    }

    ArgsBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Establece una variable de entorno para la consulta VQL. Estas variables
     * pueden ser utilizadas dentro de la consulta VQL para personalizar su
     * comportamiento. Son los prámetros de la consulta VQL p.e.
     * TargetUser={{username}}
     *
     * @param key
     * @param value
     * @return
     */
    public ArgsBuilder setVariable(String key, String value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, VQLEnv.newBuilder().setKey(key).setValue(value).build());
        return this;
    }

    /**
     * Construye los argumentos para enviar una consulta a Velociraptor
     *
     * @param vqlQuery
     * @param name
     * @return Los argumentos formateados para la consulta gRPC
     */
    public VQLCollectorArgs buildArgs() {
        VQLRequest request = null;
        VQLCollectorArgs args = null;

        String vqlQuery = solver.getQuery();
        //Primero se intenta construir la consulta con el nombre y la query, si no se proporciona el nombre, se intenta construir solo con la query, si no se proporciona la query, se lanza una excepción
        if (vqlQuery != null && name != null) {
            request = VQLRequest.newBuilder()
                    .setVQL(vqlQuery)
                    .setName(name)
                    .build();

        } else {
            log.error("La consulta VQL o el nombre del artefacto no se localizan");
            throw new IllegalArgumentException("VQL query and name must be provided to build arguments for Velociraptor.");
        }
        if (variables != null) {
            log.debug("Construyendo argumentos con variables: {}", variables.keySet());
            args = VQLCollectorArgs.newBuilder(args)
                    .addQuery(request)
                    .addAllEnv(variables.values())
                    .build();

        } else {
            log.debug("Construyendo argumentos sin variables");
            args = VQLCollectorArgs.newBuilder(args)
                    .addQuery(request)
                    .build();
        }

        return args;
    }

}
