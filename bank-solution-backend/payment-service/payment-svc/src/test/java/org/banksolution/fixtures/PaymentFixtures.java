package org.banksolution.fixtures;

import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.enums.AccountStatus;
import org.banksolution.enums.AccountType;
import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;
import org.banksolution.enums.PaymentScheme;
import org.banksolution.enums.PaymentType;
import org.banksolution.integration.account.dto.AccountResponse;
import org.banksolution.integration.account.dto.AccountWalletResponse;
import org.banksolution.integration.exchangerate.dto.ExchangeRateApiResponse;
import org.banksolution.model.PaymentAccounts;
import org.banksolution.model.request.PaymentRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PaymentFixtures {

    public static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID SOURCE_ACCOUNT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DESTINATION_ACCOUNT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final BigDecimal AMOUNT = new BigDecimal("100.00");
    public static final BigDecimal GBP_TO_EUR_RATE = new BigDecimal("1.16000000");
    public static final Instant FETCHED_AT = Instant.parse("2026-08-27T10:00:00Z");
    public static final String DESCRIPTION = "Rent";

    private PaymentFixtures() {
    }

    public static PaymentRequest createTransferOutRequest(UUID customerId, Currency fromCurrency, Currency toCurrency) {
        return PaymentRequest.builder()
                .customerId(customerId)
                .sourceAccountId(SOURCE_ACCOUNT_ID)
                .destinationAccountId(DESTINATION_ACCOUNT_ID)
                .amount(AMOUNT)
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .paymentType(PaymentType.TRANSFER_OUT)
                .fixedSide(FixedSide.SELL)
                .description(DESCRIPTION)
                .build();
    }

    public static PaymentRequest createDepositRequest(UUID customerId) {
        return PaymentRequest.builder()
                .customerId(customerId)
                .destinationAccountId(DESTINATION_ACCOUNT_ID)
                .amount(AMOUNT)
                .fromCurrency(Currency.GBP)
                .toCurrency(Currency.GBP)
                .paymentType(PaymentType.DEPOSIT)
                .build();
    }

    public static PaymentRequestEntity createPaymentRequestEntity(UUID customerId) {
        return PaymentRequestEntity.builder()
                .customerId(customerId)
                .sourceAccountId(SOURCE_ACCOUNT_ID)
                .destinationAccountId(DESTINATION_ACCOUNT_ID)
                .amount(AMOUNT)
                .fromCurrency(Currency.GBP)
                .toCurrency(Currency.EUR)
                .convertedAmount(new BigDecimal("116.00"))
                .appliedExchangeRate(GBP_TO_EUR_RATE)
                .paymentType(PaymentType.TRANSFER_OUT)
                .paymentScheme(PaymentScheme.INTERNAL_TRANSFER)
                .fixedSide(FixedSide.SELL)
                .description(DESCRIPTION)
                .build();
    }

    public static PaymentRequestEntity createPersistedPaymentRequestEntity(UUID paymentId, UUID customerId) {
        PaymentRequestEntity paymentRequestEntity = createPaymentRequestEntity(customerId);
        paymentRequestEntity.setId(paymentId);
        paymentRequestEntity.setCreatedAt(FETCHED_AT);
        return paymentRequestEntity;
    }

    public static ExchangeRateEntity createExchangeRateEntity(Currency fromCurrency, Currency toCurrency, String rate) {
        return ExchangeRateEntity.builder()
                .currencyPair(fromCurrency.name() + toCurrency.name())
                .rate(new BigDecimal(rate))
                .fetchedAt(FETCHED_AT)
                .build();
    }

    public static AccountResponse createAccountResponse(UUID accountId, String bankLocation) {
        return AccountResponse.builder()
                .id(accountId)
                .customerId(CUSTOMER_ID)
                .accountNumber("1234567890")
                .accountType(AccountType.CHECKING)
                .bankLocation(bankLocation)
                .accountStatus(AccountStatus.ACTIVE)
                .openingDate(LocalDate.of(2026, 1, 1))
                .wallets(List.of(AccountWalletResponse.builder()
                        .id(UUID.randomUUID())
                        .ledgerAccountId(UUID.randomUUID())
                        .currency("GBP")
                        .walletStatus("ACTIVE")
                        .balance(new BigDecimal("1000.00"))
                        .primary(true)
                        .build()))
                .build();
    }

    public static PaymentAccounts createPaymentAccounts(String sourceBankLocation, String destinationBankLocation) {
        return new PaymentAccounts(
                createAccountResponse(SOURCE_ACCOUNT_ID, sourceBankLocation),
                createAccountResponse(DESTINATION_ACCOUNT_ID, destinationBankLocation));
    }

    public static ExchangeRateApiResponse createExchangeRateApiResponse(Map<String, BigDecimal> gbpRates) {
        return ExchangeRateApiResponse.builder()
                .base("GBP")
                .date("2026-08-27")
                .timeLastUpdated(FETCHED_AT.getEpochSecond())
                .rates(gbpRates)
                .build();
    }
}
