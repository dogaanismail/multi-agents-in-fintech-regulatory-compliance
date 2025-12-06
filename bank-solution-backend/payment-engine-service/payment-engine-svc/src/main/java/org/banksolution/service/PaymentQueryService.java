package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentStatusProjection;
import org.banksolution.exception.PaymentNotFoundException;
import org.banksolution.mapper.PaymentStatusMapper;
import org.banksolution.model.response.PaymentStatusResponse;
import org.banksolution.repository.PaymentStatusProjectionRepository;
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
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.toString()));

        return PaymentStatusMapper.toResponse(projection);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentByReferenceNumber(String referenceNumber) {
        log.info("Querying payment by reference number: {}", referenceNumber);

        PaymentStatusProjection projection = projectionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new PaymentNotFoundException(referenceNumber));

        return PaymentStatusMapper.toResponse(projection);
    }

    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getPaymentsByCustomerId(UUID customerId) {
        log.info("Querying payments for customer: {}", customerId);

        List<PaymentStatusProjection> projections = projectionRepository.findByCustomerId(customerId);

        return projections.stream()
                .map(PaymentStatusMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentByExternalId(UUID externalPaymentId) {
        log.info("Querying payment by external ID: {}", externalPaymentId);

        PaymentStatusProjection projection = projectionRepository.findByExternalPaymentId(externalPaymentId)
                .orElseThrow(() -> new PaymentNotFoundException(externalPaymentId.toString()));

        return PaymentStatusMapper.toResponse(projection);
    }
}
