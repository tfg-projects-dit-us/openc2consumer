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
package us.dit.ueba.openc2consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del Consumer: arranca la aplicación con Spring Boot.
 * Spring detecta los componentes de este paquete y sus subpaquetes
 * y aplica la configuración necesaria para iniciar el servidor HTTP.
 */
@SpringBootApplication
public class Openc2consumerApplication {

    /** Inicia Spring Boot con los argumentos recibidos al ejecutar la aplicación. */
    public static void main(String[] args) {
        SpringApplication.run(Openc2consumerApplication.class, args);
    }

}
