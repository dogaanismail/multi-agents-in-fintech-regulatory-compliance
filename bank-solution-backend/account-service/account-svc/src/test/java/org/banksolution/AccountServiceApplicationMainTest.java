package org.banksolution;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class AccountServiceApplicationMainTest {

    @Test
    void shouldBootTheApplicationClassWithTheGivenArguments() {
        String[] launchArguments = {"--spring.profiles.active=local"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            AccountServiceApplication.main(launchArguments);

            springApplication.verify(() -> SpringApplication.run(AccountServiceApplication.class, launchArguments));
        }
    }
}
