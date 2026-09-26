package org.banksolution.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.enums.BeneficiaryType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryResponse {

    private UUID id;
    private UUID customerId;
    private BeneficiaryType type;
    private String alias;
    private String companyName;
    private String firstName;
    private String lastName;
    private BeneficiaryStatus beneficiaryStatus;
    private List<BeneficiaryCoordinateResponse> coordinates;
    private Instant createdAt;
    private Instant updatedAt;

}
