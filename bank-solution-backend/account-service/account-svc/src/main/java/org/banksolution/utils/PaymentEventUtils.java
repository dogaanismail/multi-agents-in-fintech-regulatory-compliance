package org.banksolution.utils;

import com.aml.payment.PaymentCompletedEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.enums.Currency;
import org.banksolution.entity.enums.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

@UtilityClass
@Slf4j
public class PaymentEventUtils {

    public static UUID determineAccountId(
            PaymentCompletedEvent event,
            PaymentType paymentType) {

        return switch (paymentType) {
            case TRANSFER_IN, DEPOSIT -> {
                if (event.getDestinationAccountId() == null) {
                    throw new IllegalArgumentException("Destination account ID is required for " + paymentType);
                }

                yield UUID.fromString(event.getDestinationAccountId());
            }
            case TRANSFER_OUT, WITHDRAWAL -> {
                if (event.getSourceAccountId() == null) {
                    throw new IllegalArgumentException("Source account ID is required for " + paymentType);
                }

                yield UUID.fromString(event.getSourceAccountId());
            }
        };
    }

    public static BigDecimal calculateNewBalance(
            BigDecimal currentBalance,
            BigDecimal amount,
            PaymentType paymentType) {

        return switch (paymentType) {
            case TRANSFER_IN, DEPOSIT -> currentBalance.add(amount);
            case TRANSFER_OUT, WITHDRAWAL -> currentBalance.subtract(amount);
        };
    }

    public static void validateBalance(
            BigDecimal newBalance,
            UUID accountId,
            Currency currency) {

        log.debug("Validating balance for account: {}, currency: {}, new balance: {}",
                accountId, currency, newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient balance for account: " + accountId);
        }
    }
}

