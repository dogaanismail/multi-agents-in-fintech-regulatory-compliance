package org.banksolution.entity;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "risk_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_check_request_id", nullable = false)
    private RiskCheckRequestEntity riskCheckRequest;

    @Column(name = "risk_score", precision = 5, scale = 4)
    private BigDecimal riskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "risk_action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskAction riskAction;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fraud_indicators", columnDefinition = "text[]")
    private List<String> fraudIndicators;

    @Column(name = "ml_model_version", length = 50)
    private String mlModelVersion;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @PrePersist
    protected void onCreate() {
        if (assessedAt == null) {
            assessedAt = Instant.now();
        }
    }
}
