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
public class WithdrawalChargeProcessor extends AbstractAccountChargeProcessor {

    public WithdrawalChargeProcessor(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository) {
        super(accountRepository, accountBalanceRepository);
    }

    @Override
    public void process(AccountChargeRequestedEvent event) {
        UUID sourceAccountId = UUID.fromString(event.getSourceAccountId());
        Currency fromCurrency = Currency.valueOf(event.getFromCurrency());
        Currency toCurrency = Currency.valueOf(event.getToCurrency());
        BigDecimal amount = new BigDecimal(event.getAmount());
        BigDecimal creditAmount = new BigDecimal(event.getConvertedAmount());

        log.info("Processing withdrawal: sourceAccountId:{}, amount:{}, fromCurrency:{}, toCurrency:{}",
                sourceAccountId, amount, fromCurrency, toCurrency);

        requireAccount(sourceAccountId);

        AccountBalanceEntity source = findBalance(sourceAccountId, fromCurrency);
        AccountBalanceEntity ledger = findLedgerBalance(toCurrency);

        debit(source, amount);
        credit(ledger, creditAmount);
        saveAll(source, ledger);

        log.info("Withdrawal completed: sourceAccountId:{} new balance:{}, ledger new balance:{}",
                sourceAccountId, source.getAvailableBalance(), ledger.getAvailableBalance());
    }
}
