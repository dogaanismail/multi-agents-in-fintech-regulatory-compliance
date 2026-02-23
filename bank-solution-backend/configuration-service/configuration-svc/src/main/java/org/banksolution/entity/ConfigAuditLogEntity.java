package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "system_config_audit_log")
@Table(name = "system_config_audit_log")
public class ConfigAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "config_id", nullable = false, updatable = false)
    private UUID configId;

    @Column(name = "config_key", nullable = false, length = 100, updatable = false)
    private String configKey;

    @Column(name = "old_value", length = 500, updatable = false)
    private String oldValue;

    @Column(name = "new_value", length = 500, updatable = false)
    private String newValue;

    /**
     * CREATED | UPDATED | DELETED
     */
    @Column(name = "change_type", nullable = false, length = 20, updatable = false)
    private String changeType;

    /**
     * Actor performing the change.  Hardcoded to "SYSTEM" until an
     * authentication layer is introduced.
     */
    @Column(name = "changed_by", nullable = false, length = 100, updatable = false)
    private String changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (changedBy == null) {
            changedBy = "SYSTEM";
        }
    }
}
