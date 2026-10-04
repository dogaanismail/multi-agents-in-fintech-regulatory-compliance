package org.banksolution.domain;

import org.banksolution.model.response.PaymentRequestResponse;

public record PaymentRequestResult(PaymentRequestResponse paymentRequestResponse, boolean replayed) {
}
