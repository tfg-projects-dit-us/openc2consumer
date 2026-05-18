package us.dit.ueba.openc2consumer.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dit.ueba.openc2consumer.proto.Api.VQLCollectorArgs;
import us.dit.ueba.openc2consumer.proto.Api.VQLRequest;
import us.dit.ueba.openc2consumer.services.VqlService.EvidenceType;

/**
 * El objetivo de esta clase es construir las consultas VQL que se enviarán a
 * Velociraptor.
 */
public class ArgsBuilder {

    private String name;
    private EvidenceType evidenceType;

    private String artifactsPath;
    private final static Logger log = LoggerFactory.getLogger(ArgsBuilder.class);

    public ArgsBuilder setName(String name) {
        this.name = name;
        return this;
    }

    ArgsBuilder(EvidenceType evidenceType, String path) {
        this.evidenceType = evidenceType;
        this.artifactsPath = path;
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
        String vqlQuery = getQuery();
        if (vqlQuery != null && name != null) {
            request = VQLRequest.newBuilder()
                    .setVQL(vqlQuery)
                    .setName(name)
                    .build();
            args = VQLCollectorArgs.newBuilder()
                    .addQuery(request)
                    .build();
        } else {
            log.error("VQL query or name is null. Cannot build arguments for Velociraptor.");
            throw new IllegalArgumentException("VQL query and name must be provided to build arguments for Velociraptor.");
        }
        return args;
    }

    private String getQuery() {
        log.debug("Obteniendo la consulta VQL");
        String filename = artifactsPath + File.separator + evidenceType.toString().toLowerCase() + ".artifact";
        log.debug("Archivo de consulta VQL: " + filename);
        File file = new File(filename);
        String query = null;
        if (!file.exists()) {
            log.error("Query file not found: " + filename);
            throw new IllegalArgumentException("Query file not found: " + filename);
        }
        try {
            query = Files.readString(file.toPath());
        } catch (IOException e) {
            log.error("Error reading query file: " + filename, e);
            throw new IllegalArgumentException("Error reading query file: " + filename, e);
        }
        return query;
    }

}
