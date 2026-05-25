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
package us.dit.ueba.openc2consumer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Isabel Román Modelo que representa una evidencia de comportamiento de
 * usuario.
 */
public class Evidence {

    private Long id;
    private String type;
    private List<String> usernames = new ArrayList<>();
    private String artefact;
    private String description;

    public Evidence() {
    }

    public Evidence(Long id, String type, List<String> usernames, String artefact, String description) {
        this.id = id;
        this.type = type;
        this.usernames = usernames;
        this.artefact = artefact;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }

    public String getDescription() {
        return description;
    }

    public String getArtefact() {
        return artefact;
    }

    public void setArtefact(String artefact) {
        this.artefact = artefact;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
