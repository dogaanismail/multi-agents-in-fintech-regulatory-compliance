package org.banksolution;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class BeneficiaryServiceApplicationMainTest {

    @Test
    void shouldBootTheApplicationClassWithTheGivenArguments() {
        String[] launchArguments = {"--spring.profiles.active=local"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            BeneficiaryServiceApplication.main(launchArguments);

            springApplication.verify(() -> SpringApplication.run(BeneficiaryServiceApplication.class, launchArguments));
        }
    }
}
