package org.banksolution.common;

import org.banksolution.common.annotations.IntegrationTest;
import org.banksolution.common.initializers.WireMockInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

public abstract class BaseIntegrationTest extends IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void resetWireMock() {
        WireMockInitializer.WIRE_MOCK_SERVER.resetAll();
    }
}
