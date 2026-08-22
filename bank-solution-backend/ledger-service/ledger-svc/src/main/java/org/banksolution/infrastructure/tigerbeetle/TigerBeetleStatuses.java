package org.banksolution.infrastructure.tigerbeetle;

import com.tigerbeetle.CreateAccountStatus;

public final class TigerBeetleStatuses {

    private TigerBeetleStatuses() {
    }

    static boolean isAccountPersisted(CreateAccountStatus status) {
        return status == CreateAccountStatus.Created || status == CreateAccountStatus.Exists;
    }
}
