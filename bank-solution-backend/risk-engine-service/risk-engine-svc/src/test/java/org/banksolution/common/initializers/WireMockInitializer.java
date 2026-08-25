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

    public static final String ACCOUNT_SERVICE_BASE_PATH = "/api/v1/accounts";
    public static final String NETWORK_TOPOLOGY_SERVICE_BASE_PATH = "/api/v1/networks";
    public static final String CUSTOMER_PROFILE_SERVICE_BASE_PATH = "/api/v1/customer-profiles";

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
                        "integration.account-service.url=" + wireMockUrl + ACCOUNT_SERVICE_BASE_PATH,
                        "integration.network-topology-service.url=" + wireMockUrl + NETWORK_TOPOLOGY_SERVICE_BASE_PATH,
                        "integration.customer-profile-service.url=" + wireMockUrl + CUSTOMER_PROFILE_SERVICE_BASE_PATH)
                .applyTo(configurableApplicationContext.getEnvironment());
    }
}
