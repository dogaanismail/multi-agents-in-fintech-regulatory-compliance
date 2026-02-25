package org.banksolution.processor;

import com.aml.account.AccountChargeRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.AccountBalanceEntity;
import org.banksolution.enums.Currency;
import org.banksolution.repository.AccountBalanceRepository;
import org.banksolution.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class DepositChargeProcessor extends AbstractAccountChargeProcessor {

    public DepositChargeProcessor(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository) {
        super(accountRepository, accountBalanceRepository);
    }

    @Override
    public void process(AccountChargeRequestedEvent event) {
        UUID destinationAccountId = UUID.fromString(event.getDestinationAccountId());
        Currency currency = Currency.valueOf(event.getCurrency());
        BigDecimal amount = new BigDecimal(event.getAmount());

        log.info("Processing deposit: destinationAccountId:{}, amount:{}, currency:{}",
                destinationAccountId, amount, currency);

        requireAccount(destinationAccountId);

        AccountBalanceEntity ledger = findLedgerBalance(currency);
        AccountBalanceEntity destination = findBalance(destinationAccountId, currency);

        debit(ledger, amount);
        credit(destination, amount);
        saveAll(ledger, destination);

        log.info("Deposit completed: destinationAccountId:{} new balance:{}, ledger new balance:{}",
                destinationAccountId, destination.getAvailableBalance(), ledger.getAvailableBalance());
    }
}
