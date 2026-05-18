package us.dit.ueba.openc2consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import us.dit.ueba.openc2consumer.config.TestVelociraptorConfig;
import us.dit.ueba.openc2consumer.services.VqlService;

@SpringBootTest
@Import(TestVelociraptorConfig.class)
@ActiveProfiles("test")
class Openc2consumerApplicationTests {

    @Autowired
    private VqlService vqlService;
    private static Logger log = LoggerFactory.getLogger(Openc2consumerApplicationTests.class);

    @Test
    void velociraptorServiceDescription() {
        String serviceDescriptor = vqlService.getServiceDescriptor();
        assertNotNull(serviceDescriptor, "Service descriptor should not be null");

        log.info("ServiceDescriptor: {}", vqlService.getServiceDescriptor());

    }

    @Test
    void sendQueryToVelociraptor() {
        try {
            vqlService.sendQuery(VqlService.EvidenceType.USERLOGON, "test_userlogon");
        } catch (Exception e) {
            log.error("Error sending query to Velociraptor: ", e);
            assertNotNull(e, "Exception should not be null");
        }
    }

}
