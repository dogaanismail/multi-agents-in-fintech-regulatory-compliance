package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.NetworkFeaturesDto;
import org.banksolution.repository.AccountNodeRepository;
import org.banksolution.repository.NetworkGraphRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkFeatureService {

    private final AccountNodeRepository accountNodeRepository;
    private final NetworkGraphRepository networkGraphRepository;

    public NetworkFeaturesDto getNetworkFeatures(String accountId) {
        log.info("Fetching network features for accountId: {}", accountId);
        return networkGraphRepository.getNetworkFeatures(accountId);
    }

    public void getOrCreateAccount(String accountId, String customerId) {
        accountNodeRepository.mergeAccount(accountId, customerId);
        log.debug("Account node merged: {}", accountId);
    }
}