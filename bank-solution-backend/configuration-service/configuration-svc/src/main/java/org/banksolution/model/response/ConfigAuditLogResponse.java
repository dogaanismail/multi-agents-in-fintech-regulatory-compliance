package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "One recorded change to a configuration")
public class ConfigAuditLogResponse {

    @Schema(description = "Audit entry identifier", example = "0f8e2c1a-4b7d-4e9a-9c3f-2d5b6a7e8f90")
    private UUID id;

    @Schema(description = "Identifier of the changed configuration", example = "3c9a7b2e-1d4f-4a6b-8e5c-7f0d1a2b3c4d")
    private UUID configId;

    @Schema(description = "Key of the changed configuration", example = "TRAINING_INTERVAL_SECONDS")
    private String configKey;

    @Schema(description = "Value before the change", example = "300")
    private String oldValue;

    @Schema(description = "Value after the change", example = "600")
    private String newValue;

    /** CREATED | UPDATED | DELETED */
    @Schema(description = "Kind of change", example = "UPDATED")
    private String changeType;

    @Schema(description = "Actor who made the change")
    private String changedBy;

    @Schema(description = "When the change was recorded", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;
}
