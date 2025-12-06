package org.banksolution.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentHistory;
import org.banksolution.repository.PaymentHistoryRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API for backoffice payment history queries.
 * Provides comprehensive payment audit trail with all risk assessments and agent observations.
 */
@RestController
@RequestMapping("/api/v1/payment-history")
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryController {

    private final PaymentHistoryRepository repository;

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentHistory> getPaymentHistory(@PathVariable UUID paymentId) {
        log.info("Fetching payment history for paymentId: {}", paymentId);
        
        return repository.findById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<PaymentHistory> getPaymentHistoryByReference(@PathVariable String referenceNumber) {
        log.info("Fetching payment history for reference: {}", referenceNumber);
        
        return repository.findByReferenceNumber(referenceNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PaymentHistory>> getCustomerPaymentHistory(@PathVariable UUID customerId) {
        log.info("Fetching payment history for customer: {}", customerId);
        
        List<PaymentHistory> history = repository.findByCustomerId(customerId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/customer/{customerId}/date-range")
    public ResponseEntity<List<PaymentHistory>> getCustomerPaymentHistoryByDateRange(
            @PathVariable UUID customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        log.info("Fetching payment history for customer: {} between {} and {}", customerId, startDate, endDate);
        
        List<PaymentHistory> history = repository.findByCustomerIdAndDateRange(customerId, startDate, endDate);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentHistory>> getPaymentHistoryByStatus(@PathVariable String status) {
        log.info("Fetching payment history for status: {}", status);
        
        List<PaymentHistory> history = repository.findByStatus(status);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/fraud-status/{fraudStatus}")
    public ResponseEntity<List<PaymentHistory>> getPaymentHistoryByFraudStatus(@PathVariable String fraudStatus) {
        log.info("Fetching payment history for fraud status: {}", fraudStatus);
        
        List<PaymentHistory> history = repository.findByFraudStatus(fraudStatus);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/risk-level/{riskLevel}")
    public ResponseEntity<List<PaymentHistory>> getPaymentHistoryByRiskLevel(@PathVariable String riskLevel) {
        log.info("Fetching payment history for risk level: {}", riskLevel);
        
        List<PaymentHistory> history = repository.findByRiskLevel(riskLevel);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<PaymentHistory>> getPaymentHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        log.info("Fetching payment history between {} and {}", startDate, endDate);
        
        List<PaymentHistory> history = repository.findByDateRange(startDate, endDate);
        return ResponseEntity.ok(history);
    }
}
