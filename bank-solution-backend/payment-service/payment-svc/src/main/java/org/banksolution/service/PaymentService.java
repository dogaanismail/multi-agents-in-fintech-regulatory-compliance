package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.producer.PaymentCreatedEventProducer;
import org.banksolution.repository.PaymentRequestRepository;
import org.banksolution.util.PaymentRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.banksolution.mapper.PaymentRequestMapper.toEntity;
import static org.banksolution.mapper.PaymentRequestMapper.toResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentCreatedEventProducer paymentCreatedEventProducer;
    private final AccountService accountService;

    @Transactional
    public PaymentRequestResponse requestPayment(PaymentRequest request) {
        log.info("Processing payment request for customer: {}, type: {}, amount: {} {}",
                request.getCustomerId(),
                request.getPaymentType(),
                request.getAmount(),
                request.getCurrency());

        PaymentRequestUtil.validatePaymentRequest(request);

        PaymentRequestEntity entity = toEntity(request);
        PaymentRequestEntity savedEntity = paymentRequestRepository.save(entity);

        boolean isCrossOrderPayment = accountService.isCrossOrderPayment(
                request.getSourceAccountId(),
                request.getDestinationAccountId());

        //TODO: Investigate and implement outbox pattern
        paymentCreatedEventProducer.publishPaymentCreatedEvent(savedEntity, isCrossOrderPayment);

        log.info("Payment request created: id:{}", savedEntity.getId());
        return toResponse(savedEntity, "Payment request submitted successfully and is being processed");
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getPaymentsByCustomerId(UUID customerId) {
        log.info("Fetching payments for customer: {}", customerId);

        List<PaymentRequestEntity> payments = paymentRequestRepository.findByCustomerId(customerId);

        return payments.stream()
                .map(entity -> toResponse(entity, null))
                .toList();
    }

}

