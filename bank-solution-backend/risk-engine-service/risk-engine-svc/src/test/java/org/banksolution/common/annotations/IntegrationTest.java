package org.banksolution.common.annotations;

import org.banksolution.RiskEngineServiceApplication;
import org.banksolution.common.initializers.KafkaInitializer;
import org.banksolution.common.initializers.PostgreSQLInitializer;
import org.banksolution.common.initializers.WireMockInitializer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {RiskEngineServiceApplication.class}
)
@ContextConfiguration(initializers = {
        PostgreSQLInitializer.class,
        KafkaInitializer.class,
        WireMockInitializer.class
})
@ActiveProfiles({"test"})
@AutoConfigureMockMvc
public abstract class IntegrationTest {
}
