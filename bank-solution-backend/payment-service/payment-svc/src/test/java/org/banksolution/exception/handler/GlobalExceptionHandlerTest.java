package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PaymentType;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.ExchangeRateUnavailableException;
import org.banksolution.exception.PaymentNotFoundException;
import org.banksolution.exception.UnresolvablePaymentSchemeException;
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

    private static final String PROPERTY_PATH = "getPaymentsByCustomerId.customerId";

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldTurnFieldValidationFailuresIntoABadRequestWithSubErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "paymentRequest");
        bindingResult.addError(new FieldError("paymentRequest", "amount", "Amount must be greater than 0"));

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getField, CustomError.CustomSubError::getMessage)
                .containsExactly(tuple("amount", "Amount must be greater than 0"));
    }

    @Test
    void shouldTurnConstraintViolationsIntoABadRequestWithSubErrors() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePathVariableErrors(
                new ConstraintViolationException(Set.of(createConstraintViolation("not-a-uuid"))));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(
                        CustomError.CustomSubError::getField,
                        CustomError.CustomSubError::getValue,
                        CustomError.CustomSubError::getType)
                .containsExactly(tuple("customerId", "not-a-uuid", "String"));
    }

    @Test
    void shouldReportAConstraintViolationWithoutAValueInsteadOfFailingOnIt() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePathVariableErrors(
                new ConstraintViolationException(Set.of(createConstraintViolation(null))));

        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getValue, CustomError.CustomSubError::getType)
                .containsExactly(tuple(null, null));
    }

    @Test
    void shouldTurnARejectedPaymentRequestIntoABadRequestNotANotFound() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleIllegalArgument(
                new UnresolvablePaymentSchemeException(PaymentType.DEPOSIT));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.VALIDATION_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).contains("DEPOSIT");
    }

    @Test
    void shouldTurnAMissingExchangeRateIntoAnUnprocessableEntity() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleExchangeRateUnavailable(
                new ExchangeRateUnavailableException(Currency.GBP, Currency.JPY));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.PROCESS_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("No exchange rate available for GBP to JPY");
    }

    @Test
    void shouldTurnAMissingPaymentIntoANotFound() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePaymentNotFoundException(
                new PaymentNotFoundException("PAY-1"));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Payment not found with reference number: PAY-1");
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
