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

        List<AccountResponse> accountResponses = accountServiceClient.getAccountsByIds(
                List.of(sourceAccountId, destinationAccountId));

        AccountResponse sourceAccountResponse = accountResponses.stream()
                .filter(accountResponse -> accountResponse.getId().equals(sourceAccountId))
                .findFirst()
                .orElse(null);

        AccountResponse destinationAccountResponse = accountResponses.stream()
                .filter(accountResponse -> accountResponse.getId().equals(destinationAccountId))
                .findFirst()
                .orElse(null);

        if (sourceAccountResponse == null || destinationAccountResponse == null) {
            return Optional.empty();
        }

        return Optional.of(new PaymentAccounts(sourceAccountResponse, destinationAccountResponse));
    }

    public boolean isCrossBorderPayment(PaymentAccounts paymentAccounts) {
        String sourceBankLocation = paymentAccounts.source().getBankLocation();
        String destinationBankLocation = paymentAccounts.destination().getBankLocation();

        if (sourceBankLocation == null || destinationBankLocation == null) {
            return false;
        }

        return !sourceBankLocation.equalsIgnoreCase(destinationBankLocation);
    }

}

