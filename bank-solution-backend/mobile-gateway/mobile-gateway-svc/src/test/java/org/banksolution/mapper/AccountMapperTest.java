package org.banksolution.mapper;

import org.banksolution.integration.account.model.AccountResponse;
import org.banksolution.integration.account.model.OpenAccountRequest;
import org.banksolution.model.request.MobileOpenAccountRequest;
import org.banksolution.model.response.MobileAccountResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_ACCOUNT_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.createAccountResponse;

class AccountMapperTest {

    @Test
    void shouldOpenTheAccountForTheGivenCustomer() {
        OpenAccountRequest openAccountRequest = AccountMapper.toOpenAccountRequest(
                CALLER_CUSTOMER_ID, new MobileOpenAccountRequest("SAVINGS", "GB", List.of("GBP", "EUR")));

        assertThat(openAccountRequest.customerId()).isEqualTo(CALLER_CUSTOMER_ID);
        assertThat(openAccountRequest.currencies()).containsExactly("GBP", "EUR");
    }

    @Test
    void shouldCarryEveryWalletBalance() {
        MobileAccountResponse mobileAccountResponse =
                AccountMapper.toMobileAccountResponse(createAccountResponse(CALLER_ACCOUNT_ID, CALLER_CUSTOMER_ID));

        assertThat(mobileAccountResponse.wallets()).singleElement()
                .satisfies(wallet -> assertThat(wallet.availableBalance()).isEqualByComparingTo(new BigDecimal("200.00")));
    }

    @Test
    void shouldTreatAnAccountWithoutWalletsAsHavingNone() {
        AccountResponse accountWithoutWallets =
                new AccountResponse(CALLER_ACCOUNT_ID, CALLER_CUSTOMER_ID, "GB00BANK0002", "CHECKING", "ACTIVE", null);

        assertThat(AccountMapper.toMobileAccountResponse(accountWithoutWallets).wallets()).isEmpty();
    }
}
