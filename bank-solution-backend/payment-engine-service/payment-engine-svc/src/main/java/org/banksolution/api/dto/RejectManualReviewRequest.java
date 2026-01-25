package org.banksolution.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectManualReviewRequest {
    
    private UUID paymentId;
    private String rejectedBy;
    private String rejectionReason;
}
