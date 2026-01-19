package org.banksolution.integration.account;

import feign.Headers;
import org.banksolution.integration.account.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "account-svc",
        url = "${integration.account-service.url}"
)
@Headers({"Content-Type: application/json"})
public interface AccountServiceClient {

    @GetMapping("/{id}")
    AccountResponse getAccountById(@PathVariable UUID id);

    @GetMapping("/ids")
    List<AccountResponse> getAccountsByIds(@RequestParam("ids") List<UUID> ids);

}
