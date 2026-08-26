package org.banksolution.common;

import org.banksolution.common.annotations.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

public abstract class BaseIntegrationTest extends IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
