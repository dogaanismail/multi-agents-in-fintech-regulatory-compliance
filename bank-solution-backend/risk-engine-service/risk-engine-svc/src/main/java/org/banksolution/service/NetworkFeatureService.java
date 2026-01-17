package org.banksolution.service;

import com.aml.fraud.NetworkFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.integration.networktopology.NetworkTopologyServiceClient;
import org.banksolution.integration.networktopology.dto.NetworkFeatureResponse;
import org.banksolution.mapper.NetworkFeaturesMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkFeatureService {

    private final NetworkTopologyServiceClient networkTopologyServiceClient;

    public NetworkFeatures getNetworkFeatures(String accountId) {
        log.debug("Fetching network features for accountId: {}", accountId);

        try {
            NetworkFeatureResponse response = networkTopologyServiceClient.getNetworkFeatures(accountId);
            return NetworkFeaturesMapper.toAvroNetworkFeatures(response);
        } catch (Exception e) {
            log.error("Failed to fetch network features for accountId: {}", accountId, e);
            return NetworkFeaturesMapper.getDefaultNetworkFeatures(accountId);
        }
    }
}
