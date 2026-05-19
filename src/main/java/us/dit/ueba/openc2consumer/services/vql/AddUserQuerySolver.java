package us.dit.ueba.openc2consumer.services.vql;

public class AddUserQuerySolver implements QuerySolver {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddUserQuerySolver.class);

    /**
     * Esta consulta VQL se utiliza para agregar un usuario a una tabla en
     * Velociraptor. La consulta utiliza la función add_server_table para
     * agregar una fila a una tabla específica, donde el nombre de la tabla y el
     * usuario a agregar se pasan como variables de entorno (TableName y
     * TargetUser, respectivamente). La consulta se ejecuta dentro del contexto
     * de scope(), lo que significa que se aplicará a los hosts o entidades que
     * estén dentro del alcance de la consulta.
     */
    @Override
    public String getQuery() {
        log.debug("VQL para incluir un usuario en una tabla, ambos como parámetros de la consulta VQL");
        return "SELECT add_server_table(name=TableName,row=dict(Username=TargetUser)) FROM scope()";
    }

}
