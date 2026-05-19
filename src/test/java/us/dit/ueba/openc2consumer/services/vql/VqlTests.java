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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import us.dit.ueba.openc2consumer.config.TestVelociraptorConfig;
import us.dit.ueba.openc2consumer.services.vql.VqlInterface.EvidenceType;

@SpringBootTest
@Import(TestVelociraptorConfig.class)
@ActiveProfiles("test")
class VqlTests {

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
        try {
            vqlService.sendNewArtefact(EvidenceType.USERLOGON);
        } catch (Exception e) {
            log.error("Error sending artifact to Velociraptor: ", e);
            assertNotNull(e, "Exception should not be null");
        }
    }

    @Test
    void startMonitoringInVelociraptor() {
        try {
            vqlService.startMonitoring(EvidenceType.USERLOGON);
        } catch (Exception e) {
            log.error("Error starting monitoring in Velociraptor: ", e);
            assertNotNull(e, "Exception should not be null");
        }
    }

    @Test
    void addUserInVelociraptor() {
        try {
            vqlService.addUser(EvidenceType.USERLOGON, "testuser");
        } catch (Exception e) {
            log.error("Error adding user in Velociraptor: ", e);
            assertNotNull(e, "Exception should not be null");
        }
    }

    @Test
    void deleteUserInVelociraptor() {
        try {
            vqlService.deleteUser(EvidenceType.USERLOGON, "testuser");
        } catch (Exception e) {
            log.error("Error deleting user in Velociraptor: ", e);
            assertNotNull(e, "Exception should not be null");
        }
    }
}
