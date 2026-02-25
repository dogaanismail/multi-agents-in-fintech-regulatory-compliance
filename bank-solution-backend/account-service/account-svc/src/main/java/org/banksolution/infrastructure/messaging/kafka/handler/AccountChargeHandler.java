package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.account.AccountChargeCompletedEvent;
import com.aml.account.AccountChargeRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.exception.BalanceNotFoundException;
import org.banksolution.exception.InsufficientFundsException;
import org.banksolution.infrastructure.messaging.kafka.mapper.AccountChargeCompletedEventMapper;
import org.banksolution.infrastructure.messaging.kafka.producer.AccountChargeCompletedEventProducer;
import org.banksolution.model.response.AccountChargeResponse;
import org.banksolution.processor.AccountChargeProcessorFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountChargeHandler {

    private final AccountChargeProcessorFactory accountChargeProcessorFactory;
    private final AccountChargeCompletedEventProducer accountChargeCompletedEventProducer;

    @Transactional
    public void handle(AccountChargeRequestedEvent event) {
        log.info("Handling account charge: paymentId:{}, paymentType:{}, amount:{}, currency:{}",
                event.getPaymentId(),
                event.getPaymentType(),
                event.getAmount(),
                event.getCurrency());

        AccountChargeResponse result = charge(event);
        AccountChargeCompletedEvent completedEvent = AccountChargeCompletedEventMapper.toAvroEvent(event, result);
        accountChargeCompletedEventProducer.publish(completedEvent);
    }

    private AccountChargeResponse charge(AccountChargeRequestedEvent event) {
        try {
            accountChargeProcessorFactory.getProcessor(event.getPaymentType()).process(event);
            log.info("Account charge successful for paymentId:{}", event.getPaymentId());
            return AccountChargeResponse.success();
        } catch (InsufficientFundsException e) {
            log.warn("Insufficient funds for paymentId:{}: {}", event.getPaymentId(), e.getMessage());
            return AccountChargeResponse.failure(e.getMessage());
        } catch (AccountNotFoundException e) {
            log.warn("Account not found for paymentId:{}: {}", event.getPaymentId(), e.getMessage());
            return AccountChargeResponse.failure(e.getMessage());
        } catch (BalanceNotFoundException e) {
            log.warn("Balance not found for paymentId:{}: {}", event.getPaymentId(), e.getMessage());
            return AccountChargeResponse.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during account charge for paymentId:{}: {}", event.getPaymentId(), e.getMessage(), e);
            return AccountChargeResponse.failure("Unexpected error during account charge: " + e.getMessage());
        }
    }
}
