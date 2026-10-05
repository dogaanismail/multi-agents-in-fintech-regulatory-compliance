package org.banksolution.mapper;

import org.banksolution.enums.CustomerPaymentStatus;
import org.banksolution.integration.payment.model.PaymentRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_ACCOUNT_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.createMobilePaymentRequest;
import static org.banksolution.fixtures.MobileGatewayFixtures.createPaymentHistoryPage;
import static org.banksolution.fixtures.MobileGatewayFixtures.createPaymentRequestResponse;

class PaymentMapperTest {

    @Test
    void shouldSendASameCurrencyOutgoingTransferForTheCaller() {
        PaymentRequest paymentRequest = PaymentMapper.toPaymentRequest(CALLER_CUSTOMER_ID, createMobilePaymentRequest(CALLER_ACCOUNT_ID));

        assertThat(paymentRequest.customerId()).isEqualTo(CALLER_CUSTOMER_ID);
        assertThat(paymentRequest.paymentType()).isEqualTo("TRANSFER_OUT");
        assertThat(paymentRequest.fromCurrency()).isEqualTo(paymentRequest.toCurrency()).isEqualTo("GBP");
    }

    @Test
    void shouldReportAJustSubmittedPaymentAsProcessing() {
        assertThat(PaymentMapper.toSubmittedMobilePaymentResponse(createPaymentRequestResponse()).status())
                .isEqualTo(CustomerPaymentStatus.PROCESSING);
    }

    @Test
    void shouldTranslateTheInternalStatusOfAPaymentInHistory() {
        assertThat(PaymentMapper.toMobilePaymentResponse(createPaymentHistoryPage("BLOCKED").content().getFirst()).status())
                .isEqualTo(CustomerPaymentStatus.DECLINED);
    }
}
