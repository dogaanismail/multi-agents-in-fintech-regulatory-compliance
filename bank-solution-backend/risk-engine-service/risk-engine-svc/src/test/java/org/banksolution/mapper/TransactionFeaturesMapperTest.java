package org.banksolution.mapper;

import com.aml.fraud.TransactionFeatures;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.MarlPaymentType;
import org.banksolution.integration.account.dto.AccountResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.IntegrationClientFixtures.createAccountResponse;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createDepositRiskCheckRequestEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createWithdrawalRiskCheckRequestEntity;
import static org.banksolution.mapper.TransactionFeaturesMapper.toTransactionFeatures;

class TransactionFeaturesMapperTest {

    private static final String SENDER_ACCOUNT_NUMBER = "GB000111";
    private static final String RECEIVER_ACCOUNT_NUMBER = "DE000222";
    private static final String BANK_CASH_ACCOUNT_PREFIX = "9000";

    @Test
    void shouldCopyPaymentDetailsAndBothAccountNumbersForATransfer() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();
        AccountResponse senderAccount = senderAccount(entity, "GB");
        AccountResponse receiverAccount = receiverAccount(entity, "GB");

        TransactionFeatures features = toTransactionFeatures(entity, senderAccount, receiverAccount);

        assertThat(features.getPaymentId()).isEqualTo(entity.getPaymentId());
        assertThat(features.getSenderAccount()).isEqualTo(SENDER_ACCOUNT_NUMBER);
        assertThat(features.getReceiverAccount()).isEqualTo(RECEIVER_ACCOUNT_NUMBER);
        assertThat(features.getAmount()).isEqualTo(entity.getAmount().doubleValue());
        assertThat(features.getPaymentCurrency()).isEqualTo(entity.getFromCurrency());
        assertThat(features.getReceivedCurrency()).isEqualTo(entity.getToCurrency());
        assertThat(features.getSenderBankLocation()).isEqualTo("GB");
        assertThat(features.getReceiverBankLocation()).isEqualTo("GB");
    }

    @Test
    void shouldRenderTimeAndDateFromTheRequestTimestampInTheSystemZone() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();
        Instant timestamp = Instant.ofEpochMilli(entity.getRequestTimestamp());

        TransactionFeatures features = toTransactionFeatures(
                entity, senderAccount(entity, "GB"), receiverAccount(entity, "GB"));

        assertThat(features.getTime()).isEqualTo(timestamp.atZone(ZoneId.systemDefault())
                .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        assertThat(features.getDate()).isEqualTo(timestamp.atZone(ZoneId.systemDefault())
                .toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    @Test
    void shouldClassifyATransferAcrossBankLocationsAsCrossBorder() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();

        TransactionFeatures features = toTransactionFeatures(
                entity, senderAccount(entity, "GB"), receiverAccount(entity, "DE"));

        assertThat(features.getPaymentType()).isEqualTo(MarlPaymentType.CROSS_BORDER.getValue());
    }

    @Test
    void shouldClassifyATransferWithinOneBankLocationAsAch() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();

        TransactionFeatures features = toTransactionFeatures(
                entity, senderAccount(entity, "GB"), receiverAccount(entity, "GB"));

        assertThat(features.getPaymentType()).isEqualTo(MarlPaymentType.ACH.getValue());
    }

    @Test
    void shouldSubstituteABankCashAccountForTheMissingSenderOnADeposit() {
        RiskCheckRequestEntity entity = createDepositRiskCheckRequestEntity();
        AccountResponse receiverAccount = receiverAccount(entity, "GB");

        TransactionFeatures features = toTransactionFeatures(entity, null, receiverAccount);

        assertThat(features.getSenderAccount()).startsWith(BANK_CASH_ACCOUNT_PREFIX).hasSize(10);
        assertThat(features.getReceiverAccount()).isEqualTo(RECEIVER_ACCOUNT_NUMBER);
        assertThat(features.getSenderBankLocation()).isEqualTo("GB");
        assertThat(features.getReceiverBankLocation()).isEqualTo("GB");
        assertThat(features.getPaymentType()).isEqualTo(MarlPaymentType.CASH_DEPOSIT.getValue());
    }

    @Test
    void shouldSubstituteABankCashAccountForTheMissingReceiverOnAWithdrawal() {
        RiskCheckRequestEntity entity = createWithdrawalRiskCheckRequestEntity();
        AccountResponse senderAccount = senderAccount(entity, "GB");

        TransactionFeatures features = toTransactionFeatures(entity, senderAccount, null);

        assertThat(features.getSenderAccount()).isEqualTo(SENDER_ACCOUNT_NUMBER);
        assertThat(features.getReceiverAccount()).startsWith(BANK_CASH_ACCOUNT_PREFIX).hasSize(10);
        assertThat(features.getSenderBankLocation()).isEqualTo("GB");
        assertThat(features.getReceiverBankLocation()).isEqualTo("GB");
        assertThat(features.getPaymentType()).isEqualTo(MarlPaymentType.CASH_WITHDRAWAL.getValue());
    }

    private AccountResponse senderAccount(RiskCheckRequestEntity entity, String bankLocation) {
        return createAccountResponse(
                UUID.fromString(entity.getSourceAccountId()), SENDER_ACCOUNT_NUMBER, bankLocation);
    }

    private AccountResponse receiverAccount(RiskCheckRequestEntity entity, String bankLocation) {
        return createAccountResponse(
                UUID.fromString(entity.getDestinationAccountId()), RECEIVER_ACCOUNT_NUMBER, bankLocation);
    }
}
