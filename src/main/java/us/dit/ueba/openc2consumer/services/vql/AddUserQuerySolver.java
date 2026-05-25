/*
 * Copyright 2026 Universidad de Sevilla/Departamento de Ingeniería Telemática
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is part of OpenC2Consumer, a SOAR4FUEBA (SOAR solution based on BPM paradigm) component
 */
package us.dit.ueba.openc2consumer.services.vql;

/**
 * @author Isabel Román Implementación de QuerySolver que genera una consulta
 * VQL para agregar un usuario a una tabla en Velociraptor. Esta clase se
 * encarga de construir una consulta VQL que utiliza la función add_server_table
 * para agregar una fila a una tabla específica en Velociraptor, donde el nombre
 * de la tabla, el usuario a agregar y su nivel de rastreo/vigilancia se pasan
 * como variables de entorno envql (TableName, TargetUser, TargetLevel). La
 * consulta se ejecuta dentro del contexto de scope(), lo que significa que se
 * aplicará a los hosts o entidades que estén dentro del alcance de la consulta.
 */
public class AddUserQuerySolver implements QuerySolver {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddUserQuerySolver.class);

    /**
     * Esta consulta VQL se utiliza para agregar un usuario a una tabla en
     * Velociraptor. La consulta utiliza la función add_server_table para
     * agregar una fila a una tabla específica, donde el nombre de la tabla, el
     * usuario a agregar y su nivel de rastreo/vigilancia se pasan como
     * variables de entorno envql (TableName, TargetUser, TargetLevel). La
     * consulta se ejecuta dentro del contexto de scope(), lo que significa que
     * se aplicará a los hosts o entidades que estén dentro del alcance de la
     * consulta.
     */
    @Override
    public String getQuery() {
        log.debug("VQL para incluir un usuario en una tabla, ambos como parámetros de la consulta VQL");
        String query = """     
    SELECT add_server_table(
        name = TableName,
        row = dict(
            Username = TargetUser,
            VigilanceLevel = get(item = scope(), field = 'TargetLevel', default = 'STANDARD')
        )
    ) FROM scope()
    """;

        return query;
    }

}
