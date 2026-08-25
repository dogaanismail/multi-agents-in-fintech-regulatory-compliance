package org.banksolution.mapper;

import org.banksolution.enums.MarlPaymentType;
import org.banksolution.enums.PaymentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.mapper.PaymentTypeMapper.toMarlPaymentType;

class PaymentTypeMapperTest {

    @Test
    void shouldMapDomesticTransfersToAch() {
        assertThat(toMarlPaymentType(PaymentType.TRANSFER_IN, false)).isEqualTo(MarlPaymentType.ACH);
        assertThat(toMarlPaymentType(PaymentType.TRANSFER_OUT, false)).isEqualTo(MarlPaymentType.ACH);
    }

    @Test
    void shouldMapCrossBorderTransfersToCrossBorder() {
        assertThat(toMarlPaymentType(PaymentType.TRANSFER_IN, true)).isEqualTo(MarlPaymentType.CROSS_BORDER);
        assertThat(toMarlPaymentType(PaymentType.TRANSFER_OUT, true)).isEqualTo(MarlPaymentType.CROSS_BORDER);
    }

    @Test
    void shouldMapDepositToCashDepositRegardlessOfCrossBorderFlag() {
        assertThat(toMarlPaymentType(PaymentType.DEPOSIT, false)).isEqualTo(MarlPaymentType.CASH_DEPOSIT);
        assertThat(toMarlPaymentType(PaymentType.DEPOSIT, true)).isEqualTo(MarlPaymentType.CASH_DEPOSIT);
    }

    @Test
    void shouldMapWithdrawalToCashWithdrawalRegardlessOfCrossBorderFlag() {
        assertThat(toMarlPaymentType(PaymentType.WITHDRAWAL, false)).isEqualTo(MarlPaymentType.CASH_WITHDRAWAL);
        assertThat(toMarlPaymentType(PaymentType.WITHDRAWAL, true)).isEqualTo(MarlPaymentType.CASH_WITHDRAWAL);
    }
}
