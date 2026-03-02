package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "payment_request")
@Table(name = "payment_request")
public class PaymentRequestEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "from_currency", nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    private Currency fromCurrency;

    @Column(name = "to_currency", length = 3)
    @Enumerated(EnumType.STRING)
    private Currency toCurrency;

    @Column(name = "converted_amount", precision = 19, scale = 4)
    private BigDecimal convertedAmount;

    @Column(name = "payment_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "applied_exchange_rate", precision = 19, scale = 8)
    private BigDecimal appliedExchangeRate;
}

