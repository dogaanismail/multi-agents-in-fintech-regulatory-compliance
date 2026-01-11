package org.banksolution.service;

import com.aml.fraud.TransactionFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.PaymentType;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.integration.account.dto.AccountResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static org.banksolution.mapper.TransactionFeaturesMapper.toTransactionFeatures;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionFeatureService {

    private final AccountService accountService;

    public TransactionFeatures getTransactionFeatures(RiskCheckRequestEntity riskCheckRequestEntity) {
        log.debug("Building transaction features for paymentId: {}", riskCheckRequestEntity.getPaymentId());

        PaymentType paymentType = riskCheckRequestEntity.getPaymentType();

        return switch (paymentType) {
            case TRANSFER_IN, TRANSFER_OUT -> buildTransferFeatures(riskCheckRequestEntity);
            case DEPOSIT -> buildDepositFeatures(riskCheckRequestEntity);
            case WITHDRAWAL -> buildWithdrawalFeatures(riskCheckRequestEntity);
        };
    }

    private TransactionFeatures buildTransferFeatures(RiskCheckRequestEntity riskCheckRequestEntity) {
        UUID sourceAccountId = UUID.fromString(riskCheckRequestEntity.getSourceAccountId());
        UUID destinationAccountId = UUID.fromString(riskCheckRequestEntity.getDestinationAccountId());

        List<UUID> accountIds = List.of(sourceAccountId, destinationAccountId);
        List<AccountResponse> accounts = accountService.getAccountsByIds(accountIds);

        AccountResponse senderAccount = findByAccountId(accounts, riskCheckRequestEntity.getSourceAccountId());
        AccountResponse receiverAccount = findByAccountId(accounts, riskCheckRequestEntity.getDestinationAccountId());

        return toTransactionFeatures(riskCheckRequestEntity, senderAccount, receiverAccount);
    }

    private TransactionFeatures buildDepositFeatures(RiskCheckRequestEntity riskCheckRequestEntity) {
        UUID destinationAccountId = UUID.fromString(riskCheckRequestEntity.getDestinationAccountId());
        AccountResponse receiverAccount = accountService.getAccountById(destinationAccountId);

        // TODO: Implement bank ledger account logic for cash deposits
        return toTransactionFeatures(riskCheckRequestEntity, null, receiverAccount);
    }

    private TransactionFeatures buildWithdrawalFeatures(RiskCheckRequestEntity riskCheckRequestEntity) {
        UUID sourceAccountId = UUID.fromString(riskCheckRequestEntity.getSourceAccountId());
        AccountResponse senderAccount = accountService.getAccountById(sourceAccountId);

        // TODO: Implement bank ledger account logic for cash withdrawals
        return toTransactionFeatures(riskCheckRequestEntity, senderAccount, null);
    }

    private AccountResponse findByAccountId(List<AccountResponse> accounts, String accountId) {
        UUID id = UUID.fromString(accountId);
        return accounts.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

}
