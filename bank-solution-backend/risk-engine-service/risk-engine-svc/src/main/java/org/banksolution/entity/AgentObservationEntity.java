package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.banksolution.enums.AgentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_observation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marl_assessment_id", nullable = false)
    private MarlAssessmentEntity marlAssessment;

    @Column(name = "agent_name", nullable = false, length = 50)
    private String agentName;

    @Column(name = "agent_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    @Column(name = "is_suspicious", nullable = false)
    private Boolean isSuspicious;

    @Column(name = "probability", nullable = false, precision = 5, scale = 4)
    private BigDecimal probability;

    @Column(name = "risk_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "confidence", nullable = false, length = 20)
    private String confidence;

    @Column(name = "response_time_ms", nullable = false, precision = 10, scale = 2)
    private BigDecimal responseTimeMs;

    @Column(name = "contribution", precision = 5, scale = 4)
    private BigDecimal contribution;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
