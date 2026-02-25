package org.banksolution.processor;

import com.aml.account.AccountChargeRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.banksolution.entity.AccountBalanceEntity;
import org.banksolution.enums.AccountType;
import org.banksolution.enums.Currency;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.exception.BalanceNotFoundException;
import org.banksolution.exception.InsufficientFundsException;
import org.banksolution.repository.AccountBalanceRepository;
import org.banksolution.repository.AccountRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class AbstractAccountChargeProcessor implements AccountChargeProcessor {

    protected final AccountRepository accountRepository;
    protected final AccountBalanceRepository accountBalanceRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, noRollbackFor = InsufficientFundsException.class)
    public abstract void process(AccountChargeRequestedEvent event);

    protected void requireAccount(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
    }

    protected AccountBalanceEntity findBalance(UUID accountId, Currency currency) {
        return accountBalanceRepository
                .findByAccountIdAndCurrency(accountId, currency)
                .orElseThrow(() -> new BalanceNotFoundException(accountId, currency));
    }

    protected AccountBalanceEntity findLedgerBalance(Currency currency) {
        return accountRepository.findByAccountType(AccountType.LEDGER)
                .map(ledger -> accountBalanceRepository
                        .findByAccountIdAndCurrency(ledger.getId(), currency)
                        .orElseThrow(() -> new BalanceNotFoundException(ledger.getId(), currency)))
                .orElseThrow(() -> new IllegalStateException("Ledger account not found"));
    }

    protected void debit(AccountBalanceEntity balance, BigDecimal amount) {
        BigDecimal newBalance = balance.getAvailableBalance().subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(balance.getAccount().getId(), balance.getCurrency());
        }
        balance.setAvailableBalance(newBalance);
    }

    protected void credit(AccountBalanceEntity balance, BigDecimal amount) {
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
    }

    protected void saveAll(AccountBalanceEntity... balances) {
        for (AccountBalanceEntity balance : balances) {
            accountBalanceRepository.save(balance);
        }
    }
}
