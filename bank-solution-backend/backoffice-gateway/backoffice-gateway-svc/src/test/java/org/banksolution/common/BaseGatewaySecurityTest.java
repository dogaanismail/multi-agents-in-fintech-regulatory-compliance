package org.banksolution.common;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.banksolution.fixtures.BackofficeUserFixtures.KEYCLOAK_REGISTRATION_ID;
import static org.banksolution.fixtures.BackofficeUserFixtures.createKeycloakClientRegistration;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public abstract class BaseGatewaySecurityTest {

    @Autowired
    protected WebTestClient webTestClient;

    @MockitoBean
    protected ReactiveClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void stubKeycloakClientRegistration() {
        when(clientRegistrationRepository.findByRegistrationId(KEYCLOAK_REGISTRATION_ID))
                .thenReturn(Mono.just(createKeycloakClientRegistration()));
    }
}
