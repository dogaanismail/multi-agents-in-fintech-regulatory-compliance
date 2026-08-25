package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.banksolution.enums.Currency;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.exception.AccountNumberGenerationException;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.exception.WalletCreationFailedException;
import org.banksolution.exception.WalletNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final String PROPERTY_PATH = "getAccountById.id";

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldTurnFieldValidationFailuresIntoABadRequestWithSubErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "openAccountRequest");
        bindingResult.addError(new FieldError("openAccountRequest", "customerId", "Customer ID can't be null."));
        MethodArgumentNotValidException methodArgumentNotValidException =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<CustomError> customErrorResponse =
                globalExceptionHandler.handleMethodArgumentNotValid(methodArgumentNotValidException);

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getField, CustomError.CustomSubError::getMessage)
                .containsExactly(tuple("customerId", "Customer ID can't be null."));
    }

    @Test
    void shouldTurnConstraintViolationsIntoABadRequestWithSubErrors() {
        ConstraintViolation<Object> constraintViolation = createConstraintViolation("not-a-uuid");

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePathVariableErrors(
                new ConstraintViolationException(Set.of(constraintViolation)));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Constraint violation");
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(
                        CustomError.CustomSubError::getField,
                        CustomError.CustomSubError::getValue,
                        CustomError.CustomSubError::getType)
                .containsExactly(tuple("id", "not-a-uuid", "String"));
    }

    @Test
    void shouldReportAConstraintViolationWithoutAValueInsteadOfFailingOnIt() {
        ConstraintViolation<Object> constraintViolation = createConstraintViolation(null);

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handlePathVariableErrors(
                new ConstraintViolationException(Set.of(constraintViolation)));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(
                        CustomError.CustomSubError::getField,
                        CustomError.CustomSubError::getValue,
                        CustomError.CustomSubError::getType)
                .containsExactly(tuple("id", null, null));
    }

    @Test
    void shouldTurnAnAccountNotFoundExceptionIntoANotFoundError() {
        UUID unknownAccountId = UUID.randomUUID();

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleAccountNotFoundException(
                new AccountNotFoundException(unknownAccountId));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.API_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).contains(unknownAccountId.toString());
    }

    @Test
    void shouldTurnAWalletNotFoundExceptionIntoANotFoundError() {
        UUID accountId = UUID.randomUUID();

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleWalletNotFoundException(
                new WalletNotFoundException(accountId, Currency.JPY));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.NOT_FOUND.getName());
        assertThat(customErrorResponse.getBody().getMessage())
                .contains(accountId.toString())
                .contains(Currency.JPY.name());
    }

    @Test
    void shouldTurnACustomerNotFoundExceptionIntoANotFoundError() {
        UUID unknownCustomerId = UUID.randomUUID();

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleCustomerNotFoundException(
                new CustomerNotFoundException(unknownCustomerId));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.NOT_FOUND.getName());
        assertThat(customErrorResponse.getBody().getMessage()).contains(unknownCustomerId.toString());
    }

    @Test
    void shouldTurnAWalletCreationFailureIntoAServiceUnavailableError() {
        UUID accountId = UUID.randomUUID();

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleWalletCreationFailedException(
                new WalletCreationFailedException(accountId, new IllegalStateException("ledger down")));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.PROCESS_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).contains(accountId.toString());
    }

    @Test
    void shouldTurnAnAccountNumberGenerationFailureIntoAnInternalServerError() {
        ResponseEntity<CustomError> customErrorResponse =
                globalExceptionHandler.handleAccountNumberGenerationException(new AccountNumberGenerationException(5));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.PROCESS_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).contains("5 attempts");
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
