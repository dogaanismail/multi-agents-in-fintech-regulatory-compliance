package org.banksolution.outbox.relay;

import java.util.Set;

import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.banksolution.outbox.enums.OutboxEventFailType;

public final class OutboxDeliveryFailureClassifier {

    private static final Set<Class<? extends Throwable>> PERMANENT_FAILURES = Set.of(
            RecordTooLargeException.class,
            InvalidTopicException.class,
            SerializationException.class,
            AuthorizationException.class,
            AuthenticationException.class,
            UnsupportedVersionException.class);

    private OutboxDeliveryFailureClassifier() {
    }

    public static OutboxEventFailType classifyOutboxDeliveryFailure(Throwable deliveryFailure) {
        for (Throwable cause = deliveryFailure; cause != null; cause = cause.getCause()) {
            Throwable currentCause = cause;
            if (PERMANENT_FAILURES.stream().anyMatch(permanentFailure -> permanentFailure.isInstance(currentCause))) {
                return OutboxEventFailType.PERMANENT;
            }

            if (cause.getCause() == cause) {
                break;
            }
        }

        return OutboxEventFailType.TRANSIENT;
    }
}
