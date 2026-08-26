package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;
import org.banksolution.enums.PaymentScheme;
import org.banksolution.enums.PaymentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentRequestRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        PaymentRequestEntity paymentRequestEntity = createPaymentRequestEntity(UUID.randomUUID());

        UUID savedPaymentId = paymentRequestRepository.saveAndFlush(paymentRequestEntity).getId();
        PaymentRequestEntity reloadedPaymentRequestEntity = paymentRequestRepository.findById(savedPaymentId).orElseThrow();

        assertThat(reloadedPaymentRequestEntity.getCustomerId()).isEqualTo(paymentRequestEntity.getCustomerId());
        assertThat(reloadedPaymentRequestEntity.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(reloadedPaymentRequestEntity.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID);
        assertThat(reloadedPaymentRequestEntity.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(reloadedPaymentRequestEntity.getFromCurrency()).isEqualTo(Currency.GBP);
        assertThat(reloadedPaymentRequestEntity.getToCurrency()).isEqualTo(Currency.EUR);
        assertThat(reloadedPaymentRequestEntity.getConvertedAmount()).isEqualByComparingTo("116.00");
        assertThat(reloadedPaymentRequestEntity.getAppliedExchangeRate()).isEqualByComparingTo(GBP_TO_EUR_RATE);
        assertThat(reloadedPaymentRequestEntity.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(reloadedPaymentRequestEntity.getPaymentScheme()).isEqualTo(PaymentScheme.INTERNAL_TRANSFER);
        assertThat(reloadedPaymentRequestEntity.getFixedSide()).isEqualTo(FixedSide.SELL);
        assertThat(reloadedPaymentRequestEntity.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(reloadedPaymentRequestEntity.getCreatedAt()).isNotNull();
        assertThat(reloadedPaymentRequestEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldAcceptEveryEnumValueAgainstTheCheckConstraints() {
        for (Currency currency : Currency.values()) {
            PaymentRequestEntity paymentRequestEntity = createPaymentRequestEntity(UUID.randomUUID());
            paymentRequestEntity.setFromCurrency(currency);
            paymentRequestEntity.setToCurrency(currency);
            paymentRequestRepository.saveAndFlush(paymentRequestEntity);
        }
        for (PaymentType paymentType : PaymentType.values()) {
            for (PaymentScheme paymentScheme : PaymentScheme.values()) {
                for (FixedSide fixedSide : FixedSide.values()) {
                    PaymentRequestEntity paymentRequestEntity = createPaymentRequestEntity(UUID.randomUUID());
                    paymentRequestEntity.setPaymentType(paymentType);
                    paymentRequestEntity.setPaymentScheme(paymentScheme);
                    paymentRequestEntity.setFixedSide(fixedSide);

                    UUID savedPaymentId = paymentRequestRepository.saveAndFlush(paymentRequestEntity).getId();

                    assertThat(paymentRequestRepository.findById(savedPaymentId)).map(PaymentRequestEntity::getPaymentScheme).contains(paymentScheme);
                }
            }
        }
    }

    @Test
    void shouldRejectANonPositiveAmount() {
        PaymentRequestEntity paymentRequestEntity = createPaymentRequestEntity(UUID.randomUUID());
        paymentRequestEntity.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> paymentRequestRepository.saveAndFlush(paymentRequestEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindOnlyTheCustomersPayments() {
        UUID customerId = UUID.randomUUID();
        paymentRequestRepository.saveAndFlush(createPaymentRequestEntity(customerId));
        paymentRequestRepository.saveAndFlush(createPaymentRequestEntity(customerId));
        paymentRequestRepository.saveAndFlush(createPaymentRequestEntity(UUID.randomUUID()));

        assertThat(paymentRequestRepository.findByCustomerId(customerId))
                .hasSize(2)
                .allSatisfy(paymentRequestEntity -> assertThat(paymentRequestEntity.getCustomerId()).isEqualTo(customerId));
    }
}
