package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.AccountNeighbourhood;
import org.banksolution.dto.NetworkFeaturesDto;
import org.banksolution.repository.NetworkGraphRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkFeatureService {

    private final NetworkGraphRepository networkGraphRepository;

    public NetworkFeaturesDto getNetworkFeatures(String accountId) {
        log.info("Fetching network features for accountId: {}", accountId);
        AccountNeighbourhood neighbourhood = networkGraphRepository.getAccountNeighbourhood(accountId);
        return NetworkFeatureCalculator.toNetworkFeatures(neighbourhood);
    }
}
