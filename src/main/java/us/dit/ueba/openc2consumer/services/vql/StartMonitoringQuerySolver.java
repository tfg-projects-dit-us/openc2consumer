package us.dit.ueba.openc2consumer.services.vql;

public class StartMonitoringQuerySolver implements QuerySolver {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StartMonitoringQuerySolver.class);

    /**
     * Esta consulta VQL se utiliza para iniciar la monitorización de un
     * artefacto específico en Velociraptor. La consulta utiliza la función
     * start_monitoring para comenzar a monitorear un artefacto, donde el nombre
     * del artefacto se pasa como una variable de entorno (ArtifactName). La
     * consulta se ejecuta dentro del contexto de scope(), lo que significa que
     * se aplicará a los hosts o entidades que estén dentro del alcance de la
     * consulta.
     */
    @Override
    public String getQuery() {
        log.debug("VQL para iniciar la monitorización de un artefacto específico, con el nombre del artefacto como parámetro de la consulta VQL");
        return "SELECT upsert_client_monitoring(artifact_name=ArtifactName) FROM scope()";

    }
}
