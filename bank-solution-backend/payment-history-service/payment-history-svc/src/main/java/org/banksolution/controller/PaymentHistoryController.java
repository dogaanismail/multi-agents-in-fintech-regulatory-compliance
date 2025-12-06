package org.banksolution.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.PaymentHistoryResponse;
import org.banksolution.mapper.PaymentHistoryResponseMapper;
import org.banksolution.repository.PaymentHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-history")
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryController {

    private final PaymentHistoryRepository repository;

    @GetMapping("/{paymentId}")
    public ResponseEntity<@NonNull PaymentHistoryResponse> getPaymentHistory(@PathVariable UUID paymentId) {

        log.info("Fetching payment history for paymentId: {}", paymentId);

        return repository.findById(paymentId)
                .map(PaymentHistoryResponseMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<@NonNull PaymentHistoryResponse> getPaymentHistoryByReference(
            @PathVariable String referenceNumber) {

        log.info("Fetching payment history for reference: {}", referenceNumber);

        return repository.findByReferenceNumber(referenceNumber)
                .map(PaymentHistoryResponseMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getCustomerPaymentHistory(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching payment history for customer: {}, page: {}, size: {}",
                customerId,
                pageable.getPageNumber(),
                pageable.getPageSize());

        Page<@NonNull PaymentHistoryResponse> history = repository.findByCustomerId(customerId, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);

        return ResponseEntity.ok(history);
    }

    @GetMapping("/customer/{customerId}/date-range")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getCustomerPaymentHistoryByDateRange(
            @PathVariable UUID customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching payment history for customer: {} between {} and {}, page: {}",
                customerId,
                startDate,
                endDate,
                pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> history = repository.findByCustomerIdAndDateRange(
                customerId,
                startDate,
                endDate,
                pageable).map(PaymentHistoryResponseMapper::toResponse);

        return ResponseEntity.ok(history);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history for status: {}, page: {}",
                status,
                pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> history = repository.findByStatus(status, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/fraud-status/{fraudStatus}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByFraudStatus(
            @PathVariable String fraudStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history for fraud status: {}, page: {}",
                fraudStatus, pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> history = repository.findByFraudStatus(fraudStatus, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/risk-level/{riskLevel}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByRiskLevel(
            @PathVariable String riskLevel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history for risk level: {}, page: {}",
                riskLevel, pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> history = repository.findByRiskLevel(riskLevel, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/date-range")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching payment history between {} and {}, page: {}",
                startDate,
                endDate,
                pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> history = repository.findByDateRange(startDate, endDate, pageable)
                .map(PaymentHistoryResponseMapper::toResponse);
        return ResponseEntity.ok(history);
    }

    @GetMapping
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getAllPaymentHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching all payment history, page: {}, size: {}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        Page<@NonNull PaymentHistoryResponse> history = repository.findAll(pageable)
                .map(PaymentHistoryResponseMapper::toResponse);

        return ResponseEntity.ok(history);
    }
}
