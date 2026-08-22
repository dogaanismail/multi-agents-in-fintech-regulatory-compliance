package org.banksolution.model.response;

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
public class LedgerPostingResponse {

    private UUID transferId;
    private UUID clientTransactionId;
    private PostingInstructionType postingInstructionType;
    private UUID debitAccountId;
    private UUID creditAccountId;
    private BigDecimal amount;
    private Currency currency;
    private UUID pendingTransferId;
    private Instant createdAt;

}
