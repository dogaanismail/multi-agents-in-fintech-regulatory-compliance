package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.RiskCheckRequestNotFoundException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldTurnFieldValidationFailuresIntoABadRequestWithSubErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must be positive"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<CustomError> response = globalExceptionHandler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert response.getBody() != null;
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getField)
                .containsExactly("amount");
    }

    @Test
    void shouldTurnConstraintViolationsIntoABadRequestWithSubErrors() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> constraintViolation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("getRiskCheck.paymentId");
        when(constraintViolation.getPropertyPath()).thenReturn(propertyPath);
        when(constraintViolation.getMessage()).thenReturn("must not be blank");
        when(constraintViolation.getInvalidValue()).thenReturn(" ");

        ResponseEntity<CustomError> response = globalExceptionHandler.handlePathVariableErrors(
                new ConstraintViolationException(Set.of(constraintViolation)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert response.getBody() != null;
        assertThat(response.getBody().getMessage()).isEqualTo("Constraint violation");
        assertThat(response.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getField)
                .containsExactly("paymentId");
    }

    @Test
    void shouldTurnARuntimeExceptionIntoANotFoundError() {
        UUID unknownId = UUID.randomUUID();

        ResponseEntity<CustomError> response = globalExceptionHandler.handleRuntimeException(
                new RiskCheckRequestNotFoundException(unknownId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assert response.getBody() != null;
        assertThat(response.getBody().getMessage()).contains(unknownId.toString());
    }
}
