package org.banksolution.service;

import com.aml.fraud.TransactionFeatures;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.MarlPaymentType;
import org.banksolution.exception.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.IntegrationClientFixtures.createAccountResponse;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createDepositRiskCheckRequestEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createWithdrawalRiskCheckRequestEntity;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionFeatureServiceTest {

    private static final String SENDER_ACCOUNT_NUMBER = "GB000111";
    private static final String RECEIVER_ACCOUNT_NUMBER = "DE000222";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionFeatureService transactionFeatureService;

    @Test
    void shouldFetchBothAccountsInOneBatchForATransfer() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        UUID sourceAccountId = UUID.fromString(riskCheckRequest.getSourceAccountId());
        UUID destinationAccountId = UUID.fromString(riskCheckRequest.getDestinationAccountId());
        when(accountService.getAccountsByIds(List.of(sourceAccountId, destinationAccountId)))
                .thenReturn(List.of(
                        createAccountResponse(sourceAccountId, SENDER_ACCOUNT_NUMBER, "GB"),
                        createAccountResponse(destinationAccountId, RECEIVER_ACCOUNT_NUMBER, "DE")));

        TransactionFeatures features = transactionFeatureService.getTransactionFeatures(riskCheckRequest);

        assertThat(features.getSenderAccount()).isEqualTo(SENDER_ACCOUNT_NUMBER);
        assertThat(features.getReceiverAccount()).isEqualTo(RECEIVER_ACCOUNT_NUMBER);
        assertThat(features.getPaymentType()).isEqualTo(MarlPaymentType.CROSS_BORDER.getValue());
    }

    @Test
    void shouldFetchOnlyTheDestinationAccountForADeposit() {
        RiskCheckRequestEntity riskCheckRequest = createDepositRiskCheckRequestEntity();
        UUID destinationAccountId = UUID.fromString(riskCheckRequest.getDestinationAccountId());
        when(accountService.getAccountById(destinationAccountId))
                .thenReturn(createAccountResponse(destinationAccountId, RECEIVER_ACCOUNT_NUMBER, "GB"));

        TransactionFeatures features = transactionFeatureService.getTransactionFeatures(riskCheckRequest);

        assertThat(features.getReceiverAccount()).isEqualTo(RECEIVER_ACCOUNT_NUMBER);
        assertThat(features.getPaymentType()).isEqualTo(MarlPaymentType.CASH_DEPOSIT.getValue());
    }

    @Test
    void shouldFetchOnlyTheSourceAccountForAWithdrawal() {
        RiskCheckRequestEntity riskCheckRequest = createWithdrawalRiskCheckRequestEntity();
        UUID sourceAccountId = UUID.fromString(riskCheckRequest.getSourceAccountId());
        when(accountService.getAccountById(sourceAccountId))
                .thenReturn(createAccountResponse(sourceAccountId, SENDER_ACCOUNT_NUMBER, "GB"));

        TransactionFeatures features = transactionFeatureService.getTransactionFeatures(riskCheckRequest);

        assertThat(features.getSenderAccount()).isEqualTo(SENDER_ACCOUNT_NUMBER);
        assertThat(features.getPaymentType()).isEqualTo(MarlPaymentType.CASH_WITHDRAWAL.getValue());
    }

    @Test
    void shouldFailWhenTheBatchLookupIsMissingARequestedAccount() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        UUID sourceAccountId = UUID.fromString(riskCheckRequest.getSourceAccountId());
        UUID destinationAccountId = UUID.fromString(riskCheckRequest.getDestinationAccountId());
        when(accountService.getAccountsByIds(List.of(sourceAccountId, destinationAccountId)))
                .thenReturn(List.of(createAccountResponse(sourceAccountId, SENDER_ACCOUNT_NUMBER, "GB")));

        assertThatThrownBy(() -> transactionFeatureService.getTransactionFeatures(riskCheckRequest))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(destinationAccountId.toString());
    }
}
