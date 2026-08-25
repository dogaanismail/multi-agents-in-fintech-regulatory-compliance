package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.CustomerAlreadyExistsException;
import org.banksolution.exception.CustomerNotFoundException;
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

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldTurnFieldValidationFailuresIntoABadRequestWithSubErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "customerCreateRequest");
        bindingResult.addError(new FieldError("customerCreateRequest", "email", "Please enter valid e-mail address"));
        MethodArgumentNotValidException methodArgumentNotValidException =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<CustomError> customErrorResponse =
                globalExceptionHandler.handleMethodArgumentNotValid(methodArgumentNotValidException);

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getField)
                .containsExactly("email");
    }

    @Test
    void shouldTurnConstraintViolationsIntoABadRequestWithSubErrors() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> constraintViolation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("getCustomerById.id");
        when(constraintViolation.getPropertyPath()).thenReturn(propertyPath);
        when(constraintViolation.getMessage()).thenReturn("must not be null");
        when(constraintViolation.getInvalidValue()).thenReturn("not-a-uuid");

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
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> constraintViolation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("getCustomerById.id");
        when(constraintViolation.getPropertyPath()).thenReturn(propertyPath);
        when(constraintViolation.getMessage()).thenReturn("must not be null");
        when(constraintViolation.getInvalidValue()).thenReturn(null);

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
    void shouldTurnACustomerNotFoundExceptionIntoANotFoundError() {
        UUID unknownCustomerId = UUID.randomUUID();

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleCustomerNotFoundException(
                new CustomerNotFoundException(unknownCustomerId));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).contains(unknownCustomerId.toString());
    }

    @Test
    void shouldTurnACustomerAlreadyExistsExceptionIntoAConflictError() {
        String takenEmail = "alice@example.com";

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleCustomerAlreadyExistException(
                new CustomerAlreadyExistsException(takenEmail));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).contains(takenEmail);
    }

    @Test
    void shouldTurnAnyOtherRuntimeExceptionIntoANotFoundError() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleRuntimeException(
                new IllegalStateException("unexpected failure"));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("unexpected failure");
    }
}
