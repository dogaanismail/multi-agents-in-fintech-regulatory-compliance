package org.banksolution.fixtures;

import org.banksolution.integration.account.model.AccountResponse;
import org.banksolution.integration.account.model.AccountWalletResponse;
import org.banksolution.integration.customer.model.CustomerResponse;
import org.banksolution.integration.payment.model.PaymentRequestResponse;
import org.banksolution.integration.paymenthistory.model.PaymentHistoryPage;
import org.banksolution.integration.paymenthistory.model.PaymentHistoryResponse;
import org.banksolution.model.request.MobilePaymentRequest;
import org.banksolution.model.request.OnboardingRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class MobileGatewayFixtures {

    public static final String CALLER_SUBJECT = "6b1e3c1a-7d2f-4a8b-9c0d-1e2f3a4b5c6d";
    public static final String CALLER_EMAIL = "ada@example.com";
    public static final UUID CALLER_CUSTOMER_ID = UUID.fromString("0f4a7b2c-1d3e-4f5a-8b9c-0d1e2f3a4b5c");
    public static final UUID OTHER_CUSTOMER_ID = UUID.fromString("9e8d7c6b-5a49-4382-a1b0-c9d8e7f6a5b4");
    public static final UUID CALLER_ACCOUNT_ID = UUID.fromString("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
    public static final UUID OTHER_ACCOUNT_ID = UUID.fromString("5d4c3b2a-1f0e-4d9c-8b7a-6f5e4d3c2b1a");
    public static final String CUSTOMER_IDENTITY_PATH = "/api/v1/customers/identities/" + CALLER_SUBJECT;

    private MobileGatewayFixtures() {
    }

    public static JwtRequestPostProcessor createCustomerJwt() {
        return jwt().jwt(token -> token.subject(CALLER_SUBJECT).claim("email", CALLER_EMAIL));
    }

    public static CustomerResponse createCallerCustomerResponse() {
        return new CustomerResponse(CALLER_CUSTOMER_ID, "Ada", "Lovelace", CALLER_EMAIL);
    }

    public static OnboardingRequest createOnboardingRequest() {
        return new OnboardingRequest("Ada", "Lovelace", "+905551112233", LocalDate.parse("1990-12-10"), "GB", "London", "GB");
    }

    public static AccountResponse createAccountResponse(
            UUID accountId,
            UUID customerId) {

        return new AccountResponse(accountId, customerId, "GB00BANK0001", "CHECKING", "ACTIVE",
                List.of(new AccountWalletResponse("GBP", new BigDecimal("250.00"), new BigDecimal("200.00"))));
    }

    public static MobilePaymentRequest createMobilePaymentRequest(UUID sourceAccountId) {
        return new MobilePaymentRequest(sourceAccountId, OTHER_ACCOUNT_ID, new BigDecimal("42.50"), "GBP", "Rent");
    }

    public static PaymentRequestResponse createPaymentRequestResponse() {
        return new PaymentRequestResponse(UUID.randomUUID(), CALLER_ACCOUNT_ID, OTHER_ACCOUNT_ID,
                new BigDecimal("42.50"), "GBP", "Rent", Instant.parse("2026-10-06T09:00:00Z"));
    }

    public static PaymentHistoryPage createPaymentHistoryPage(String... internalStatuses) {
        return new PaymentHistoryPage(Arrays.stream(internalStatuses)
                .map(internalStatus -> new PaymentHistoryResponse(UUID.randomUUID(), CALLER_ACCOUNT_ID, OTHER_ACCOUNT_ID,
                        BigDecimal.TEN, "GBP", "Rent", internalStatus, Instant.parse("2026-10-06T09:00:00Z")))
                .toList());
    }
}
