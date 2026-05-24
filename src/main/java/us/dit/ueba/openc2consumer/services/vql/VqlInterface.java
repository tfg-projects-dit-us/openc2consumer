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
 * @author Isabel Román Esta interfaz define los métodos que deben implementar
 * las clases que se encargan de construir y enviar consultas VQL a
 * Velociraptor. Cada método corresponde a una acción específica que se puede
 * realizar
 */
public interface VqlInterface {

    /* 
    public static enum EvidenceType {
        USERLOGON,
        USERSESSION,
        USERLOGOUT,
        USERSESSIONDURATION
    }
     */
    /**
     * Este método se encarga de construir y enviar una consulta VQL para
     * agregar una nueva evidencia en Velociraptor, utilizando un archivo
     * .artifact específico para cada tipo de evidencia. La ruta de los archivos
     * .artifact se configura en application.properties, y el nombre del archivo
     * se corresponde con el tipo de evidencia (e.g., userlogon.artifact para
     * evidencias de tipo USERLOGON).
     *
     * @param evidenceType
     */
    public void sendNewArtefact(String evidenceType);

    /**
     * Este método se encarga de construir y enviar una consulta VQL para
     * iniciar la monitorización de una evidencia determinada
     *
     * @param evidenceType
     */
    public void startMonitoring(String evidenceType);

    /**
     * Este método se encarga de construir y enviar una consulta VQL para
     * agregar un usuario a una evidencia, utilizando el nombre de la evidencia,
     * el usuario a agregar y su nivel de rastreo/vigilancia como variables de
     * entorno envql (Tipo de evidencia, nombre de usuario y nivel de vigilancia
     * deseado).
     *
     * @param evidenceType
     * @param username
     * @param vigilanceLevel
     */
    public void addUser(String evidenceType, String username, String vigilanceLevel);

    /**
     * Añade un usuario a una evidencia, sin nivel de vigilancia, se pone el de
     * por defecto
     *
     * @param evidenceType
     * @param username
     */
    public void addUser(String evidenceType, String username); // Sobrecarga si no se introduce nivel de vigilancia, se considerará STANDARD por defecto

    /**
     * Este método se encarga de construir y enviar una consulta VQL para
     * eliminar un usuario de una tabla en Velociraptor, utilizando el nombre de
     * la tabla y el usuario a eliminar como variables de entorno envql
     * (TableName, TargetUser).
     *
     * @param evidenceType
     * @param username
     */
    public void deleteUser(String evidenceType, String username);

}
