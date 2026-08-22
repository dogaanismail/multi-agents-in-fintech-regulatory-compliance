package org.banksolution.mapper;

import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.UInt128;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.model.response.LedgerAccountResponse;
import org.banksolution.model.response.LedgerInternalAccountResponse;
import org.banksolution.util.MoneyUtils;

import java.time.Instant;

public final class LedgerAccountMapper {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private LedgerAccountMapper() {
    }

    public static LedgerAccount toLedgerAccount(AccountBatch batch) {
        Currency currency = Currency.fromNumericCode(batch.getLedger());

        return LedgerAccount.builder()
                .id(UInt128.asUUID(batch.getId()))
                .accountId(UInt128.asUUID(batch.getUserData128()))
                .accountType(LedgerAccountType.fromCode(batch.getCode()))
                .currency(currency)
                .creditsPosted(MoneyUtils.toAmount(batch.getCreditsPosted(), currency))
                .creditsPending(MoneyUtils.toAmount(batch.getCreditsPending(), currency))
                .debitsPosted(MoneyUtils.toAmount(batch.getDebitsPosted(), currency))
                .debitsPending(MoneyUtils.toAmount(batch.getDebitsPending(), currency))
                .createdAt(toInstant(batch.getTimestamp()))
                .build();
    }

    public static LedgerInternalAccount toLedgerInternalAccount(
            AccountBatch batch) {

        Currency currency = Currency.fromNumericCode(batch.getLedger());

        return LedgerInternalAccount.builder()
                .id(UInt128.asUUID(batch.getId()))
                .accountType(LedgerAccountType.fromCode(batch.getCode()))
                .currency(currency)
                .creditsPosted(MoneyUtils.toAmount(batch.getCreditsPosted(), currency))
                .creditsPending(MoneyUtils.toAmount(batch.getCreditsPending(), currency))
                .debitsPosted(MoneyUtils.toAmount(batch.getDebitsPosted(), currency))
                .debitsPending(MoneyUtils.toAmount(batch.getDebitsPending(), currency))
                .createdAt(toInstant(batch.getTimestamp()))
                .build();
    }

    public static LedgerAccountResponse toLedgerAccountResponse(
            LedgerAccount account) {

        return LedgerAccountResponse.builder()
                .ledgerAccountId(account.id())
                .accountId(account.accountId())
                .accountType(account.accountType())
                .currency(account.currency())
                .creditsPosted(account.creditsPosted())
                .creditsPending(account.creditsPending())
                .debitsPosted(account.debitsPosted())
                .debitsPending(account.debitsPending())
                .availableBalance(account.availableBalance())
                .createdAt(account.createdAt())
                .build();
    }

    public static LedgerInternalAccountResponse toLedgerInternalAccountResponse(
            LedgerInternalAccount account) {

        return LedgerInternalAccountResponse.builder()
                .ledgerAccountId(account.id())
                .accountType(account.accountType())
                .currency(account.currency())
                .creditsPosted(account.creditsPosted())
                .creditsPending(account.creditsPending())
                .debitsPosted(account.debitsPosted())
                .debitsPending(account.debitsPending())
                .netBalance(account.netBalance())
                .createdAt(account.createdAt())
                .build();
    }

    private static Instant toInstant(long timestampNanos) {
        if (timestampNanos == 0) {
            return null;
        }

        return Instant.ofEpochSecond(timestampNanos / NANOS_PER_SECOND, timestampNanos % NANOS_PER_SECOND);
    }
}
