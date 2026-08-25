package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

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
    void shouldTurnAnyOtherRuntimeExceptionIntoANotFoundError() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleRuntimeException(
                new IllegalStateException("unexpected failure"));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("unexpected failure");
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
