package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PostingInstructionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "One ledger transfer recorded for a posting")
public class LedgerPostingResponse {

    @Schema(description = "Ledger transfer identifier", example = "e9a3c6b1-2d7f-4e8a-b5c4-8f1d0a3e6b27")
    private UUID transferId;

    @Schema(description = "Client transaction the posting belongs to", example = "b1f4d8e2-6a3c-4f7b-8d9e-2c5a7e0f1b46")
    private UUID clientTransactionId;

    @Schema(description = "Posting instruction that produced the transfer", example = "INBOUND_AUTHORISATION")
    private PostingInstructionType postingInstructionType;

    @Schema(description = "Ledger account debited", example = "4d6b9e1f-8c2a-4b3d-a7e5-0f9c2b8d1a64")
    private UUID debitAccountId;

    @Schema(description = "Ledger account credited", example = "a5c8e2d7-9f1b-4a3c-8e6d-3b7f0c9a2e15")
    private UUID creditAccountId;

    @Schema(description = "Posting amount in major units", example = "250.00")
    private BigDecimal amount;

    @Schema(description = "Posting currency (ISO 4217)", example = "GBP")
    private Currency currency;

    @Schema(description = "Authorisation this transfer settles or releases", example = "e9a3c6b1-2d7f-4e8a-b5c4-8f1d0a3e6b27")
    private UUID pendingTransferId;

    @Schema(description = "When the transfer was recorded", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;

}
