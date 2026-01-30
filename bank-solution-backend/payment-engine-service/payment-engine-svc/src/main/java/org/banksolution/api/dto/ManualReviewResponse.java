package org.banksolution.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewResponse {
    
    private String paymentId;
    private String message;
    private String reviewedBy;
}
