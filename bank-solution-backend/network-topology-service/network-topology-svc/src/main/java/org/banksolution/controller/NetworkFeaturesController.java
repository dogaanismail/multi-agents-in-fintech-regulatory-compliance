package org.banksolution.controller;

import org.banksolution.servicesecurity.RequiresPermission;
import org.banksolution.servicesecurity.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.NetworkFeaturesDto;
import org.banksolution.model.CustomResponse;
import org.banksolution.service.NetworkFeatureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Network Features")
@RestController
@RequestMapping("/api/v1/networks")
@RequiredArgsConstructor
@Slf4j
public class NetworkFeaturesController {

    private final NetworkFeatureService networkFeatureService;

    @Operation(summary = "Get network features for an account")
    @GetMapping("/features/{accountId}")
    @RequiresPermission(Permissions.RISK_READ)
    public ResponseEntity<NetworkFeaturesDto> getNetworkFeatures(@PathVariable String accountId) {
        log.info("REST request to get network features for account: {}", accountId);
        NetworkFeaturesDto features = networkFeatureService.getNetworkFeatures(accountId);
        return ResponseEntity.ok(features);
    }
}