package org.banksolution.common.annotations;

import org.springframework.context.annotation.Import;
import org.banksolution.servicesecurity.testing.EveryPermissionMockMvcConfiguration;
import org.banksolution.CustomerServiceApplication;
import org.banksolution.common.initializers.PostgreSQLInitializer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {CustomerServiceApplication.class}
)
@ContextConfiguration(initializers = {
        PostgreSQLInitializer.class
})
@ActiveProfiles({"test"})
@AutoConfigureMockMvc
@Import(EveryPermissionMockMvcConfiguration.class)
public abstract class IntegrationTest {
}
