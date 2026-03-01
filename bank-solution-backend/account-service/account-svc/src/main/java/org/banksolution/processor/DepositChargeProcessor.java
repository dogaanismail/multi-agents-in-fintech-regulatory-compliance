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
        Currency fromCurrency = Currency.valueOf(event.getFromCurrency());
        Currency toCurrency = Currency.valueOf(event.getToCurrency());
        BigDecimal amount = new BigDecimal(event.getAmount());
        BigDecimal creditAmount = new BigDecimal(event.getConvertedAmount());

        log.info("Processing deposit: destinationAccountId:{}, amount:{}, fromCurrency:{}, toCurrency:{}",
                destinationAccountId, amount, fromCurrency, toCurrency);

        requireAccount(destinationAccountId);

        AccountBalanceEntity ledger = findLedgerBalance(fromCurrency);
        AccountBalanceEntity destination = findBalance(destinationAccountId, toCurrency);

        debit(ledger, amount);
        credit(destination, creditAmount);
        saveAll(ledger, destination);

        log.info("Deposit completed: destinationAccountId:{} new balance:{}, ledger new balance:{}",
                destinationAccountId, destination.getAvailableBalance(), ledger.getAvailableBalance());
    }
}
