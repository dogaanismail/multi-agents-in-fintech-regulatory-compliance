package org.banksolution.common.initializers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.banksolution.servicesecurity.testing.ServiceTokenStubs.SERVICE_TOKEN_PATH;

public class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @SuppressWarnings({"resource", "java:S2095"})
    public static final WireMockServer WIRE_MOCK_SERVER =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {
        if (!WIRE_MOCK_SERVER.isRunning()) {
            WIRE_MOCK_SERVER.start();
        }
        String wireMockUrl = WIRE_MOCK_SERVER.baseUrl();
        TestPropertyValues.of(
                        "spring.security.oauth2.client.provider.bank-internal.token-uri=" + wireMockUrl + SERVICE_TOKEN_PATH,
                        "CUSTOMER_SERVICE_URL=" + wireMockUrl,
                        "ACCOUNT_SERVICE_URL=" + wireMockUrl,
                        "PAYMENT_SERVICE_URL=" + wireMockUrl,
                        "PAYMENT_HISTORY_SERVICE_URL=" + wireMockUrl,
                        "management.tracing.export.enabled=false")
                .applyTo(configurableApplicationContext.getEnvironment());
    }
}
