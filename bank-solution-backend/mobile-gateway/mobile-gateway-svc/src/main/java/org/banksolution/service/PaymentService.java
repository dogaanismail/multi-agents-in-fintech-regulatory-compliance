package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.integration.payment.PaymentServiceClient;
import org.banksolution.integration.payment.model.PaymentRequestResponse;
import org.banksolution.integration.paymenthistory.PaymentHistoryServiceClient;
import org.banksolution.mapper.PaymentMapper;
import org.banksolution.model.request.MobilePaymentRequest;
import org.banksolution.model.response.MobilePaymentResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerIdentityService customerIdentityService;
    private final AccountService accountService;
    private final PaymentServiceClient paymentServiceClient;
    private final PaymentHistoryServiceClient paymentHistoryServiceClient;

    public MobilePaymentResponse submitPayment(
            Jwt caller,
            String idempotencyKey,
            MobilePaymentRequest mobilePaymentRequest) {

        UUID customerId = customerIdentityService.getOnboardedCustomer(caller).id();
        accountService.requireAccountOwnedBy(customerId, mobilePaymentRequest.sourceAccountId());

        log.info("Customer {} submits a payment from account {}", customerId, mobilePaymentRequest.sourceAccountId());
        PaymentRequestResponse paymentRequestResponse = paymentServiceClient.requestPayment(
                idempotencyKey,
                PaymentMapper.toPaymentRequest(customerId, mobilePaymentRequest));
        return PaymentMapper.toSubmittedMobilePaymentResponse(paymentRequestResponse);
    }

    public List<MobilePaymentResponse> getPayments(
            Jwt caller,
            int page,
            int size) {

        UUID customerId = customerIdentityService.getOnboardedCustomer(caller).id();
        return paymentHistoryServiceClient
                .getCustomerPaymentHistory(customerId, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE))
                .content().stream()
                .map(PaymentMapper::toMobilePaymentResponse)
                .toList();
    }
}
