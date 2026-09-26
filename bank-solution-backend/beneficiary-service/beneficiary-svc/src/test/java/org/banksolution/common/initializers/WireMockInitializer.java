package org.banksolution.common.initializers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String CUSTOMER_SERVICE_BASE_PATH = "/api/v1/customers";

    /**
     * Started once and shared by every integration test in the JVM; stopping it between
     * classes would break the cached Spring context, whose Feign client keeps its port.
     */
    public static final WireMockServer WIRE_MOCK_SERVER =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {

        if (!WIRE_MOCK_SERVER.isRunning()) {
            WIRE_MOCK_SERVER.start();
        }

        WireMock.configureFor(WIRE_MOCK_SERVER.port());

        TestPropertyValues.of(
                        "integration.customer-service.url=http://localhost:" + WIRE_MOCK_SERVER.port() + CUSTOMER_SERVICE_BASE_PATH)
                .applyTo(configurableApplicationContext.getEnvironment());
    }
}
