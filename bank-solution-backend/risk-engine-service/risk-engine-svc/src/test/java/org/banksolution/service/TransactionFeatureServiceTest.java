package org.banksolution.service;

import com.aml.fraud.TransactionFeatures;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.PaymentType;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.integration.account.dto.AccountResponse;
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
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createRiskCheckRequestEntity;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionFeatureServiceTest {

    private static final UUID SOURCE_ACCOUNT_ID = UUID.randomUUID();
    private static final UUID DESTINATION_ACCOUNT_ID = UUID.randomUUID();
    private static final String EXTERNAL_ACCOUNT_PREFIX = "9000";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionFeatureService transactionFeatureService;

    @Test
    void shouldBuildTransferFeaturesFromBothInternalAccounts() {
        RiskCheckRequestEntity riskCheckRequestEntity = createRiskCheckRequestEntity(
                PaymentType.TRANSFER_OUT,
                SOURCE_ACCOUNT_ID.toString(),
                DESTINATION_ACCOUNT_ID.toString());
        AccountResponse senderAccount = createAccountResponse(SOURCE_ACCOUNT_ID, "1111111111", "GB");
        AccountResponse receiverAccount = createAccountResponse(DESTINATION_ACCOUNT_ID, "2222222222", "DE");
        when(accountService.getAccountsByIds(List.of(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID)))
                .thenReturn(List.of(senderAccount, receiverAccount));

        TransactionFeatures transactionFeatures =
                transactionFeatureService.getTransactionFeatures(riskCheckRequestEntity);

        assertThat(transactionFeatures.getSenderAccount()).isEqualTo("1111111111");
        assertThat(transactionFeatures.getReceiverAccount()).isEqualTo("2222222222");
        assertThat(transactionFeatures.getSenderBankLocation()).isEqualTo("GB");
        assertThat(transactionFeatures.getReceiverBankLocation()).isEqualTo("DE");
    }

    @Test
    void shouldTreatMissingDestinationAccountAsExternalReceiver() {
        RiskCheckRequestEntity riskCheckRequestEntity = createRiskCheckRequestEntity(
                PaymentType.TRANSFER_OUT, SOURCE_ACCOUNT_ID.toString(), null);
        AccountResponse senderAccount = createAccountResponse(SOURCE_ACCOUNT_ID, "1111111111", "GB");
        when(accountService.getAccountsByIds(List.of(SOURCE_ACCOUNT_ID)))
                .thenReturn(List.of(senderAccount));

        TransactionFeatures transactionFeatures =
                transactionFeatureService.getTransactionFeatures(riskCheckRequestEntity);

        assertThat(transactionFeatures.getSenderAccount()).isEqualTo("1111111111");
        assertThat(transactionFeatures.getReceiverAccount()).startsWith(EXTERNAL_ACCOUNT_PREFIX);
        assertThat(transactionFeatures.getSenderBankLocation()).isEqualTo("GB");
        assertThat(transactionFeatures.getReceiverBankLocation()).isEqualTo("GB");
    }

    @Test
    void shouldTreatMissingSourceAccountAsExternalSender() {
        RiskCheckRequestEntity riskCheckRequestEntity = createRiskCheckRequestEntity(
                PaymentType.TRANSFER_IN, null, DESTINATION_ACCOUNT_ID.toString());
        AccountResponse receiverAccount = createAccountResponse(DESTINATION_ACCOUNT_ID, "2222222222", "DE");
        when(accountService.getAccountsByIds(List.of(DESTINATION_ACCOUNT_ID)))
                .thenReturn(List.of(receiverAccount));

        TransactionFeatures transactionFeatures =
                transactionFeatureService.getTransactionFeatures(riskCheckRequestEntity);

        assertThat(transactionFeatures.getSenderAccount()).startsWith(EXTERNAL_ACCOUNT_PREFIX);
        assertThat(transactionFeatures.getReceiverAccount()).isEqualTo("2222222222");
        assertThat(transactionFeatures.getSenderBankLocation()).isEqualTo("DE");
        assertThat(transactionFeatures.getReceiverBankLocation()).isEqualTo("DE");
    }

    @Test
    void shouldThrowWhenAnInternalAccountIsMissingFromTheLookup() {
        RiskCheckRequestEntity riskCheckRequestEntity = createRiskCheckRequestEntity(
                PaymentType.TRANSFER_OUT,
                SOURCE_ACCOUNT_ID.toString(),
                DESTINATION_ACCOUNT_ID.toString());
        AccountResponse senderAccount = createAccountResponse(SOURCE_ACCOUNT_ID, "1111111111", "GB");
        when(accountService.getAccountsByIds(List.of(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID)))
                .thenReturn(List.of(senderAccount));

        assertThatThrownBy(() -> transactionFeatureService.getTransactionFeatures(riskCheckRequestEntity))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
