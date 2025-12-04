package org.banksolution.handler;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.AccountBalanceEntity;
import org.banksolution.entity.enums.Currency;
import org.banksolution.entity.enums.PaymentType;
import org.banksolution.exception.BalanceNotFoundException;
import org.banksolution.repository.AccountBalanceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.banksolution.utils.PaymentEventUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceHandler {

    private final AccountBalanceRepository accountBalanceRepository;

    @Transactional
    public void processPaymentCompletedEvent(PaymentCompletedEvent event) {

        log.info("Processing payment event: eventId:{}, paymentId:{}, type:{}",
                event.getEventId(), event.getPaymentId(), event.getPaymentType());

        PaymentType paymentType = PaymentType.valueOf(event.getPaymentType().name());
        Currency currency = Currency.valueOf(event.getCurrency());
        BigDecimal amount = new BigDecimal(event.getAmount());

        UUID accountId = determineAccountId(event, paymentType);

        updateBalance(accountId, currency, amount, paymentType);
    }

    private void updateBalance(
            UUID accountId,
            Currency currency,
            BigDecimal amount,
            PaymentType paymentType) {

        log.info("Updating balance for account:{}, currency:{}, amount:{}, type:{}",
                accountId, currency, amount, paymentType);

        AccountBalanceEntity balance = accountBalanceRepository.findByAccountIdAndCurrency(accountId, currency)
                .orElseThrow(() -> new BalanceNotFoundException(accountId, currency));

        BigDecimal newBalance = calculateNewBalance(balance.getAvailableBalance(), amount, paymentType);

        validateBalance(newBalance, accountId, currency);

        balance.setAvailableBalance(newBalance);
        accountBalanceRepository.save(balance);

        log.info("Balance updated successfully. New balance: {} {}", newBalance, currency);
    }
}

