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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import us.dit.ueba.openc2consumer.integrations.velociraptor.TestVelociraptorConfig;

/** Pruebas con gRPC simulado: los errores inesperados fallan la prueba. No ejecutan VQL real. */
@SpringBootTest
@Import(TestVelociraptorConfig.class)
@ActiveProfiles("test")
class VqlTests {
    private static final java.nio.file.Path ARTIFACTS = createArtifacts();

    private static java.nio.file.Path createArtifacts() {
        try {
            var directory = java.nio.file.Files.createTempDirectory("vql-grpc-tests");
            var artifact = directory.resolve("userlogon.artifact");
            java.nio.file.Files.writeString(artifact, "SELECT 1 FROM scope()");
            directory.toFile().deleteOnExit();
            artifact.toFile().deleteOnExit();
            return directory;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @org.springframework.test.context.DynamicPropertySource
    static void artifactProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("artifacts.path", () -> ARTIFACTS.toString());
    }

    @Autowired
    private VqlService vqlService;
    private static Logger log = LoggerFactory.getLogger(VqlTests.class);

    @Test
    void velociraptorServiceDescription() {
        String serviceDescriptor = vqlService.getServiceDescriptor();
        assertNotNull(serviceDescriptor, "Service descriptor should not be null");

        log.info("ServiceDescriptor: {}", vqlService.getServiceDescriptor());

    }

    @Test
    void sendArtifactToVelociraptor() {
        assertDoesNotThrow(() -> vqlService.sendNewArtefact("userlogon"));
    }

    @Test
    void startMonitoringInVelociraptor() {
        assertDoesNotThrow(() -> vqlService.startMonitoring("userlogon"));
    }

    @Test
    void addUserInVelociraptor() {
        assertDoesNotThrow(() -> vqlService.addUser("userlogon", "testuser"));
    }

    @Test
    void addUserWithLevel() {
        assertDoesNotThrow(() -> vqlService.addUser("userlogon", "testuser", "HIGH"));
    }

    @Test
    void deleteUserInVelociraptor() {
        assertDoesNotThrow(() -> vqlService.deleteUser("userlogon", "testuser"));
    }

    @Test
    void deleteUserWithInvalidEvidenceTypeThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> vqlService.deleteUser("invalidtype", "testuser"),
                "Se debe lanzar IllegalArgumentException cuando el tipo de evidencia no es válido");
    }

    @Test
    void sendNewArtefactWithMissingFileThrowsException() {

        assertThrows(RuntimeException.class,
                () -> vqlService.sendNewArtefact("usersession"),
                "Se debe lanzar IllegalArgumentException cuando el fichero de artefacto no existe");

    }
}
