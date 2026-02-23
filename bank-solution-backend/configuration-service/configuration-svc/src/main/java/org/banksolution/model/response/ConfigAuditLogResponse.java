package org.banksolution.model.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigAuditLogResponse {

    private UUID id;
    private UUID configId;
    private String configKey;
    private String oldValue;
    private String newValue;
    /** CREATED | UPDATED | DELETED */
    private String changeType;
    private String changedBy;
    private Instant createdAt;
}
