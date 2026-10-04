package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.PaymentIdempotency;
import org.banksolution.domain.PaymentRequestResult;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.exception.IdempotencyKeyReusedException;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.repository.PaymentRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.banksolution.mapper.PaymentRequestMapper.toPaymentRequestResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotentPaymentService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentService paymentService;

    public PaymentRequestResult requestPayment(
            String idempotencyKey,
            PaymentRequest paymentRequest) {

        PaymentIdempotency paymentIdempotency = PaymentIdempotency.derivePaymentIdempotency(idempotencyKey, paymentRequest);

        Optional<PaymentRequestEntity> existingPaymentRequestEntity =
                paymentRequestRepository.findById(paymentIdempotency.paymentId());
        if (existingPaymentRequestEntity.isPresent()) {
            return replayPaymentRequest(existingPaymentRequestEntity.get(), paymentIdempotency);
        }

        try {
            return new PaymentRequestResult(paymentService.createPayment(paymentIdempotency, paymentRequest), false);
        } catch (DataIntegrityViolationException concurrentDuplicateException) {
            PaymentRequestEntity concurrentlyCreatedPaymentRequestEntity = paymentRequestRepository
                    .findById(paymentIdempotency.paymentId())
                    .orElseThrow(() -> concurrentDuplicateException);
            return replayPaymentRequest(concurrentlyCreatedPaymentRequestEntity, paymentIdempotency);
        }
    }

    private static PaymentRequestResult replayPaymentRequest(
            PaymentRequestEntity paymentRequestEntity,
            PaymentIdempotency paymentIdempotency) {

        if (!paymentIdempotency.requestFingerprint().equals(paymentRequestEntity.getRequestFingerprint())) {
            throw new IdempotencyKeyReusedException(paymentRequestEntity.getId());
        }

        log.info("Replaying payment request {} for Idempotency-Key {}",
                paymentRequestEntity.getId(),
                paymentIdempotency.idempotencyKey());

        return new PaymentRequestResult(
                toPaymentRequestResponse(paymentRequestEntity, PaymentService.PAYMENT_SUBMITTED_MESSAGE),
                true);
    }
}
