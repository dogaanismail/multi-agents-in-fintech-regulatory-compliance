package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.PaymentHistoryResponse;
import org.banksolution.service.PaymentHistoryQueryService;
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
@Tag(name = "Payment History")
@RequestMapping("/api/v1/payment-history")
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryController {

    private final PaymentHistoryQueryService paymentHistoryQueryService;

    @Operation(summary = "Get payment history by payment")
    @GetMapping("/{paymentId}")
    public ResponseEntity<@NonNull PaymentHistoryResponse> getPaymentHistory(@PathVariable UUID paymentId) {
        log.info("Fetching payment history for paymentId: {}", paymentId);
        return paymentHistoryQueryService.getPaymentHistoryById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get payment history by customer")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getCustomerPaymentHistory(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history for customer: {}, page: {}, size: {}",
                customerId,
                pageable.getPageNumber(),
                pageable.getPageSize());

        Page<@NonNull PaymentHistoryResponse> paymentHistoryResponses = paymentHistoryQueryService.getCustomerPaymentHistory(customerId, pageable);
        return ResponseEntity.ok(paymentHistoryResponses);
    }

    @Operation(summary = "Get customer payment history by dates")
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

        Page<@NonNull PaymentHistoryResponse> paymentHistoryResponses = paymentHistoryQueryService.getCustomerPaymentHistoryByDateRange(
                customerId,
                startDate,
                endDate,
                pageable);
        return ResponseEntity.ok(paymentHistoryResponses);
    }

    @Operation(summary = "Get payment history by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history for status: {}, page: {}",
                status,
                pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> paymentHistoryResponses = paymentHistoryQueryService.getPaymentHistoryByStatus(status, pageable);
        return ResponseEntity.ok(paymentHistoryResponses);
    }

    @Operation(summary = "Get payment history by fraud status")
    @GetMapping("/fraud-status/{fraudStatus}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByFraudStatus(
            @PathVariable String fraudStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history for fraud status: {}, page: {}",
                fraudStatus, pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> paymentHistoryResponses = paymentHistoryQueryService.getPaymentHistoryByFraudStatus(fraudStatus, pageable);
        return ResponseEntity.ok(paymentHistoryResponses);
    }

    @Operation(summary = "Get payment history by risk level")
    @GetMapping("/risk-level/{riskLevel}")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByRiskLevel(
            @PathVariable String riskLevel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history for risk level: {}, page: {}",
                riskLevel, pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> paymentHistoryResponses = paymentHistoryQueryService.getPaymentHistoryByRiskLevel(riskLevel, pageable);
        return ResponseEntity.ok(paymentHistoryResponses);
    }

    @Operation(summary = "Get payment history by date range")
    @GetMapping("/date-range")
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getPaymentHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching payment history between {} and {}, page: {}",
                startDate,
                endDate,
                pageable.getPageNumber());

        Page<@NonNull PaymentHistoryResponse> paymentHistoryResponses = paymentHistoryQueryService.getPaymentHistoryByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(paymentHistoryResponses);
    }

    @Operation(summary = "List all payment history")
    @GetMapping
    public ResponseEntity<@NonNull Page<@NonNull PaymentHistoryResponse>> getAllPaymentHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all payment history, page: {}, size: {}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        Page<@NonNull PaymentHistoryResponse> paymentHistoryResponses = paymentHistoryQueryService.getAllPaymentHistory(pageable);
        return ResponseEntity.ok(paymentHistoryResponses);
    }
}
