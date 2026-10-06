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
package us.dit.ueba.openc2consumer.integrations.velociraptor.vql;

/**
 * Define las operaciones para registrar artefactos y gestionar usuarios en su monitorización.
 * VqlService implementa estos métodos preparando y enviando consultas VQL a Velociraptor.
 * @author Isabel Román
 */
public interface VqlInterface {


    /** Solicita registrar un artefacto mediante la consulta leída del archivo .artifact correspondiente. */
    void sendNewArtefact(String evidenceType);

    /** Solicita activar la monitorización del artefacto asociado al tipo de evidencia. */
    void startMonitoring(String evidenceType);

    /** Añade un usuario a la monitorización del tipo de evidencia con el nivel indicado. */
    void addUser(String evidenceType, String username, String vigilanceLevel);

    /** Añade un usuario a la monitorización con el nivel STANDARD. */
    void addUser(String evidenceType, String username);

    /** Retira al usuario de la tabla de monitorización del tipo de evidencia. */
    void deleteUser(String evidenceType, String username);

}
