package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.enums.MarlPaymentType;
import org.banksolution.enums.PaymentType;

@UtilityClass
public class PaymentTypeMapper {

    public static MarlPaymentType toMarlPaymentType(PaymentType paymentType, boolean isCrossBorder) {
        return switch (paymentType) {
            case TRANSFER_IN, TRANSFER_OUT -> isCrossBorder ? MarlPaymentType.CROSS_BORDER : MarlPaymentType.ACH;
            case DEPOSIT -> MarlPaymentType.CASH_DEPOSIT;
            case WITHDRAWAL -> MarlPaymentType.CASH_WITHDRAWAL;
        };
    }
}
