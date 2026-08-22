package org.banksolution.controller;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.Currency;
import org.banksolution.service.LedgerAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.banksolution.enums.PostingInstructionType.OUTBOUND_AUTHORISATION;
import static org.banksolution.fixtures.LedgerPostingFixtures.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerPostingControllerTest extends BaseIntegrationTest {

    private static final String POSTINGS_URL = "/api/v1/ledger/postings";
    private static final String BY_CLIENT_TRANSACTION_URL = POSTINGS_URL + "/{clientTransactionId}";

    private static final Currency CURRENCY = Currency.GBP;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal AUTHORISED_AMOUNT = new BigDecimal("250.00");

    private static final String POSTING_INSTRUCTION_TYPE = "$.postingInstructionType";
    private static final String CLIENT_TRANSACTION_ID = "$.clientTransactionId";
    private static final String HEADER = "$.header";
    private static final String VALIDATION_ERROR = "VALIDATION ERROR";

    @Autowired
    private LedgerAccountService ledgerAccountService;

    @Test
    void shouldApplyOutboundAuthorisation() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOutboundAuthorisation(
                                clientTransactionId, customerAccountId, AUTHORISED_AMOUNT, CURRENCY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(POSTING_INSTRUCTION_TYPE).value(OUTBOUND_AUTHORISATION.name()))
                .andExpect(jsonPath(CLIENT_TRANSACTION_ID).value(clientTransactionId.toString()))
                .andExpect(jsonPath("$.transferId")
                        .value(LedgerTransferIds.deriveTransferId(clientTransactionId, OUTBOUND_AUTHORISATION)
                                .toString()))
                .andExpect(jsonPath("$.amount").value(250.00));
    }

    @Test
    void shouldApplyTheFullAuthoriseThenSettleLifecycle() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        applyPosting(createOutboundAuthorisation(
                clientTransactionId, customerAccountId, AUTHORISED_AMOUNT, CURRENCY));

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSettlement(clientTransactionId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(POSTING_INSTRUCTION_TYPE).value("SETTLEMENT"))
                .andExpect(jsonPath("$.pendingTransferId")
                        .value(LedgerTransferIds.deriveTransferId(clientTransactionId, OUTBOUND_AUTHORISATION)
                                .toString()));
    }

    @Test
    void shouldApplyTheFullAuthoriseThenReleaseLifecycle() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        applyPosting(createOutboundAuthorisation(
                clientTransactionId, customerAccountId, AUTHORISED_AMOUNT, CURRENCY));

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRelease(clientTransactionId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(POSTING_INSTRUCTION_TYPE).value("RELEASE"));
    }

    @Test
    void shouldApplyInboundAuthorisation() throws Exception {
        UUID customerAccountId = givenFundedWallet();

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInboundAuthorisation(
                                UUID.randomUUID(), customerAccountId, AUTHORISED_AMOUNT, CURRENCY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(POSTING_INSTRUCTION_TYPE).value("INBOUND_AUTHORISATION"));
    }

    @Test
    void shouldApplyOutboundHardSettlement() throws Exception {
        UUID customerAccountId = givenFundedWallet();

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOutboundHardSettlement(
                                UUID.randomUUID(), customerAccountId, AUTHORISED_AMOUNT, CURRENCY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(POSTING_INSTRUCTION_TYPE).value("OUTBOUND_HARD_SETTLEMENT"));
    }

    @Test
    void shouldListEveryPostingForAClientTransaction() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        applyPosting(createOutboundAuthorisation(
                clientTransactionId, customerAccountId, AUTHORISED_AMOUNT, CURRENCY));
        applyPosting(createSettlement(clientTransactionId));

        mockMvc.perform(get(BY_CLIENT_TRANSACTION_URL, clientTransactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnEmptyListForAnUnknownClientTransaction() throws Exception {
        mockMvc.perform(get(BY_CLIENT_TRANSACTION_URL, UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectSettlementWithoutAnAuthorisation() throws Exception {
        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSettlement(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(HEADER).value("NOT EXIST"));
    }

    @Test
    void shouldRejectAuthorisationThatExceedsAvailableFunds() throws Exception {
        UUID customerAccountId = givenFundedWallet();

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOutboundAuthorisation(
                                UUID.randomUUID(), customerAccountId, new BigDecimal("100000.00"), CURRENCY))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath(HEADER).value("PROCESS ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutAnyPostingInstruction() throws Exception {
        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content("{\"clientTransactionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(HEADER).value(VALIDATION_ERROR));
    }

    @Test
    void shouldRejectRequestWithTwoPostingInstructions() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        var request = createOutboundAuthorisation(
                clientTransactionId, customerAccountId, AUTHORISED_AMOUNT, CURRENCY);
        request.setSettlement(new org.banksolution.model.request
                .CreateLedgerPostingInstructionRequest.SettlementRequest());

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(HEADER).value(VALIDATION_ERROR));
    }

    @Test
    void shouldRejectNegativeAmount() throws Exception {
        UUID customerAccountId = givenFundedWallet();

        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOutboundAuthorisation(
                                UUID.randomUUID(), customerAccountId, new BigDecimal("-1.00"), CURRENCY))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(HEADER).value(VALIDATION_ERROR));
    }

    private UUID givenFundedWallet() throws Exception {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, CURRENCY);
        applyPosting(createInboundHardSettlement(
                UUID.randomUUID(), customerAccountId, OPENING_BALANCE, CURRENCY));
        return customerAccountId;
    }

    private void applyPosting(Object request) throws Exception {
        mockMvc.perform(post(POSTINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
