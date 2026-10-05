package org.banksolution.mapper;

import org.banksolution.integration.payment.model.PaymentRequest;
import org.banksolution.integration.payment.model.PaymentRequestResponse;
import org.banksolution.integration.paymenthistory.model.PaymentHistoryResponse;
import org.banksolution.model.request.MobilePaymentRequest;
import org.banksolution.model.response.MobilePaymentResponse;

import java.util.UUID;

import static org.banksolution.enums.CustomerPaymentStatus.PROCESSING;
import static org.banksolution.enums.CustomerPaymentStatus.toCustomerPaymentStatus;

public final class PaymentMapper {

    private static final String OUTGOING_TRANSFER = "TRANSFER_OUT";

    private PaymentMapper() {
    }

    public static PaymentRequest toPaymentRequest(
            UUID customerId,
            MobilePaymentRequest mobilePaymentRequest) {

        return new PaymentRequest(
                customerId,
                mobilePaymentRequest.sourceAccountId(),
                mobilePaymentRequest.destinationAccountId(),
                mobilePaymentRequest.amount(),
                mobilePaymentRequest.currency(),
                mobilePaymentRequest.currency(),
                OUTGOING_TRANSFER,
                mobilePaymentRequest.description());
    }

    public static MobilePaymentResponse toSubmittedMobilePaymentResponse(PaymentRequestResponse paymentRequestResponse) {
        return new MobilePaymentResponse(
                paymentRequestResponse.id(),
                paymentRequestResponse.sourceAccountId(),
                paymentRequestResponse.destinationAccountId(),
                paymentRequestResponse.amount(),
                paymentRequestResponse.fromCurrency(),
                paymentRequestResponse.description(),
                PROCESSING,
                paymentRequestResponse.createdAt());
    }

    public static MobilePaymentResponse toMobilePaymentResponse(PaymentHistoryResponse paymentHistoryResponse) {
        return new MobilePaymentResponse(
                paymentHistoryResponse.paymentId(),
                paymentHistoryResponse.sourceAccountId(),
                paymentHistoryResponse.destinationAccountId(),
                paymentHistoryResponse.amount(),
                paymentHistoryResponse.fromCurrency(),
                paymentHistoryResponse.description(),
                toCustomerPaymentStatus(paymentHistoryResponse.status()),
                paymentHistoryResponse.initiatedAt());
    }
}
