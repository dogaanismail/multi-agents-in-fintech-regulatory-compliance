package org.banksolution.common.initializers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * One WireMock server stands in for every Feign target; the clients are told apart by
 * base path, exactly as the real services are behind their own ports.
 */
public class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String CUSTOMER_SERVICE_BASE_PATH = "/api/v1/customers";
    public static final String LEDGER_SERVICE_BASE_PATH = "/api/v1/ledger/accounts";

    /**
     * Started once and shared by every integration test in the JVM; stopping it between
     * classes would break the cached Spring context, whose Feign clients keep its port.
     */
    public static final WireMockServer WIRE_MOCK_SERVER =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {

        if (!WIRE_MOCK_SERVER.isRunning()) {
            WIRE_MOCK_SERVER.start();
        }

        WireMock.configureFor(WIRE_MOCK_SERVER.port());

        String wireMockUrl = "http://localhost:" + WIRE_MOCK_SERVER.port();
        TestPropertyValues.of(
                        "integration.customer-service.url=" + wireMockUrl + CUSTOMER_SERVICE_BASE_PATH,
                        "integration.ledger-service.url=" + wireMockUrl + LEDGER_SERVICE_BASE_PATH)
                .applyTo(configurableApplicationContext.getEnvironment());
    }
}
