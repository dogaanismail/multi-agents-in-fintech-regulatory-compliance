package org.banksolution.projection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.banksolution.entity.PaymentStatusProjection;
import org.banksolution.enums.FraudCheckStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.event.*;
import org.banksolution.repository.PaymentIdMappingRepository;
import org.banksolution.repository.PaymentStatusProjectionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProjectionHandler {

    private final PaymentStatusProjectionRepository projectionRepository;
    private final PaymentIdMappingRepository mappingRepository;

    @EventHandler
    public void on(PaymentInitiatedEvent event) {
        log.info("Projecting PaymentInitiatedEvent: {}", event.getPaymentId());

        PaymentStatusProjection projection = PaymentStatusProjection.builder()
                .paymentId(UUID.fromString(event.getPaymentId().toString()))
                .externalPaymentId(event.getExternalPaymentId())
                .referenceNumber(event.getReferenceNumber())
                .customerId(event.getCustomerId())
                .sourceAccountId(event.getSourceAccountId())
                .destinationAccountId(event.getDestinationAccountId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .paymentType(event.getPaymentType())
                .description(event.getDescription())
                .status(PaymentStatus.INITIATED)
                .fraudStatus(FraudCheckStatus.PENDING)
                .initiatedAt(LocalDateTime.now())
                .build();

        projectionRepository.save(projection);
        mappingRepository.save(event.getReferenceNumber(), event.getPaymentId());

        log.info("Payment projection created: {}", event.getPaymentId());
    }

    @EventHandler
    public void on(RiskCheckRequestedEvent event) {
        log.info("Updating projection for RiskCheckRequestedEvent: {}", event.getPaymentId());

        projectionRepository.findById(UUID.fromString(event.getPaymentId().toString()))
                .ifPresent(projection -> {
                    projection.setStatus(PaymentStatus.FRAUD_CHECK_PENDING);
                    projection.setFraudStatus(FraudCheckStatus.PENDING);
                    projectionRepository.save(projection);
                });
    }

    @EventHandler
    public void on(FraudCheckApprovedEvent event) {
        log.info("Updating projection for FraudCheckApprovedEvent: {}", event.getPaymentId());

        projectionRepository.findById(UUID.fromString(event.getPaymentId().toString()))
                .ifPresent(projection -> {
                    projection.setStatus(PaymentStatus.FRAUD_CHECK_APPROVED);
                    projection.setFraudStatus(FraudCheckStatus.APPROVED);
                    projection.setFraudConfidence(event.getConfidence());
                    projection.setMaddpgQValue(event.getMaddpgQValue());
                    projection.setFraudCheckedAt(LocalDateTime.now());
                    projectionRepository.save(projection);
                });
    }

    @EventHandler
    public void on(PaymentBlockedEvent event) {
        log.info("Updating projection for PaymentBlockedEvent: {}", event.getPaymentId());

        projectionRepository.findById(UUID.fromString(event.getPaymentId().toString()))
                .ifPresent(projection -> {
                    projection.setStatus(PaymentStatus.BLOCKED);
                    projection.setFraudStatus(FraudCheckStatus.BLOCKED);
                    projection.setFraudConfidence(event.getConfidence());
                    projection.setMaddpgQValue(event.getMaddpgQValue());
                    projection.setBlockedAt(LocalDateTime.now());
                    projectionRepository.save(projection);
                });
    }

    @EventHandler
    public void on(ManualReviewRequestedEvent event) {
        log.info("Updating projection for ManualReviewRequestedEvent: {}", event.getPaymentId());

        projectionRepository.findById(UUID.fromString(event.getPaymentId().toString()))
                .ifPresent(projection -> {
                    projection.setStatus(PaymentStatus.MANUAL_REVIEW_REQUIRED);
                    projection.setFraudStatus(FraudCheckStatus.REVIEW_REQUIRED);
                    projection.setFraudConfidence(event.getConfidence());
                    projection.setMaddpgQValue(event.getMaddpgQValue());
                    projectionRepository.save(projection);
                });
    }

    @EventHandler
    public void on(PaymentCompletedEvent event) {
        log.info("Updating projection for PaymentCompletedEvent: {}", event.getPaymentId());

        projectionRepository.findById(UUID.fromString(event.getPaymentId().toString()))
                .ifPresent(projection -> {
                    projection.setStatus(PaymentStatus.COMPLETED);
                    projection.setCompletedAt(LocalDateTime.now());
                    projectionRepository.save(projection);
                });
    }
}
