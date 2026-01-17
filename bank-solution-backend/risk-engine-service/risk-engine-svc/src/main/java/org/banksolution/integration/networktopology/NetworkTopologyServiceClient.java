package org.banksolution.integration.networktopology;

import feign.Headers;
import org.banksolution.integration.networktopology.dto.NetworkFeatureResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "network-topology-svc",
        url = "${integration.network-topology-service.url}"
)
@Headers({"Content-Type: application/json"})
public interface NetworkTopologyServiceClient {

    @GetMapping("/features/{accountId}")
    NetworkFeatureResponse getNetworkFeatures(@PathVariable String accountId);
}
