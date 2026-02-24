package org.banksolution.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverrideDecisionRequest {

    private UUID paymentId;
    private String overriddenBy;
    private String overrideReason;
    private boolean approvePayment;
}
