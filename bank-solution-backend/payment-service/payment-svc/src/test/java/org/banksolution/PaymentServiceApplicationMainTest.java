package org.banksolution;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class PaymentServiceApplicationMainTest {

    @Test
    void shouldBootTheApplicationClassWithTheGivenArguments() {
        String[] launchArguments = {"--spring.profiles.active=local"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            PaymentServiceApplication.main(launchArguments);

            springApplication.verify(() -> SpringApplication.run(PaymentServiceApplication.class, launchArguments));
        }
    }
}
