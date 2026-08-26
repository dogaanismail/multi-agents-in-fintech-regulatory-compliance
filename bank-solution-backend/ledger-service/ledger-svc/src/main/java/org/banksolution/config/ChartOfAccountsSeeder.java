package org.banksolution.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.repository.TigerBeetleInternalAccountRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ledger.chart-of-accounts.seed-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountsSeeder implements ApplicationRunner {

    private final TigerBeetleInternalAccountRepository tigerBeetleInternalAccountRepository;
    private final ChartOfAccountsProperties chartOfAccountsProperties;

    @Override
    public void run(@NonNull ApplicationArguments applicationArguments) {
        List<LedgerInternalAccount> internalAccounts = chartOfAccountsProperties.currencies().stream()
                .flatMap(currency -> Arrays.stream(LedgerAccountType.internalTypes())
                        .map(internalAccountType -> LedgerInternalAccount.newInternalAccount(internalAccountType, currency)))
                .toList();

        try {
            tigerBeetleInternalAccountRepository.persistInternalAccounts(internalAccounts);
            log.info("Chart of accounts ready: {} internal accounts across {}",
                    internalAccounts.size(), chartOfAccountsProperties.currencies());
        } catch (Exception seedingFailure) {
            log.error("Chart of accounts seeding failed; internal accounts may be missing", seedingFailure);
        }
    }
}
