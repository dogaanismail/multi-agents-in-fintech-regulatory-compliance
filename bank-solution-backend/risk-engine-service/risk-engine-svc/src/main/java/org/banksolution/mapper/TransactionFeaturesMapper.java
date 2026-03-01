package org.banksolution.mapper;

import com.aml.fraud.TransactionFeatures;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.MarlPaymentType;
import org.banksolution.integration.account.dto.AccountResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static org.banksolution.mapper.PaymentTypeMapper.toMarlPaymentType;

@UtilityClass
public class TransactionFeaturesMapper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // TODO: Replace with actual bank ledger account from account-service
    private static final String BANK_CASH_ACCOUNT_PREFIX = "9000";

    public static TransactionFeatures toTransactionFeatures(
            RiskCheckRequestEntity entity,
            AccountResponse senderAccount,
            AccountResponse receiverAccount) {

        Instant timestamp = Instant.ofEpochMilli(entity.getRequestTimestamp());
        LocalTime time = timestamp.atZone(ZoneId.systemDefault()).toLocalTime();
        LocalDate date = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();

        AccountResponse customerAccount = senderAccount != null ? senderAccount : receiverAccount;
        String bankLocation = customerAccount.getBankLocation();

        String senderAccountNumber = senderAccount != null ? senderAccount.getAccountNumber() : generateBankCashAccountNumber();
        String receiverAccountNumber = receiverAccount != null ? receiverAccount.getAccountNumber() : generateBankCashAccountNumber();
        String senderBankLocation = senderAccount != null ? senderAccount.getBankLocation() : bankLocation;
        String receiverBankLocation = receiverAccount != null ? receiverAccount.getBankLocation() : bankLocation;

        boolean isCrossBorder = !senderBankLocation.equals(receiverBankLocation);
        MarlPaymentType marlPaymentType = toMarlPaymentType(entity.getPaymentType(), isCrossBorder);

        return TransactionFeatures.newBuilder()
                .setPaymentId(entity.getPaymentId())
                .setTime(time.format(TIME_FORMATTER))
                .setDate(date.format(DATE_FORMATTER))
                .setSenderAccount(senderAccountNumber)
                .setReceiverAccount(receiverAccountNumber)
                .setAmount(entity.getAmount().doubleValue())
                .setPaymentCurrency(entity.getFromCurrency())
                .setReceivedCurrency(entity.getToCurrency())
                .setSenderBankLocation(senderBankLocation)
                .setReceiverBankLocation(receiverBankLocation)
                .setPaymentType(marlPaymentType.getValue())
                .build();
    }

    private static String generateBankCashAccountNumber() {
        // TODO: Replace with actual bank ledger account
        long randomPart = ThreadLocalRandom.current().nextLong(100000, 999999);
        return BANK_CASH_ACCOUNT_PREFIX + randomPart;
    }
}
