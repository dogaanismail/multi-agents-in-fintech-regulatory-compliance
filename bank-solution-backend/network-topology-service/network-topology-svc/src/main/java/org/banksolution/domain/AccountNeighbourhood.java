package org.banksolution.domain;

import java.util.List;
import java.util.Set;

public record AccountNeighbourhood(
        String accountId,
        Set<String> senderAccountIds,
        Set<String> receiverAccountIds,
        int cycle3Count,
        int twoHopOutReach,
        List<AccountMovement> incomingMovements,
        List<AccountMovement> outgoingMovements) {

    public static AccountNeighbourhood empty(
            String accountId) {

        return new AccountNeighbourhood(accountId,
                Set.of(),
                Set.of(),
                0,
                0,
                List.of(),
                List.of());
    }
}
