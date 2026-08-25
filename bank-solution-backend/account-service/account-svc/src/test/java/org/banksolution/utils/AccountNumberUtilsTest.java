package org.banksolution.utils;

import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNumberUtilsTest {

    @RepeatedTest(20)
    void shouldGenerateATenDigitNumberThatNeverStartsWithZero() {
        String accountNumber = AccountNumberUtils.generateAccountNumber();

        assertThat(accountNumber).hasSize(10).matches("[1-9][0-9]{9}");
    }
}
