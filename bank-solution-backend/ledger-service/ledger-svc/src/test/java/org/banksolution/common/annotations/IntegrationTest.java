package org.banksolution.common.annotations;

import org.banksolution.LedgerServiceApplication;
import org.banksolution.common.initializers.TigerBeetleInitializer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {LedgerServiceApplication.class}
)
@ContextConfiguration(initializers = {
        TigerBeetleInitializer.class
})
@ActiveProfiles({"test"})
@AutoConfigureMockMvc
public abstract class IntegrationTest {
}
