package org.banksolution.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import static com.aml.payment.PaymentType.*;

@UtilityClass
public class TransactionClassificationUtil {

    private static final List<String> CASH_TRANSACTION_TYPES = List.of(DEPOSIT.name(), WITHDRAWAL.name());
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("10000");
    private static final int NIGHT_START_HOUR = 22;
    private static final int NIGHT_END_HOUR = 6;

    public boolean isCashTransaction(String paymentType) {
        return CASH_TRANSACTION_TYPES.contains(paymentType);
    }

    public boolean isLargeTransaction(BigDecimal amount) {
        return amount != null && amount.compareTo(LARGE_TRANSACTION_THRESHOLD) > 0;
    }

    public boolean isNightTransaction(LocalDateTime transactionTime) {
        int hour = transactionTime.getHour();
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR;
    }

    public boolean isWeekendTransaction(LocalDateTime transactionTime) {
        DayOfWeek day = transactionTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
