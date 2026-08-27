package org.banksolution.exception.handler;

import org.banksolution.exception.CustomError;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldTurnAMalformedPathOrQueryValueIntoABadRequest() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleArgumentTypeMismatch(
                new MethodArgumentTypeMismatchException("not-a-uuid", UUID.class, "paymentId", mock(MethodParameter.class), null));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(CustomError.Header.VALIDATION_ERROR.getName());
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Invalid value for paymentId: not-a-uuid");
    }

    @Test
    void shouldTurnAMissingQueryParameterIntoABadRequest() {
        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleMissingParameter(
                new MissingServletRequestParameterException("startDate", "Instant"));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getMessage()).isEqualTo("Missing request parameter: startDate");
    }
}
