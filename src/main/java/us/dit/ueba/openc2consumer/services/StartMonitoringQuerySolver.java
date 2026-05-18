package us.dit.ueba.openc2consumer.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;

import us.dit.ueba.openc2consumer.services.VqlInterface.EvidenceType;

public class StartMonitoringQuerySolver implements QuerySolver {

    EvidenceType evidenceType;
    @Value("${artifacts.path}")
    String artifactsPath;
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StartMonitoringQuerySolver.class);

    StartMonitoringQuerySolver(VqlService.EvidenceType evidenceType) {
        this.evidenceType = evidenceType;
    }

    @Override
    public String getQuery() {
        log.debug("Obteniendo la consulta VQL");
        String filename = artifactsPath + File.separator + evidenceType.toString().toLowerCase() + ".monitoring";
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
