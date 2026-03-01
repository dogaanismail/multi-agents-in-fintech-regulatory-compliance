package org.banksolution.model;

import org.banksolution.integration.account.dto.AccountResponse;

public record PaymentAccounts(AccountResponse source, AccountResponse destination) {
}
