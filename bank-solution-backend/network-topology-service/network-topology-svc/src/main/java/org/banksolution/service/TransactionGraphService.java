package org.banksolution.service;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.repository.NetworkGraphRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionGraphService {

    private final NetworkGraphRepository networkGraphRepository;

    public void createTransactionRelationship(PaymentCompletedEvent event) {
        String sourceAccountId = event.getSourceAccountId();
        String destinationAccountId = event.getDestinationAccountId();
        String paymentId = event.getPaymentId();

        log.info("Creating transaction relationship: {} -> {} for payment: {}",
                sourceAccountId,
                destinationAccountId,
                paymentId);

        networkGraphRepository.createTransactionRelationship(
                sourceAccountId,
                destinationAccountId,
                paymentId,
                new BigDecimal(event.getAmount()).doubleValue(),
                event.getFromCurrency(),
                event.getToCurrency(),
                event.getPaymentType().name(),
                event.getTimestamp(),
                event.getRiskCheckPassed()
        );

        log.info("Transaction relationship merged for payment: {}", paymentId);
    }
}
