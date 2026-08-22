package org.banksolution.common.initializers;

import org.banksolution.common.containers.TigerBeetleContainer;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class TigerBeetleInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Started once and shared by every integration test in the JVM, so it deliberately
     * outlives any try-with-resources scope. Testcontainers' Ryuk sidecar removes it
     * when the JVM exits.
     */
    @SuppressWarnings({"resource", "java:S2095"})
    private static final TigerBeetleContainer TIGER_BEETLE_CONTAINER = new TigerBeetleContainer();

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {

        if (!TIGER_BEETLE_CONTAINER.isRunning()) {
            TIGER_BEETLE_CONTAINER.start();
        }

        TestPropertyValues.of(
                        "ledger.tigerbeetle.cluster-id=0",
                        "ledger.tigerbeetle.addresses=" + TIGER_BEETLE_CONTAINER.address())
                .applyTo(configurableApplicationContext.getEnvironment());
    }
}
