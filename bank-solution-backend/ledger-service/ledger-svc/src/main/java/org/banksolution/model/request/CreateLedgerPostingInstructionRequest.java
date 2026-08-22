package org.banksolution.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Envelope carrying exactly one posting instruction, so the caller states which of the six
 * instruction types it means by which field it populates.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLedgerPostingInstructionRequest {

    @NotNull(message = "Client transaction ID can't be null.")
    private UUID clientTransactionId;

    @Valid
    private CustomerAccountMovementRequest inboundAuthorisation;

    @Valid
    private CustomerAccountMovementRequest outboundAuthorisation;

    @Valid
    private CustomerAccountMovementRequest inboundHardSettlement;

    @Valid
    private CustomerAccountMovementRequest outboundHardSettlement;

    private SettlementRequest settlement;

    private ReleaseRequest release;

    @AssertTrue(message = "Exactly one posting instruction type must be provided.")
    public boolean isExactlyOnePostingInstructionProvided() {
        return Stream.of(inboundAuthorisation, outboundAuthorisation, inboundHardSettlement,
                        outboundHardSettlement, settlement, release)
                .filter(Objects::nonNull)
                .count() == 1;
    }

    public record SettlementRequest() {
    }

    public record ReleaseRequest() {
    }
}
