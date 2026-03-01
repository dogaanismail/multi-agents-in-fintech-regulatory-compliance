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
public class TransferChargeProcessor extends AbstractAccountChargeProcessor {

    public TransferChargeProcessor(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository) {
        super(accountRepository, accountBalanceRepository);
    }

    @Override
    public void process(AccountChargeRequestedEvent event) {
        UUID sourceAccountId = UUID.fromString(event.getSourceAccountId());
        UUID destinationAccountId = UUID.fromString(event.getDestinationAccountId());
        Currency fromCurrency = Currency.valueOf(event.getFromCurrency());
        Currency toCurrency = Currency.valueOf(event.getToCurrency());
        BigDecimal amount = new BigDecimal(event.getAmount());
        BigDecimal creditAmount = new BigDecimal(event.getConvertedAmount());

        log.info("Processing transfer: sourceAccountId:{}, destinationAccountId:{}, amount:{}, fromCurrency:{}, toCurrency:{}",
                sourceAccountId, destinationAccountId, amount, fromCurrency, toCurrency);

        requireAccount(sourceAccountId);
        requireAccount(destinationAccountId);

        AccountBalanceEntity source = findBalance(sourceAccountId, fromCurrency);
        AccountBalanceEntity destination = findBalance(destinationAccountId, toCurrency);

        debit(source, amount);
        credit(destination, creditAmount);
        saveAll(source, destination);

        log.info("Transfer completed: sourceAccountId:{} new balance:{}, destinationAccountId:{} new balance:{}",
                sourceAccountId, source.getAvailableBalance(), destinationAccountId, destination.getAvailableBalance());
    }
}
