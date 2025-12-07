package org.banksolution.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.api.dto.InitiatePaymentRequest;
import org.banksolution.api.dto.InitiatePaymentResponse;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for payment commands (CQRS write side).
 * <p>
 * This service only handles commands - no queries.
 * For querying payment status, use the payment-history service.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandController {

    private final CommandGateway commandGateway;

    /**
     * Initiate a new payment.
     * This creates a new aggregate and starts the payment workflow.
     */
    @PostMapping
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(@RequestBody InitiatePaymentRequest request) {

        log.info("Received payment initiation request for customer: {}", request.getCustomerId());

        PaymentId paymentId = new PaymentId();

        InitiatePaymentCommand command = new InitiatePaymentCommand(
                paymentId,
                request.getExternalPaymentId(),
                request.getReferenceNumber(),
                request.getCustomerId(),
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentType(),
                request.getDescription()
        );

        commandGateway.sendAndWait(command);

        log.info("Payment initiated successfully: {}", paymentId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new InitiatePaymentResponse(
                        paymentId.toString(),
                        request.getReferenceNumber(),
                        "Payment initiated successfully"
                ));
    }

}
