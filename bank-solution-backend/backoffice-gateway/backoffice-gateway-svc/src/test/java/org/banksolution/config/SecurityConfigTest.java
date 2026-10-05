package org.banksolution.config;

import org.banksolution.common.BaseGatewaySecurityTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.BackofficeUserFixtures.createStaffLogin;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

class SecurityConfigTest extends BaseGatewaySecurityTest {

    private static final String VIEWER = "viewer";
    private static final String OPERATOR = "operator";
    private static final String COMPLIANCE_OFFICER = "compliance-officer";
    private static final String ADMIN = "admin";
    private static final String SUPER_ADMIN = "super-admin";

    private static final String PAYMENTS = "/api/v1/payments";
    private static final String MANUAL_REVIEW_APPROVE = "/api/v1/payment-engine/payments/p-1/manual-review/approve";
    private static final String MANUAL_REVIEW_REJECT = "/api/v1/payment-engine/payments/p-1/manual-review/reject";
    private static final String DECISION_OVERRIDE = "/api/v1/payment-engine/payments/p-1/decision/override";
    private static final String PAYMENT_REQUEST = "/api/v1/payments/request";
    private static final String CUSTOMER = "/api/v1/customers/c-1";
    private static final String CONFIGURATIONS = "/api/v1/configurations";
    private static final String MARL_TRAINING_TRIGGER = "/api/v1/marl/training/trigger";
    private static final String LEDGER_POSTINGS = "/api/v1/ledger/postings";
    private static final String PAYMENT_ENGINE_INITIATE = "/api/v1/payment-engine/payments";
    private static final String UNLISTED_WRITE = "/api/v1/payment-history/rebuild";

    static Stream<Arguments> permissionMatrix() {
        return Stream.of(
                arguments(VIEWER, GET, PAYMENTS, true),
                arguments(OPERATOR, GET, PAYMENTS, true),
                arguments(COMPLIANCE_OFFICER, GET, PAYMENTS, true),
                arguments(ADMIN, GET, PAYMENTS, true),

                arguments(VIEWER, POST, MANUAL_REVIEW_APPROVE, false),
                arguments(OPERATOR, POST, MANUAL_REVIEW_APPROVE, false),
                arguments(COMPLIANCE_OFFICER, POST, MANUAL_REVIEW_APPROVE, true),
                arguments(ADMIN, POST, MANUAL_REVIEW_APPROVE, true),
                arguments(OPERATOR, POST, MANUAL_REVIEW_REJECT, false),
                arguments(COMPLIANCE_OFFICER, POST, MANUAL_REVIEW_REJECT, true),
                arguments(OPERATOR, POST, DECISION_OVERRIDE, false),
                arguments(COMPLIANCE_OFFICER, POST, DECISION_OVERRIDE, true),

                arguments(VIEWER, POST, PAYMENT_REQUEST, false),
                arguments(COMPLIANCE_OFFICER, POST, PAYMENT_REQUEST, false),
                arguments(OPERATOR, POST, PAYMENT_REQUEST, true),
                arguments(ADMIN, POST, PAYMENT_REQUEST, true),
                arguments(VIEWER, PUT, CUSTOMER, false),
                arguments(OPERATOR, PUT, CUSTOMER, true),

                arguments(OPERATOR, DELETE, CUSTOMER, false),
                arguments(ADMIN, DELETE, CUSTOMER, true),
                arguments(OPERATOR, POST, CONFIGURATIONS, false),
                arguments(COMPLIANCE_OFFICER, POST, CONFIGURATIONS, false),
                arguments(ADMIN, POST, CONFIGURATIONS, true),
                arguments(COMPLIANCE_OFFICER, POST, MARL_TRAINING_TRIGGER, false),
                arguments(ADMIN, POST, MARL_TRAINING_TRIGGER, true),
                arguments(OPERATOR, POST, LEDGER_POSTINGS, false),
                arguments(ADMIN, POST, LEDGER_POSTINGS, true),
                arguments(OPERATOR, POST, PAYMENT_ENGINE_INITIATE, false),
                arguments(ADMIN, POST, PAYMENT_ENGINE_INITIATE, true),

                arguments(SUPER_ADMIN, GET, PAYMENTS, false),
                arguments(SUPER_ADMIN, POST, MANUAL_REVIEW_APPROVE, false),
                arguments(SUPER_ADMIN, POST, CONFIGURATIONS, false),
                arguments(ADMIN, POST, UNLISTED_WRITE, false));
    }

    @ParameterizedTest(name = "{0} {1} {2} allowed={3}")
    @MethodSource("permissionMatrix")
    void shouldGrantEachRoleExactlyItsPermissions(
            String realmRole,
            HttpMethod method,
            String path,
            boolean allowed) {

        HttpStatus status = HttpStatus.valueOf(webTestClient
                .mutateWith(createStaffLogin(realmRole))
                .mutateWith(csrf())
                .method(method)
                .uri(path)
                .exchange()
                .returnResult(Void.class)
                .getStatus()
                .value());

        if (allowed) {
            assertThat(status).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        } else {
            assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void shouldAnswerUnauthenticatedApiCallsWith401InsteadOfALoginRedirect() {
        webTestClient.get().uri(PAYMENTS)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectStateChangingRequestsWithoutACsrfToken() {
        webTestClient.mutateWith(createStaffLogin(COMPLIANCE_OFFICER))
                .post().uri(MANUAL_REVIEW_APPROVE)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldIssueAReadableCsrfCookieForTheBrowser() {
        webTestClient.mutateWith(createStaffLogin(VIEWER))
                .get().uri("/api/v1/me")
                .exchange()
                .expectCookie().exists("XSRF-TOKEN")
                .expectCookie().httpOnly("XSRF-TOKEN", false);
    }

    @Test
    void shouldLetAUserWithoutBusinessPermissionsReadTheirOwnProfile() {
        webTestClient.mutateWith(createStaffLogin(SUPER_ADMIN))
                .get().uri("/api/v1/me")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldLeaveTheHealthEndpointOpenForProbes() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldStartTheKeycloakLoginWithAPkceChallenge() {
        webTestClient.get().uri("/oauth2/authorization/keycloak")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().value("Location", location -> assertThat(location)
                        .startsWith("http://keycloak:8180/realms/bank-staff/protocol/openid-connect/auth")
                        .contains("code_challenge=")
                        .contains("code_challenge_method=S256"));
    }
}
