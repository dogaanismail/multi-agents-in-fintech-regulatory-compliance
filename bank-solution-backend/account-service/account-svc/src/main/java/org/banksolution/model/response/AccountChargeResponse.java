package org.banksolution.model.response;

import lombok.Getter;

@Getter
public final class AccountChargeResponse {

    private final boolean success;
    private final String failureReason;

    private AccountChargeResponse(boolean success, String failureReason) {
        this.success = success;
        this.failureReason = failureReason;
    }

    public static AccountChargeResponse success() {
        return new AccountChargeResponse(true, null);
    }

    public static AccountChargeResponse failure(String reason) {
        return new AccountChargeResponse(false, reason);
    }

}
