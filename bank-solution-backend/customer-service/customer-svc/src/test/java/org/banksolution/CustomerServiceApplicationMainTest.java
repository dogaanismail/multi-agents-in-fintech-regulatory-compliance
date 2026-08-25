package org.banksolution;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class CustomerServiceApplicationMainTest {

    @Test
    void shouldBootTheApplicationClassWithTheGivenArguments() {
        String[] launchArguments = {"--spring.profiles.active=local"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            CustomerServiceApplication.main(launchArguments);

            springApplication.verify(() -> SpringApplication.run(CustomerServiceApplication.class, launchArguments));
        }
    }
}
