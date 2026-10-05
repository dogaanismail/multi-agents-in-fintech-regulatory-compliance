package org.banksolution.integration.paymenthistory.model;

import java.util.List;

public record PaymentHistoryPage(List<PaymentHistoryResponse> content) {
}
