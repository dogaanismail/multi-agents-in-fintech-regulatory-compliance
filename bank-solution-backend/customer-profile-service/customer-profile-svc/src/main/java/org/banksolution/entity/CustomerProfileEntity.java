package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private UUID customerId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transaction_count", nullable = false)
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "avg_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal avgAmount = BigDecimal.ZERO;

    @Column(name = "median_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal medianAmount = BigDecimal.ZERO;

    @Column(name = "max_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal maxAmount = BigDecimal.ZERO;

    @Column(name = "min_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal minAmount = BigDecimal.ZERO;

    @Column(name = "std_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal stdAmount = BigDecimal.ZERO;

    @Column(name = "active_days", nullable = false)
    @Builder.Default
    private Integer activeDays = 0;

    @Column(name = "transactions_per_day", precision = 10, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal transactionsPerDay = BigDecimal.ZERO;

    @Column(name = "first_transaction_date")
    private LocalDate firstTransactionDate;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    @Column(name = "cross_border_count", nullable = false)
    @Builder.Default
    private Integer crossBorderCount = 0;

    @Column(name = "cash_transaction_count", nullable = false)
    @Builder.Default
    private Integer cashTransactionCount = 0;

    @Column(name = "large_transaction_count", nullable = false)
    @Builder.Default
    private Integer largeTransactionCount = 0;

    @Column(name = "night_transaction_count", nullable = false)
    @Builder.Default
    private Integer nightTransactionCount = 0;

    @Column(name = "weekend_transaction_count", nullable = false)
    @Builder.Default
    private Integer weekendTransactionCount = 0;

    @Column(name = "cross_border_ratio", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal crossBorderRatio = BigDecimal.ZERO;

    @Column(name = "cash_transaction_ratio", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal cashTransactionRatio = BigDecimal.ZERO;

    @Column(name = "large_transaction_ratio", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal largeTransactionRatio = BigDecimal.ZERO;

    @Column(name = "night_transaction_ratio", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal nightTransactionRatio = BigDecimal.ZERO;

    @Column(name = "weekend_transaction_ratio", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal weekendTransactionRatio = BigDecimal.ZERO;

    @Column(name = "unique_receivers", nullable = false)
    @Builder.Default
    private Integer uniqueReceivers = 0;

    @Column(name = "unique_receiver_countries", nullable = false)
    @Builder.Default
    private Integer uniqueReceiverCountries = 0;

    @Column(name = "receiver_diversity", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal receiverDiversity = BigDecimal.ZERO;

    @Column(name = "unique_currencies", nullable = false)
    @Builder.Default
    private Integer uniqueCurrencies = 0;

    @Column(name = "amount_consistency", precision = 5, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal amountConsistency = BigDecimal.ZERO;

    @Column(name = "last_updated_at", nullable = false)
    @UpdateTimestamp
    private Instant lastUpdatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 0;
}