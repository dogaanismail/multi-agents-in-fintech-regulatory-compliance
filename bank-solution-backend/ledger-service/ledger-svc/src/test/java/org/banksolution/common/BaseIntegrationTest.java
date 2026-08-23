package org.banksolution.common;

import org.banksolution.common.annotations.IntegrationTest;
import org.banksolution.infrastructure.messaging.kafka.producer.WalletBalanceChangedEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

public abstract class BaseIntegrationTest extends IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // No Kafka broker in the integration context; a real template would block on every send
    @MockitoBean
    protected WalletBalanceChangedEventProducer walletBalanceChangedEventProducer;
}
