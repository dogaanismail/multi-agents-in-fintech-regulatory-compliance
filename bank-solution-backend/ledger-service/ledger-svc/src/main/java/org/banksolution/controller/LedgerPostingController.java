package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.mapper.LedgerPostingInstructionMapper;
import org.banksolution.mapper.LedgerTransferMapper;
import org.banksolution.model.request.CreateLedgerPostingInstructionRequest;
import org.banksolution.model.response.LedgerPostingResponse;
import org.banksolution.service.LedgerPostingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger/postings")
@Tag(name = "Ledger Postings")
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingController {

    private final LedgerPostingService ledgerPostingService;

    @PostMapping
    @Operation(summary = "Apply a posting instruction",
            description = "Applies exactly one posting instruction: authorisation, settlement, release or hard settlement")
    public ResponseEntity<@NonNull List<LedgerPostingResponse>> createLedgerPostingInstruction(
            @Valid @RequestBody CreateLedgerPostingInstructionRequest request) {
        LedgerPostingInstruction postingInstruction =
                LedgerPostingInstructionMapper.toLedgerPostingInstruction(request);

        log.info("POST /api/v1/ledger/postings - {} for client transaction {}",
                postingInstruction.postingInstructionType(), postingInstruction.clientTransactionId());

        List<LedgerTransfer> ledgerTransfers = ledgerPostingService.applyPostingInstruction(postingInstruction);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ledgerTransfers.stream().map(LedgerTransferMapper::toLedgerPostingResponse).toList());
    }

    @GetMapping("/{clientTransactionId}")
    @Operation(summary = "Retrieve postings",
            description = "Retrieves every ledger posting recorded against a client transaction")
    public ResponseEntity<@NonNull List<LedgerPostingResponse>> getPostingsByClientTransactionId(
            @PathVariable UUID clientTransactionId) {
        log.info("GET /api/v1/ledger/postings/{}", clientTransactionId);

        return ResponseEntity.ok(ledgerPostingService.getPostingsByClientTransactionId(clientTransactionId).stream()
                .map(LedgerTransferMapper::toLedgerPostingResponse)
                .toList());
    }
}
