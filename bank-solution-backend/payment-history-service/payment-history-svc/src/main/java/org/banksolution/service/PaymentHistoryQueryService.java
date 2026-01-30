package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.PaymentHistoryResponse;
import org.banksolution.mapper.PaymentHistoryResponseMapper;
import org.banksolution.repository.PaymentHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentHistoryQueryService {

    private final PaymentHistoryRepository repository;

    public Optional<PaymentHistoryResponse> getPaymentHistoryById(UUID paymentId) {
        log.debug("Querying payment history for paymentId: {}", paymentId);
        
        return repository.findById(paymentId)
                .map(PaymentHistoryResponseMapper::toResponse);
    }

    public Page<PaymentHistoryResponse> getCustomerPaymentHistory(UUID customerId, Pageable pageable) {
        log.debug("Querying payment history for customer: {}, page: {}", 
                customerId, pageable.getPageNumber());
        
        return repository.findByCustomerId(customerId, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
    }

    public Page<PaymentHistoryResponse> getCustomerPaymentHistoryByDateRange(
            UUID customerId,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {
        
        log.debug("Querying payment history for customer: {} between {} and {}", 
                customerId, startDate, endDate);
        
        return repository.findByCustomerIdAndDateRange(customerId, startDate, endDate, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
    }

    public Page<PaymentHistoryResponse> getPaymentHistoryByStatus(String status, Pageable pageable) {
        log.debug("Querying payment history for status: {}", status);
        
        return repository.findByStatus(status, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
    }

    public Page<PaymentHistoryResponse> getPaymentHistoryByFraudStatus(String fraudStatus, Pageable pageable) {
        log.debug("Querying payment history for fraud status: {}", fraudStatus);
        
        return repository.findByFraudStatus(fraudStatus, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
    }

    public Page<PaymentHistoryResponse> getPaymentHistoryByRiskLevel(String riskLevel, Pageable pageable) {
        log.debug("Querying payment history for risk level: {}", riskLevel);
        
        return repository.findByRiskLevel(riskLevel, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
    }

    public Page<PaymentHistoryResponse> getPaymentHistoryByDateRange(
            Instant startDate,
            Instant endDate,
            Pageable pageable) {
        
        log.debug("Querying payment history between {} and {}", startDate, endDate);
        
        return repository.findByDateRange(startDate, endDate, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
    }

    public Page<PaymentHistoryResponse> getAllPaymentHistory(Pageable pageable) {
        log.debug("Querying all payment history, page: {}", pageable.getPageNumber());
        
        return repository.findAll(pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
    }
}
