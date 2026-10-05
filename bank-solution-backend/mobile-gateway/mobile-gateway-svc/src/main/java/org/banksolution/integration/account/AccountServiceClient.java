package org.banksolution.integration.account;

import org.banksolution.integration.account.model.AccountResponse;
import org.banksolution.integration.account.model.OpenAccountRequest;
import org.banksolution.servicesecurity.feign.ServiceTokenFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(
        configuration = ServiceTokenFeignConfiguration.class,
        name = "account-svc",
        url = "${integration.account-service.url}")
public interface AccountServiceClient {

    @GetMapping("/customer/{customerId}")
    List<AccountResponse> getAccountsByCustomerId(@PathVariable UUID customerId);

    @GetMapping("/{accountId}")
    AccountResponse getAccountById(@PathVariable UUID accountId);

    @PostMapping("/open-account")
    AccountResponse openAccount(@RequestBody OpenAccountRequest openAccountRequest);
}
