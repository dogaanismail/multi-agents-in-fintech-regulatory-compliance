package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import org.banksolution.integration.account.AccountServiceClient;
import org.banksolution.integration.account.dto.AccountResponse;
import org.banksolution.model.PaymentAccounts;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountServiceClient accountServiceClient;

    public Optional<PaymentAccounts> loadPaymentAccounts(UUID sourceAccountId, UUID destinationAccountId) {
        if (sourceAccountId == null || destinationAccountId == null) {
            return Optional.empty();
        }

        List<AccountResponse> accounts = accountServiceClient.getAccountsByIds(
                List.of(sourceAccountId, destinationAccountId));

        AccountResponse source = accounts.stream()
                .filter(acc -> acc.getId().equals(sourceAccountId))
                .findFirst()
                .orElse(null);

        AccountResponse destination = accounts.stream()
                .filter(acc -> acc.getId().equals(destinationAccountId))
                .findFirst()
                .orElse(null);

        if (source == null || destination == null) {
            return Optional.empty();
        }

        return Optional.of(new PaymentAccounts(source, destination));
    }

    public boolean isCrossOrderPayment(PaymentAccounts accounts) {
        String sourceLocation = accounts.source().getBankLocation();
        String destinationLocation = accounts.destination().getBankLocation();

        if (sourceLocation == null || destinationLocation == null) {
            return false;
        }

        return !sourceLocation.equalsIgnoreCase(destinationLocation);
    }

}

