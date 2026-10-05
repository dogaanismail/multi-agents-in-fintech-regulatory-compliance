package org.banksolution.integration.paymenthistory;

import org.banksolution.integration.paymenthistory.model.PaymentHistoryPage;
import org.banksolution.servicesecurity.feign.ServiceTokenFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        configuration = ServiceTokenFeignConfiguration.class,
        name = "payment-history-svc",
        url = "${integration.payment-history-service.url}")
public interface PaymentHistoryServiceClient {

    @GetMapping("/customer/{customerId}")
    PaymentHistoryPage getCustomerPaymentHistory(
            @PathVariable UUID customerId,
            @RequestParam("page") int page,
            @RequestParam("size") int size);
}
