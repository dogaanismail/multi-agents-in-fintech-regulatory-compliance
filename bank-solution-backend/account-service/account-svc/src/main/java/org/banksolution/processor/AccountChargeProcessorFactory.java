package org.banksolution.processor;

import com.aml.account.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountChargeProcessorFactory {

    private final TransferChargeProcessor transferChargeProcessor;
    private final DepositChargeProcessor depositChargeProcessor;
    private final WithdrawalChargeProcessor withdrawalChargeProcessor;

    public AccountChargeProcessor getProcessor(PaymentType paymentType) {
        return switch (paymentType) {
            case TRANSFER_IN, TRANSFER_OUT -> transferChargeProcessor;
            case DEPOSIT -> depositChargeProcessor;
            case WITHDRAWAL -> withdrawalChargeProcessor;
        };
    }
}
