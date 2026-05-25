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

public class DeleteUserQuerySolver implements QuerySolver {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeleteUserQuerySolver.class);

    /**
     * @author Isabel Román Esta consulta VQL se utiliza para eliminar un
     * usuario de una tabla específica en Velociraptor. La consulta utiliza la
     * función remove_server_table para eliminar un usuario de una tabla, donde
     * el nombre de la tabla y el usuario a eliminar se pasan como variables de
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
