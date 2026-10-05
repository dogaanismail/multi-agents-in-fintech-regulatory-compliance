package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.InvalidPaymentStateException;
import org.banksolution.exception.PaymentNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final String PROPERTY_PATH = "approveManualReview.paymentId";

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldTurnFieldValidationFailuresIntoABadRequestWithSubErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "initiatePaymentRequest");
        bindingResult.addError(new FieldError("initiatePaymentRequest", "amount", "must be positive"));
        MethodArgumentNotValidException methodArgumentNotValidException =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<CustomError> customErrorResponse =
                globalExceptionHandler.handleMethodArgumentNotValid(methodArgumentNotValidException);

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getField, CustomError.CustomSubError::getMessage)
                .containsExactly(tuple("amount", "must be positive"));
    }

    @Test
    void shouldTurnConstraintViolationsIntoABadRequestWithSubErrors() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePathVariableErrors(
                new ConstraintViolationException(Set.of(createConstraintViolation("not-a-uuid"))));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Constraint violation");
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(
                        CustomError.CustomSubError::getField,
                        CustomError.CustomSubError::getValue,
                        CustomError.CustomSubError::getType)
                .containsExactly(tuple("paymentId", "not-a-uuid", "String"));
    }

    @Test
    void shouldReportAConstraintViolationWithoutAValueInsteadOfFailingOnIt() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePathVariableErrors(
                new ConstraintViolationException(Set.of(createConstraintViolation(null))));

        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(
                        CustomError.CustomSubError::getField,
                        CustomError.CustomSubError::getValue,
                        CustomError.CustomSubError::getType)
                .containsExactly(tuple("paymentId", null, null));
    }

    @Test
    void shouldTurnAnInvalidPaymentStateIntoAConsistentConflict() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleInvalidPaymentStateException(
                new InvalidPaymentStateException("Payment is not in MANUAL_REVIEW_REQUIRED status"));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.API_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Payment is not in MANUAL_REVIEW_REQUIRED status");
    }

    @Test
    void shouldHideAnUnexpectedErrorBehindAGenericInternalServerError() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleRuntimeException(
                new IllegalStateException("connection to db-password-secret failed"));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.API_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Unexpected error");
    }

    @Test
    void shouldTurnAMalformedRequestBodyIntoABadRequest() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("JSON parse error", mock(HttpInputMessage.class)));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Malformed request body");
    }

    @Test
    void shouldTurnAnInvalidPathValueIntoABadRequestNamingTheParameter() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleMethodArgumentTypeMismatch(
                new MethodArgumentTypeMismatchException("not-a-uuid", UUID.class, "paymentId", mock(MethodParameter.class), null));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Invalid value for paymentId");
    }

    @Test
    void shouldKeepTheStatusAndReasonOfAResponseStatusException() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.CONFLICT, "Already processing"));
        ResponseEntity<CustomError> customErrorResponseWithoutReason = globalExceptionHandler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Already processing");
        assertThat(customErrorResponseWithoutReason.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assert customErrorResponseWithoutReason.getBody() != null;
        assertThat(customErrorResponseWithoutReason.getBody().getMessage()).isEqualTo("Service Unavailable");
    }

    @Test
    void shouldTurnACommandForAnUnknownPaymentIntoANotFound() {
        String unknownPaymentId = UUID.randomUUID().toString();

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleAggregateNotFound(
                new AggregateNotFoundException(unknownPaymentId, "The aggregate was not found in the event store"));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Payment not found: " + unknownPaymentId);
    }

    @Test
    void shouldTurnAMissingPaymentIntoANotFound() {
        String unknownPaymentId = UUID.randomUUID().toString();

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePaymentNotFound(
                new PaymentNotFoundException("Failed to load for paymentId: %s", null, unknownPaymentId));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).contains(unknownPaymentId);
    }

    private static ConstraintViolation<Object> createConstraintViolation(Object invalidValue) {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> constraintViolation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn(PROPERTY_PATH);
        when(constraintViolation.getPropertyPath()).thenReturn(propertyPath);
        when(constraintViolation.getMessage()).thenReturn("must not be null");
        when(constraintViolation.getInvalidValue()).thenReturn(invalidValue);
        return constraintViolation;
    }
}
