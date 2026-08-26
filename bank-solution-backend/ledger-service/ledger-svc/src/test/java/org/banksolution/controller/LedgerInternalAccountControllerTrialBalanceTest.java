package org.banksolution.controller;

import org.banksolution.enums.Currency;
import org.banksolution.model.response.TrialBalanceResponse;
import org.banksolution.service.LedgerAccountService;
import org.banksolution.service.LedgerInternalAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * A real TigerBeetle book always nets to zero, so the unbalanced branch can only be
 * exercised against stubbed services.
 */
@ExtendWith(MockitoExtension.class)
class LedgerInternalAccountControllerTrialBalanceTest {

    @Mock
    private LedgerInternalAccountService ledgerInternalAccountService;

    @Mock
    private LedgerAccountService ledgerAccountService;

    @InjectMocks
    private LedgerInternalAccountController ledgerInternalAccountController;

    @Test
    void shouldFlagABookWhoseInternalAccountsDoNotOffsetTheCustomerWallets() {
        when(ledgerInternalAccountService.getInternalAccounts(Currency.GBP)).thenReturn(List.of());
        when(ledgerInternalAccountService.netBalance(Currency.GBP)).thenReturn(new BigDecimal("-1000.00"));
        when(ledgerAccountService.netBalanceOfCustomerWallets(Currency.GBP)).thenReturn(new BigDecimal("999.99"));

        TrialBalanceResponse trialBalanceResponse =
                ledgerInternalAccountController.getTrialBalance(Currency.GBP).getBody();

        assert trialBalanceResponse != null;
        assertThat(trialBalanceResponse.getNet()).isEqualByComparingTo("-0.01");
        assertThat(trialBalanceResponse.isBalanced()).isFalse();
        assertThat(trialBalanceResponse.getInternalAccounts()).isEmpty();
    }
}
