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

public interface VqlInterface {

    /* 
    public static enum EvidenceType {
        USERLOGON,
        USERSESSION,
        USERLOGOUT,
        USERSESSIONDURATION
    }
     */
    public void sendNewArtefact(String evidenceType);

    public void startMonitoring(String evidenceType);

    public void addUser(String evidenceType, String username, String vigilanceLevel);

    public void addUser(String evidenceType, String username); // Sobrecarga si no se introduce nivel de vigilancia, se considerará STANDARD por defecto

    public void deleteUser(String evidenceType, String username);

}
