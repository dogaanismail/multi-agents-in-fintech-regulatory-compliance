package org.banksolution.service;

import org.banksolution.integration.account.AccountServiceClient;
import org.banksolution.model.PaymentAccounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldResolveBothAccountsWhenTheAccountServiceKnowsThem() {
        when(accountServiceClient.getAccountsByIds(List.of(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID))).thenReturn(List.of(
                createAccountResponse(DESTINATION_ACCOUNT_ID, "DE"),
                createAccountResponse(SOURCE_ACCOUNT_ID, "GB")));

        Optional<PaymentAccounts> paymentAccounts = accountService.loadPaymentAccounts(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID);

        assertThat(paymentAccounts).isPresent();
        assertThat(paymentAccounts.get().source().getId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(paymentAccounts.get().destination().getId()).isEqualTo(DESTINATION_ACCOUNT_ID);
    }

    @Test
    void shouldNotCallTheAccountServiceWhenEitherSideIsMissing() {
        assertThat(accountService.loadPaymentAccounts(null, DESTINATION_ACCOUNT_ID)).isEmpty();
        assertThat(accountService.loadPaymentAccounts(SOURCE_ACCOUNT_ID, null)).isEmpty();

        verifyNoInteractions(accountServiceClient);
    }

    @Test
    void shouldTreatAPartiallyKnownPairAsUnresolved() {
        when(accountServiceClient.getAccountsByIds(List.of(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID)))
                .thenReturn(List.of(createAccountResponse(SOURCE_ACCOUNT_ID, "GB")))
                .thenReturn(List.of(createAccountResponse(DESTINATION_ACCOUNT_ID, "GB")))
                .thenReturn(List.of());

        assertThat(accountService.loadPaymentAccounts(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID)).isEmpty();
        assertThat(accountService.loadPaymentAccounts(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID)).isEmpty();
        assertThat(accountService.loadPaymentAccounts(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID)).isEmpty();
    }

    @Test
    void shouldFlagCrossBorderOnlyWhenBothLocationsAreKnownAndDiffer() {
        assertThat(accountService.isCrossBorderPayment(createPaymentAccounts("GB", "DE"))).isTrue();
        assertThat(accountService.isCrossBorderPayment(createPaymentAccounts("GB", "gb"))).isFalse();
        assertThat(accountService.isCrossBorderPayment(createPaymentAccounts(null, "DE"))).isFalse();
        assertThat(accountService.isCrossBorderPayment(createPaymentAccounts("GB", null))).isFalse();
    }
}
