package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Envelope carrying exactly one posting instruction")
public class CreateLedgerPostingInstructionRequest {

    @Schema(description = "Client transaction the posting belongs to", example = "b1f4d8e2-6a3c-4f7b-8d9e-2c5a7e0f1b46")
    @NotNull(message = "Client transaction ID can't be null.")
    private UUID clientTransactionId;

    @Schema(description = "Hold funds for an incoming payment")
    @Valid
    private CustomerAccountMovementRequest inboundAuthorisation;

    @Schema(description = "Hold funds for an outgoing payment")
    @Valid
    private CustomerAccountMovementRequest outboundAuthorisation;

    @Schema(description = "Credit an incoming payment in one step")
    @Valid
    private CustomerAccountMovementRequest inboundHardSettlement;

    @Schema(description = "Debit an outgoing payment in one step")
    @Valid
    private CustomerAccountMovementRequest outboundHardSettlement;

    @Schema(description = "Hold funds for an account-to-account transfer")
    @Valid
    private InternalTransferMovementRequest internalTransferAuthorisation;

    @Schema(description = "Settle the transaction's pending authorisation")
    private SettlementRequest settlement;

    @Schema(description = "Release the transaction's pending authorisation")
    private ReleaseRequest release;

    @AssertTrue(message = "Exactly one posting instruction type must be provided.")
    public boolean isExactlyOnePostingInstructionProvided() {
        return Stream.of(inboundAuthorisation, outboundAuthorisation, inboundHardSettlement,
                        outboundHardSettlement, internalTransferAuthorisation, settlement, release)
                .filter(Objects::nonNull)
                .count() == 1;
    }

    @Schema(description = "Settles the pending authorisation; carries no fields")
    public record SettlementRequest() {
    }

    @Schema(description = "Voids the pending authorisation; carries no fields")
    public record ReleaseRequest() {
    }
}
