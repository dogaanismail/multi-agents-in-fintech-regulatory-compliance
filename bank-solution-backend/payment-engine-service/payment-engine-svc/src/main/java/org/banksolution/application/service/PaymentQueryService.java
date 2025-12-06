package org.banksolution.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.api.dto.PaymentStatusResponse;
import org.banksolution.api.mapper.PaymentStatusMapper;
import org.banksolution.exception.PaymentNotFoundException;
import org.banksolution.infrastructure.persistence.projection.PaymentStatusProjection;
import org.banksolution.infrastructure.persistence.repository.PaymentStatusProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryService {

    private final PaymentStatusProjectionRepository projectionRepository;

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentById(UUID paymentId) {
        log.info("Querying payment by ID: {}", paymentId);

        PaymentStatusProjection projection = projectionRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        return PaymentStatusMapper.toResponse(projection);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentByReferenceNumber(String referenceNumber) {
        log.info("Querying payment by reference number: {}", referenceNumber);

        PaymentStatusProjection projection = projectionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new RuntimeException("Payment not found with reference number: " + referenceNumber));

        return PaymentStatusMapper.toResponse(projection);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentByExternalId(UUID externalPaymentId) {
        log.info("Querying payment by external ID: {}", externalPaymentId);

        PaymentStatusProjection projection = projectionRepository.findByExternalPaymentId(externalPaymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with external ID: " + externalPaymentId));

        return PaymentStatusMapper.toResponse(projection);
    }

    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getPaymentsByCustomerId(UUID customerId) {
        log.info("Querying payments by customer ID: {}", customerId);

        List<PaymentStatusProjection> projections = projectionRepository.findByCustomerId(customerId);

        return projections.stream()
                .map(PaymentStatusMapper::toResponse)
                .toList();
    }
}
