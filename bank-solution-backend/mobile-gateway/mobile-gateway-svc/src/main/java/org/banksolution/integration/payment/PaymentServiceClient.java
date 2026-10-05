package org.banksolution.integration.payment;

import org.banksolution.http.IdempotencyHeaders;
import org.banksolution.integration.payment.model.PaymentRequest;
import org.banksolution.integration.payment.model.PaymentRequestResponse;
import org.banksolution.servicesecurity.feign.ServiceTokenFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        configuration = ServiceTokenFeignConfiguration.class,
        name = "payment-svc",
        url = "${integration.payment-service.url}")
public interface PaymentServiceClient {

    @PostMapping("/request")
    PaymentRequestResponse requestPayment(
            @RequestHeader(IdempotencyHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestBody PaymentRequest paymentRequest);
}
