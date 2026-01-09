package org.banksolution.entity;

import com.aml.risk.MarlAction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marl_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarlAssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_check_request_id", nullable = false)
    private RiskCheckRequestEntity riskCheckRequest;

    @Column(name = "action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MarlAction action;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "maddpg_q_value", nullable = false, precision = 10, scale = 6)
    private BigDecimal maddpgQValue;

    @Column(name = "processing_time_ms", nullable = false, precision = 10, scale = 2)
    private BigDecimal processingTimeMs;

    @Column(name = "mode", nullable = false, length = 20)
    private String mode;

    @Column(name = "response_timestamp", nullable = false)
    private Long responseTimestamp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

}
