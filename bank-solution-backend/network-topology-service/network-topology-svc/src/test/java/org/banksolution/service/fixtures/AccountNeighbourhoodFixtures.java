package org.banksolution.service.fixtures;

import org.banksolution.domain.AccountMovement;

import java.time.Instant;

public class AccountNeighbourhoodFixtures {

    public static final String ACCOUNT_ID = "account-under-test";

    private AccountNeighbourhoodFixtures() {
    }

    public static AccountMovement createMovement(
            String counterpartyAccountId,
            double amount,
            String timestampIso) {

        return new AccountMovement(
                counterpartyAccountId,
                amount,
                Instant.parse(timestampIso).toEpochMilli());
    }
}
