package org.banksolution.domain;

import org.banksolution.model.request.PaymentRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public record PaymentIdempotency(UUID paymentId, String idempotencyKey, String requestFingerprint) {

    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

    private static final String FINGERPRINT_FIELD_SEPARATOR = "|";

    public static PaymentIdempotency derivePaymentIdempotency(
            String idempotencyKey,
            PaymentRequest paymentRequest) {

        requireValidIdempotencyKey(idempotencyKey);
        return new PaymentIdempotency(
                derivePaymentId(paymentRequest.getCustomerId(), idempotencyKey),
                idempotencyKey,
                derivePaymentRequestFingerprint(paymentRequest));
    }

    public static UUID derivePaymentId(UUID customerId, String idempotencyKey) {
        String seed = "payment:" + customerId + ":" + idempotencyKey;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    public static String derivePaymentRequestFingerprint(PaymentRequest paymentRequest) {
        String canonicalPaymentRequest = String.join(FINGERPRINT_FIELD_SEPARATOR, Stream.of(
                        paymentRequest.getCustomerId(),
                        paymentRequest.getSourceAccountId(),
                        paymentRequest.getDestinationAccountId(),
                        toCanonicalAmount(paymentRequest.getAmount()),
                        paymentRequest.getFromCurrency(),
                        paymentRequest.getToCurrency(),
                        paymentRequest.getPaymentType(),
                        paymentRequest.getFixedSide(),
                        paymentRequest.getDescription())
                .map(fingerprintField -> Objects.toString(fingerprintField, ""))
                .toList());

        return HexFormat.of().formatHex(sha256(canonicalPaymentRequest));
    }

    private static void requireValidIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be between 1 and " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
    }

    private static String toCanonicalAmount(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros().toPlainString();
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }
}
