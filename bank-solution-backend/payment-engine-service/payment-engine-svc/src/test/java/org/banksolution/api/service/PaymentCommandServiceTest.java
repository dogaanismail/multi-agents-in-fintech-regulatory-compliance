package org.banksolution.api.service;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.api.dto.ApproveManualReviewRequest;
import org.banksolution.api.dto.InitiatePaymentRequest;
import org.banksolution.api.dto.InitiatePaymentResponse;
import org.banksolution.api.dto.ManualReviewResponse;
import org.banksolution.api.dto.OverrideDecisionRequest;
import org.banksolution.api.dto.OverrideDecisionResponse;
import org.banksolution.api.dto.RejectManualReviewRequest;
import org.banksolution.domain.payment.command.ApproveManualReviewCommand;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.command.OverrideDecisionCommand;
import org.banksolution.domain.payment.command.RejectManualReviewCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    @Mock
    private CommandGateway commandGateway;

    @InjectMocks
    private PaymentCommandService paymentCommandService;

    @Test
    void shouldInitiateAnOutboundPaymentWithTheAmountAsConvertedAmount() {
        InitiatePaymentRequest initiatePaymentRequest = new InitiatePaymentRequest(
                PAYMENT_UUID,
                CUSTOMER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                null,
                PAYMENT_TYPE,
                true,
                DESCRIPTION);

        InitiatePaymentResponse initiatePaymentResponse = paymentCommandService.initiatePayment(initiatePaymentRequest);

        ArgumentCaptor<InitiatePaymentCommand> initiatePaymentCommandCaptor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        verify(commandGateway).sendAndWait(initiatePaymentCommandCaptor.capture());
        InitiatePaymentCommand initiatePaymentCommand = initiatePaymentCommandCaptor.getValue();
        assertThat(initiatePaymentCommand.paymentId()).isEqualTo(createPaymentId());
        assertThat(initiatePaymentCommand.toCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(initiatePaymentCommand.convertedAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(initiatePaymentCommand.appliedExchangeRate()).isNull();
        assertThat(initiatePaymentCommand.paymentScheme()).isEqualTo("EXTERNAL_OUTBOUND");
        assertThat(initiatePaymentCommand.fixedSide()).isEqualTo("SELL");
        assertThat(initiatePaymentCommand.isCrossBorderPayment()).isTrue();
        assertThat(initiatePaymentResponse.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(initiatePaymentResponse.getMessage()).isEqualTo("Payment initiated successfully");
    }

    @Test
    void shouldGenerateAPaymentIdAndKeepTheTargetCurrencyWhenGiven() {
        InitiatePaymentRequest initiatePaymentRequest = new InitiatePaymentRequest(
                null, CUSTOMER_ID, SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID,
                AMOUNT, FROM_CURRENCY, "EUR", PAYMENT_TYPE, false, DESCRIPTION);

        InitiatePaymentResponse initiatePaymentResponse = paymentCommandService.initiatePayment(initiatePaymentRequest);

        ArgumentCaptor<InitiatePaymentCommand> initiatePaymentCommandCaptor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        verify(commandGateway).sendAndWait(initiatePaymentCommandCaptor.capture());
        assertThat(initiatePaymentCommandCaptor.getValue().toCurrency()).isEqualTo("EUR");
        assertThat(initiatePaymentCommandCaptor.getValue().paymentId().getIdentifier()).isNotNull();
        assertThat(initiatePaymentResponse.getPaymentId())
                .isEqualTo(initiatePaymentCommandCaptor.getValue().paymentId().toString());
    }

    @Test
    void shouldApproveTheManualReviewOfThePaymentInThePath() {
        ApproveManualReviewRequest approveManualReviewRequest =
                new ApproveManualReviewRequest(UUID.randomUUID(), OFFICER, APPROVAL_NOTES);

        ManualReviewResponse manualReviewResponse =
                paymentCommandService.approveManualReview(PAYMENT_UUID.toString(), approveManualReviewRequest);

        verify(commandGateway).sendAndWait(new ApproveManualReviewCommand(createPaymentId(), OFFICER, APPROVAL_NOTES));
        assertThat(manualReviewResponse.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(manualReviewResponse.getReviewedBy()).isEqualTo(OFFICER);
        assertThat(manualReviewResponse.getMessage()).contains("approved");
    }

    @Test
    void shouldRejectTheManualReviewOfThePaymentInThePath() {
        RejectManualReviewRequest rejectManualReviewRequest =
                new RejectManualReviewRequest(null, OFFICER, REJECTION_REASON);

        ManualReviewResponse manualReviewResponse =
                paymentCommandService.rejectManualReview(PAYMENT_UUID.toString(), rejectManualReviewRequest);

        verify(commandGateway).sendAndWait(new RejectManualReviewCommand(createPaymentId(), OFFICER, REJECTION_REASON));
        assertThat(manualReviewResponse.getMessage()).contains(REJECTION_REASON);
        assertThat(manualReviewResponse.getReviewedBy()).isEqualTo(OFFICER);
    }

    @Test
    void shouldOverrideTheDecisionAndReportTheResultingStatus() {
        OverrideDecisionResponse approvedResponse = paymentCommandService.overrideDecision(
                PAYMENT_UUID.toString(), new OverrideDecisionRequest(null, OFFICER, OVERRIDE_REASON, true));
        OverrideDecisionResponse rejectedResponse = paymentCommandService.overrideDecision(
                PAYMENT_UUID.toString(), new OverrideDecisionRequest(null, OFFICER, OVERRIDE_REASON, false));

        verify(commandGateway).sendAndWait(new OverrideDecisionCommand(createPaymentId(), OFFICER, OVERRIDE_REASON, true));
        verify(commandGateway).sendAndWait(new OverrideDecisionCommand(createPaymentId(), OFFICER, OVERRIDE_REASON, false));
        assertThat(approvedResponse.getNewStatus()).isEqualTo("OVERRIDE_APPROVED");
        assertThat(rejectedResponse.getNewStatus()).isEqualTo("OVERRIDE_REJECTED");
        assertThat(approvedResponse.getOverriddenBy()).isEqualTo(OFFICER);
        assertThat(approvedResponse.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
    }
}
