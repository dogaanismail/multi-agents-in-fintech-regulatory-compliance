package org.banksolution.mapper;

import com.aml.payment.PaymentCompletedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.TransactionSnapshotEntity;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.banksolution.util.TransactionClassificationUtil.*;

@UtilityClass
public class TransactionSnapshotMapper {

    public TransactionSnapshotEntity toEntity(PaymentCompletedEvent event) {
        Instant timestamp = Instant.ofEpochMilli(event.getTimestamp());
        LocalDateTime txTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());

        TransactionSnapshotEntity transactionSnapshotEntity = TransactionSnapshotEntity.builder()
                .customerId(UUID.fromString(event.getCustomerId()))
                .accountId(UUID.fromString(event.getSourceAccountId()))
                .paymentId(event.getPaymentId())
                .amount(new BigDecimal(event.getAmount()))
                .currency(event.getCurrency())
                .paymentType(event.getPaymentType().name())
                .senderBankLocation(event.getSourceAccountBankLocation())
                .receiverBankLocation(event.getDestinationAccountBankLocation())
                .receiverAccountId(event.getDestinationAccountId())
                .transactionTimestamp(timestamp)
                .transactionDate(txTime.toLocalDate())
                .transactionTime(txTime.toLocalTime())
                .isCrossBorder(event.getIsCrossBorderPayment())
                .build();

        enrichWithClassifications(transactionSnapshotEntity, txTime);
        return transactionSnapshotEntity;
    }

    private void enrichWithClassifications(TransactionSnapshotEntity transactionSnapshotEntity, LocalDateTime txTime) {
        transactionSnapshotEntity.setCashTransaction(isCashTransaction(transactionSnapshotEntity.getPaymentType()));
        transactionSnapshotEntity.setLargeTransaction(isLargeTransaction(transactionSnapshotEntity.getAmount()));
        transactionSnapshotEntity.setNightTransaction(isNightTransaction(txTime));
        transactionSnapshotEntity.setWeekendTransaction(isWeekendTransaction(txTime));
        transactionSnapshotEntity.setReceiverCountry(transactionSnapshotEntity.getReceiverBankLocation());
    }

}
