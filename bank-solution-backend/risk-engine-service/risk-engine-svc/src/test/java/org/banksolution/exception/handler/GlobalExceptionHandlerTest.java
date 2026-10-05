package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.banksolution.exception.CustomError;
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
}
