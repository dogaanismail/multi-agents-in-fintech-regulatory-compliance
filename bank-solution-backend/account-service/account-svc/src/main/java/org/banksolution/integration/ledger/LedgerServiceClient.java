package org.banksolution.integration.ledger;

import feign.Headers;
import org.banksolution.integration.ledger.dto.CreateLedgerAccountsRequest;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "ledger-svc",
        url = "${integration.ledger-service.url}"
)
@Headers({"Content-Type: application/json"})
public interface LedgerServiceClient {

    @PostMapping("/batch")
    List<LedgerAccountResponse> createLedgerAccounts(@RequestBody CreateLedgerAccountsRequest request);
}
