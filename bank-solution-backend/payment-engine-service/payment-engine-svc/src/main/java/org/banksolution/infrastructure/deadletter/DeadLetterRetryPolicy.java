package org.banksolution.infrastructure.deadletter;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.deadletter.DeadLetter;
import org.axonframework.messaging.deadletter.Decisions;
import org.axonframework.messaging.deadletter.EnqueueDecision;
import org.axonframework.messaging.deadletter.EnqueuePolicy;

public class DeadLetterRetryPolicy implements EnqueuePolicy<EventMessage<?>> {

    public static final String RETRIES_DIAGNOSTIC = "retries";

    @Override
    public EnqueueDecision<EventMessage<?>> decide(DeadLetter<? extends EventMessage<?>> deadLetter, Throwable cause) {
        return Decisions.enqueue(cause, letter -> letter.diagnostics().and(RETRIES_DIAGNOSTIC, nextRetryCount(letter)));
    }

    public static int retriesOf(DeadLetter<? extends EventMessage<?>> deadLetter) {
        Object retries = deadLetter.diagnostics().get(RETRIES_DIAGNOSTIC);
        return retries instanceof Number number ? number.intValue() : 0;
    }

    private static int nextRetryCount(DeadLetter<? extends EventMessage<?>> deadLetter) {
        MetaData diagnostics = deadLetter.diagnostics();
        return diagnostics.containsKey(RETRIES_DIAGNOSTIC) ? retriesOf(deadLetter) + 1 : 0;
    }
}
