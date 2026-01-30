package org.banksolution.service;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProcessor {

    private final TransactionSnapshotService transactionSnapshotService;
    private final CustomerProfileUpdateService customerProfileUpdateService;

    @Transactional
    public void process(PaymentCompletedEvent event) {
        String paymentId = event.getPaymentId();
        UUID customerId = UUID.fromString(event.getCustomerId());

        log.debug("Processing PaymentCompletedEvent: paymentId: {}, customerId: {}", paymentId, customerId);

        transactionSnapshotService.saveIfNotExists(event);
        customerProfileUpdateService.updateProfile(customerId);

        log.info("Successfully processed PaymentCompletedEvent: paymentId: {}", paymentId);
    }
}
