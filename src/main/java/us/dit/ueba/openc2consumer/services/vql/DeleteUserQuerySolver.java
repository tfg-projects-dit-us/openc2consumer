package us.dit.ueba.openc2consumer.services.vql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dit.ueba.openc2consumer.services.vql.VqlInterface.EvidenceType;

public class DeleteUserQuerySolver implements QuerySolver {

    EvidenceType evidenceType;
    String artifactsPath;
    private final static Logger log = LoggerFactory.getLogger(DeleteUserQuerySolver.class);

    DeleteUserQuerySolver(EvidenceType evidenceType, String artifactsPath) {
        this.evidenceType = evidenceType;
        this.artifactsPath = artifactsPath;
    }

    @Override
    public String getQuery() {
        log.debug("Obteniendo la consulta VQL");
        String filename = artifactsPath + File.separator + evidenceType.toString().toLowerCase() + ".delete";
        log.debug("Archivo de consulta VQL: " + filename);
        File file = new File(filename);
        String query = null;
        if (!file.exists()) {
            log.error("Archivo de consulta no encontrado: " + filename);
            throw new IllegalArgumentException("Archivo de consulta no encontrado: " + filename);
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
