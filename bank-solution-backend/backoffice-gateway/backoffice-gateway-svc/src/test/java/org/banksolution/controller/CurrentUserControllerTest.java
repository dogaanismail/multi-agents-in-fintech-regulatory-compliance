package org.banksolution.controller;

import org.banksolution.common.BaseGatewaySecurityTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.BackofficeUserFixtures.createOfficerLogin;

class CurrentUserControllerTest extends BaseGatewaySecurityTest {

    @Test
    void shouldDescribeTheSignedInUserFromTheirIdToken() {
        webTestClient.mutateWith(createOfficerLogin("officer", "Clara Compliance", "officer@bank-solution.local"))
                .get().uri("/api/v1/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("officer")
                .jsonPath("$.fullName").isEqualTo("Clara Compliance")
                .jsonPath("$.email").isEqualTo("officer@bank-solution.local")
                .jsonPath("$.roles").value(roles -> assertThat(roles.toString()).contains("compliance-officer", "viewer"));
    }
}
