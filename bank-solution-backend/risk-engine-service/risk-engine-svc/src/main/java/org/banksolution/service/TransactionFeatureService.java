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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

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
        // A transfer to or from another bank has no internal account on that side;
        // the mapper substitutes an external account number for a null account.
        List<UUID> internalAccountIds = Stream.of(
                        riskCheckRequestEntity.getSourceAccountId(),
                        riskCheckRequestEntity.getDestinationAccountId())
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .toList();

        List<AccountResponse> accounts = accountService.getAccountsByIds(internalAccountIds);

        AccountResponse senderAccount =
                findByAccountIdOrNull(accounts, riskCheckRequestEntity.getSourceAccountId());
        AccountResponse receiverAccount =
                findByAccountIdOrNull(accounts, riskCheckRequestEntity.getDestinationAccountId());

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

    private AccountResponse findByAccountIdOrNull(List<AccountResponse> accounts, String accountId) {
        return accountId == null ? null : findByAccountId(accounts, accountId);
    }

    private AccountResponse findByAccountId(List<AccountResponse> accounts, String accountId) {
        UUID id = UUID.fromString(accountId);
        return accounts.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

}
