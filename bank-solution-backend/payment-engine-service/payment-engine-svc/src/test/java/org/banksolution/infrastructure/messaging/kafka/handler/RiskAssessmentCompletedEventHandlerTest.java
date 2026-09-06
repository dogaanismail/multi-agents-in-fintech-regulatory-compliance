package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.CompleteRiskAssessmentCommand;
import org.banksolution.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEvent;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEventWithMarl;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentCompletedEventHandlerTest {

    @Mock
    private CommandGateway commandGateway;

    @InjectMocks
    private RiskAssessmentCompletedEventHandler riskAssessmentCompletedEventHandler;

    @Test
    void shouldRecordTheCompletionOnTheAggregateSoItIsPartOfThePaymentStream() {
        riskAssessmentCompletedEventHandler.handle(
                createRiskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10));

        CompleteRiskAssessmentCommand completeRiskAssessmentCommand = captureDispatchedCommand();
        assertThat(completeRiskAssessmentCommand.paymentId()).isEqualTo(createPaymentId());
        assertThat(completeRiskAssessmentCommand.riskAssessment().riskAction()).isEqualTo("PROCEED");
        assertThat(completeRiskAssessmentCommand.riskAssessment().riskLevel()).isEqualTo("LOW");
        assertThat(completeRiskAssessmentCommand.riskAssessment().riskScore()).isEqualTo(0.10);
        assertThat(completeRiskAssessmentCommand.riskAssessment().marlAssessment()).isNull();
    }

    @Test
    void shouldCarryTheMarlAssessmentWhenTheOrchestratorContributed() {
        riskAssessmentCompletedEventHandler.handle(createRiskAssessmentCompletedEventWithMarl(RiskAction.BLOCK));

        CompleteRiskAssessmentCommand completeRiskAssessmentCommand = captureDispatchedCommand();
        assertThat(completeRiskAssessmentCommand.riskAssessment().marlAssessment().action()).isEqualTo("BLOCK");
        assertThat(completeRiskAssessmentCommand.riskAssessment().marlAssessment().maddpgQValue()).isEqualTo(0.42);
    }

    @Test
    void shouldFailTheKafkaRecordWhenTheAggregateRejectsTheCompletion() {
        InvalidPaymentStateException rejection = new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        when(commandGateway.sendAndWait(any())).thenThrow(rejection);

        assertThatThrownBy(() -> riskAssessmentCompletedEventHandler.handle(
                createRiskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10)))
                .isSameAs(rejection);
    }

    private CompleteRiskAssessmentCommand captureDispatchedCommand() {
        ArgumentCaptor<Object> dispatchedCommandCaptor = ArgumentCaptor.forClass(Object.class);
        verify(commandGateway).sendAndWait(dispatchedCommandCaptor.capture());

        assertThat(dispatchedCommandCaptor.getValue()).isInstanceOf(CompleteRiskAssessmentCommand.class);

        return (CompleteRiskAssessmentCommand) dispatchedCommandCaptor.getValue();
    }
}
