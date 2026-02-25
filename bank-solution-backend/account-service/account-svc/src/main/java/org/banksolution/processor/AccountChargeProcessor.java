package org.banksolution.processor;

import com.aml.account.AccountChargeRequestedEvent;

public interface AccountChargeProcessor {

    void process(AccountChargeRequestedEvent event);
}
