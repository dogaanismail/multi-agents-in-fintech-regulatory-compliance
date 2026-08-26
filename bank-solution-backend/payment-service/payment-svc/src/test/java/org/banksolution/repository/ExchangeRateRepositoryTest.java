package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.FETCHED_AT;
import static org.banksolution.fixtures.PaymentFixtures.createExchangeRateEntity;

class ExchangeRateRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Test
    void shouldPersistTheRateWithEightDecimalsAndBumpTheVersionOnUpdate() {
        ExchangeRateEntity exchangeRateEntity = createExchangeRateEntity(Currency.MAD, Currency.PKR, "27.12345678");

        UUID savedExchangeRateId = exchangeRateRepository.saveAndFlush(exchangeRateEntity).getId();
        ExchangeRateEntity persistedExchangeRateEntity = exchangeRateRepository.findById(savedExchangeRateId).orElseThrow();
        short versionBeforeUpdate = persistedExchangeRateEntity.getVersion();
        persistedExchangeRateEntity.setRate(new BigDecimal("27.20000000"));
        exchangeRateRepository.saveAndFlush(persistedExchangeRateEntity);

        ExchangeRateEntity updatedExchangeRateEntity = exchangeRateRepository.findById(savedExchangeRateId).orElseThrow();
        assertThat(updatedExchangeRateEntity.getRate()).isEqualByComparingTo("27.2");
        assertThat(updatedExchangeRateEntity.getFetchedAt()).isEqualTo(FETCHED_AT);
        assertThat(updatedExchangeRateEntity.getVersion()).isEqualTo((short) (versionBeforeUpdate + 1));
        assertThat(exchangeRateRepository.findByCurrencyPair("MADPKR")).isPresent();
    }

    @Test
    void shouldRejectADuplicatePairAndANonPositiveRate() {
        exchangeRateRepository.saveAndFlush(createExchangeRateEntity(Currency.TRY, Currency.AED, "0.12000000"));
        ExchangeRateEntity duplicateExchangeRateEntity = createExchangeRateEntity(Currency.TRY, Currency.AED, "0.13000000");
        ExchangeRateEntity zeroExchangeRateEntity = createExchangeRateEntity(Currency.AED, Currency.TRY, "0");

        assertThatThrownBy(() -> exchangeRateRepository.saveAndFlush(duplicateExchangeRateEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> exchangeRateRepository.saveAndFlush(zeroExchangeRateEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
