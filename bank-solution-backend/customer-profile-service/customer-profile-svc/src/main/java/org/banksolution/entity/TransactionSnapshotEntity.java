package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "payment_id", nullable = false, unique = true, length = 100)
    private String paymentId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "from_currency", length = 10, nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", length = 10, nullable = false)
    private String toCurrency;

    @Column(name = "payment_type", length = 50, nullable = false)
    private String paymentType;

    @Column(name = "sender_bank_location", length = 50)
    private String senderBankLocation;

    @Column(name = "receiver_bank_location", length = 50)
    private String receiverBankLocation;

    @Column(name = "is_cross_border", nullable = false)
    @Builder.Default
    private boolean isCrossBorder = false;

    @Column(name = "is_cash_transaction", nullable = false)
    @Builder.Default
    private boolean isCashTransaction = false;

    @Column(name = "is_large_transaction", nullable = false)
    @Builder.Default
    private boolean isLargeTransaction = false;

    @Column(name = "is_night_transaction", nullable = false)
    @Builder.Default
    private boolean isNightTransaction = false;

    @Column(name = "is_weekend_transaction", nullable = false)
    @Builder.Default
    private boolean isWeekendTransaction = false;

    @Column(name = "receiver_account_id", length = 100)
    private String receiverAccountId;

    @Column(name = "receiver_country", length = 50)
    private String receiverCountry;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "transaction_timestamp", nullable = false)
    private Instant transactionTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}