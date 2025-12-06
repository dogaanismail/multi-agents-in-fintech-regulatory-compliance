package org.banksolution.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_id_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIdMapping {

    @Id
    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;
}
