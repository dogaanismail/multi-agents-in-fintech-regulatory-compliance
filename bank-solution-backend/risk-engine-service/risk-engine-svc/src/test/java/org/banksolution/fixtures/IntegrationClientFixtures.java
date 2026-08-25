package org.banksolution.fixtures;

import org.banksolution.enums.AccountStatus;
import org.banksolution.enums.AccountType;
import org.banksolution.integration.account.dto.AccountResponse;
import org.banksolution.integration.customerprofile.dto.CustomerFeaturesResponse;
import org.banksolution.integration.networktopology.dto.NetworkFeatureResponse;

import java.time.LocalDate;
import java.util.UUID;

public final class IntegrationClientFixtures {

    private IntegrationClientFixtures() {
    }

    public static AccountResponse createAccountResponse(UUID accountId, String accountNumber, String bankLocation) {
        return AccountResponse.builder()
                .id(accountId)
                .customerId(UUID.randomUUID())
                .accountNumber(accountNumber)
                .accountType(AccountType.CHECKING)
                .bankLocation(bankLocation)
                .accountStatus(AccountStatus.ACTIVE)
                .openingDate(LocalDate.of(2024, 3, 15))
                .build();
    }

    public static CustomerFeaturesResponse createCustomerFeaturesResponse(String customerId, String accountId) {
        return CustomerFeaturesResponse.builder()
                .customerId(customerId)
                .accountId(accountId)
                .transactionCount(42)
                .totalAmount(12500.75)
                .avgAmount(297.64)
                .medianAmount(150.00)
                .maxAmount(5000.00)
                .minAmount(5.00)
                .stdAmount(410.32)
                .activeDays(30)
                .transactionsPerDay(1.4)
                .crossBorderRatio(0.25)
                .cashTransactionRatio(0.10)
                .largeTransactionRatio(0.05)
                .nightTransactionRatio(0.08)
                .weekendTransactionRatio(0.30)
                .uniqueReceivers(12)
                .uniqueReceiverCountries(3)
                .receiverDiversity(0.29)
                .uniqueCurrencies(2)
                .amountConsistency(0.61)
                .build();
    }

    public static NetworkFeatureResponse createNetworkFeatureResponse(String accountId) {
        return NetworkFeatureResponse.builder()
                .accountId(accountId)
                .inDegree(5)
                .outDegree(8)
                .degreeCentrality(0.013)
                .inDegreeCentrality(0.005)
                .outDegreeCentrality(0.008)
                .betweennessCentrality(0.0021)
                .closenessCentrality(0.31)
                .pagerank(0.0009)
                .eigenvectorCentrality(0.044)
                .clusteringCoefficient(0.12)
                .community(7)
                .build();
    }
}
