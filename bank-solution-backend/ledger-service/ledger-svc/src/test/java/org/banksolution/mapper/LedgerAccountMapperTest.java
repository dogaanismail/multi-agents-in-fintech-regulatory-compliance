package org.banksolution.mapper;

import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.UInt128;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.model.response.LedgerAccountResponse;
import org.banksolution.model.response.LedgerInternalAccountResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerAccountMapperTest {

    private static final UUID LEDGER_ACCOUNT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CUSTOMER_ACCOUNT_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final long TIMESTAMP_NANOS = 1_700_000_000_123_456_789L;

    @Test
    void shouldMapWalletIdentityFromTheTigerBeetleAccount() {
        AccountBatch accountBatch = createAccountBatch(LedgerAccountType.WALLET, Currency.GBP);
        accountBatch.setUserData128(UInt128.asBytes(CUSTOMER_ACCOUNT_ID));

        LedgerAccount ledgerAccount = LedgerAccountMapper.toLedgerAccount(accountBatch);

        assertThat(ledgerAccount.id()).isEqualTo(LEDGER_ACCOUNT_ID);
        assertThat(ledgerAccount.accountId()).isEqualTo(CUSTOMER_ACCOUNT_ID);
        assertThat(ledgerAccount.accountType()).isEqualTo(LedgerAccountType.WALLET);
        assertThat(ledgerAccount.currency()).isEqualTo(Currency.GBP);
    }

    @Test
    void shouldReadTheAccountTypeFromTheTigerBeetleCode() {
        AccountBatch accountBatch = createAccountBatch(LedgerAccountType.INBOUND_CLEARING, Currency.EUR);

        assertThat(LedgerAccountMapper.toLedgerInternalAccount(accountBatch).accountType())
                .isEqualTo(LedgerAccountType.INBOUND_CLEARING);
    }

    @Test
    void shouldReadTheCurrencyFromTheTigerBeetleLedger() {
        AccountBatch accountBatch = createAccountBatch(LedgerAccountType.SUSPENSE, Currency.JPY);

        assertThat(LedgerAccountMapper.toLedgerInternalAccount(accountBatch).currency()).isEqualTo(Currency.JPY);
    }

    @Test
    void shouldConvertTheNanosecondTimestampToAnInstant() {
        AccountBatch accountBatch = createAccountBatch(LedgerAccountType.WALLET, Currency.GBP);
        accountBatch.setTimestamp(TIMESTAMP_NANOS);

        assertThat(LedgerAccountMapper.toLedgerAccount(accountBatch).createdAt())
                .isEqualTo(Instant.ofEpochSecond(1_700_000_000L, 123_456_789L));
    }

    @Test
    void shouldLeaveCreatedAtNullForAnUntimestampedAccount() {
        AccountBatch accountBatch = createAccountBatch(LedgerAccountType.WALLET, Currency.GBP);

        assertThat(LedgerAccountMapper.toLedgerAccount(accountBatch).createdAt()).isNull();
    }

    @Test
    void shouldMapTheWalletOntoItsResponseIncludingTheDerivedAvailableBalance() {
        LedgerAccount ledgerAccount = LedgerAccount.builder()
                .id(LEDGER_ACCOUNT_ID)
                .accountId(CUSTOMER_ACCOUNT_ID)
                .accountType(LedgerAccountType.WALLET)
                .currency(Currency.GBP)
                .creditsPosted(new BigDecimal("1000.00"))
                .creditsPending(new BigDecimal("50.00"))
                .debitsPosted(new BigDecimal("200.00"))
                .debitsPending(new BigDecimal("100.00"))
                .createdAt(Instant.EPOCH)
                .build();

        LedgerAccountResponse response = LedgerAccountMapper.toLedgerAccountResponse(ledgerAccount);

        assertThat(response.getLedgerAccountId()).isEqualTo(LEDGER_ACCOUNT_ID);
        assertThat(response.getAccountId()).isEqualTo(CUSTOMER_ACCOUNT_ID);
        assertThat(response.getAccountType()).isEqualTo(LedgerAccountType.WALLET);
        assertThat(response.getCurrency()).isEqualTo(Currency.GBP);
        assertThat(response.getCreditsPosted()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getCreditsPending()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.getDebitsPosted()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getDebitsPending()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(response.getCreatedAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void shouldMapTheInternalAccountOntoItsResponseIncludingTheDerivedNetBalance() {
        LedgerInternalAccount internalAccount = LedgerInternalAccount.builder()
                .id(LEDGER_ACCOUNT_ID)
                .accountType(LedgerAccountType.OUTBOUND_CLEARING)
                .currency(Currency.GBP)
                .creditsPosted(new BigDecimal("800.00"))
                .creditsPending(BigDecimal.ZERO)
                .debitsPosted(new BigDecimal("300.00"))
                .debitsPending(BigDecimal.ZERO)
                .createdAt(Instant.EPOCH)
                .build();

        LedgerInternalAccountResponse response =
                LedgerAccountMapper.toLedgerInternalAccountResponse(internalAccount);

        assertThat(response.getLedgerAccountId()).isEqualTo(LEDGER_ACCOUNT_ID);
        assertThat(response.getAccountType()).isEqualTo(LedgerAccountType.OUTBOUND_CLEARING);
        assertThat(response.getNetBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    private static AccountBatch createAccountBatch(
            LedgerAccountType accountType,
            Currency currency) {

        AccountBatch accountBatch = new AccountBatch(1);
        accountBatch.add();
        accountBatch.setId(UInt128.asBytes(LEDGER_ACCOUNT_ID));
        accountBatch.setLedger(currency.getNumericCode());
        accountBatch.setCode(accountType.getCode());

        return accountBatch;
    }
}
