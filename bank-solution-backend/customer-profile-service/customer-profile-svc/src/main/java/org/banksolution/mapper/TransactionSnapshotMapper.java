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
                .id(UUID.randomUUID())
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
                .build();

        enrichWithClassifications(transactionSnapshotEntity, txTime);
        return transactionSnapshotEntity;
    }

    private void enrichWithClassifications(TransactionSnapshotEntity transactionSnapshotEntity, LocalDateTime txTime) {
        transactionSnapshotEntity.setIsCrossBorder(isCrossBorder(transactionSnapshotEntity.getSenderBankLocation(), transactionSnapshotEntity.getReceiverBankLocation()));
        transactionSnapshotEntity.setIsCashTransaction(isCashTransaction(transactionSnapshotEntity.getPaymentType()));
        transactionSnapshotEntity.setIsLargeTransaction(isLargeTransaction(transactionSnapshotEntity.getAmount()));
        transactionSnapshotEntity.setIsNightTransaction(isNightTransaction(txTime));
        transactionSnapshotEntity.setIsWeekendTransaction(isWeekendTransaction(txTime));
        transactionSnapshotEntity.setReceiverCountry(transactionSnapshotEntity.getReceiverBankLocation());
    }

}
