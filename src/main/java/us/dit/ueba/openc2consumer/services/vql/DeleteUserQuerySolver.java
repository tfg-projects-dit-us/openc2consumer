package us.dit.ueba.openc2consumer.services.vql;

public class DeleteUserQuerySolver implements QuerySolver {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeleteUserQuerySolver.class);

    /**
     * Esta consulta VQL se utiliza para eliminar un usuario de una tabla
     * específica en Velociraptor. La consulta utiliza la función
     * remove_server_table para eliminar un usuario de una tabla, donde el
     * nombre de la tabla y el usuario a eliminar se pasan como variables de
     * entorno (TableName y TargetUser, respectivamente). La consulta se ejecuta
     * dentro del contexto de scope(), lo que significa que se aplicará a los
     * hosts o entidades que estén dentro del alcance de la consulta.
     */
    @Override
    public String getQuery() {
        log.debug("VQL para eliminar un usuario de una tabla, con el nombre de la tabla como parámetro de la consulta VQL");
        return "SELECT remove_server_table(name=TableName,condition=\"Username = '\" + TargetUser + \"'\") FROM scope()";
    }

}
