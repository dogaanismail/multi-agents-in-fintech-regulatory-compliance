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
        Currency currency = Currency.valueOf(event.getCurrency());
        BigDecimal amount = new BigDecimal(event.getAmount());

        log.info("Processing withdrawal: sourceAccountId:{}, amount:{}, currency:{}",
                sourceAccountId, amount, currency);

        requireAccount(sourceAccountId);

        AccountBalanceEntity source = findBalance(sourceAccountId, currency);
        AccountBalanceEntity ledger = findLedgerBalance(currency);

        debit(source, amount);
        credit(ledger, amount);
        saveAll(source, ledger);

        log.info("Withdrawal completed: sourceAccountId:{} new balance:{}, ledger new balance:{}",
                sourceAccountId, source.getAvailableBalance(), ledger.getAvailableBalance());
    }
}
