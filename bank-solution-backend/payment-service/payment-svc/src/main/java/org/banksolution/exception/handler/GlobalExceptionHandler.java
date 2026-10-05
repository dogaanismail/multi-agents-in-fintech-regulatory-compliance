package org.banksolution.exception.handler;

import org.springframework.security.access.AccessDeniedException;
import org.banksolution.servicesecurity.RequiresPermission;
import org.banksolution.servicesecurity.Permissions;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.ExchangeRateUnavailableException;
import org.banksolution.exception.IdempotencyKeyReusedException;
import org.banksolution.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<@NonNull CustomError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        List<CustomError.CustomSubError> subErrors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(
                error -> {
                    String fieldName = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    subErrors.add(
                            CustomError.CustomSubError.builder()
                                    .field(fieldName)
                                    .message(message)
                                    .build()
                    );
                }
        );

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Validation failed")
                .subErrors(subErrors)
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<@NonNull CustomError> handlePathVariableErrors(ConstraintViolationException constraintViolationException) {

        List<CustomError.CustomSubError> subErrors = new ArrayList<>();
        constraintViolationException.getConstraintViolations()
                .forEach(constraintViolation -> subErrors.add(toConstraintViolationSubError(constraintViolation)));

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Constraint violation")
                .subErrors(subErrors)
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    private static CustomError.CustomSubError toConstraintViolationSubError(ConstraintViolation<?> constraintViolation) {
        Object invalidValue = constraintViolation.getInvalidValue();

        return CustomError.CustomSubError.builder()
                .message(constraintViolation.getMessage())
                .field(StringUtils.substringAfterLast(constraintViolation.getPropertyPath().toString(), "."))
                .value(invalidValue != null ? invalidValue.toString() : null)
                .type(invalidValue != null ? invalidValue.getClass().getSimpleName() : null)
                .build();
    }

    /**
     * Account-presence and payment-scheme checks throw IllegalArgumentException; without this
     * handler they fell through to the RuntimeException catch-all and surfaced as 404.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<@NonNull CustomError> handleIllegalArgument(IllegalArgumentException ex) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    protected ResponseEntity<@NonNull CustomError> handleMissingRequestHeader(MissingRequestHeaderException missingRequestHeaderException) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message(missingRequestHeaderException.getHeaderName() + " header is required")
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    protected ResponseEntity<@NonNull CustomError> handleIdempotencyKeyReused(IdempotencyKeyReusedException idempotencyKeyReusedException) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .header(CustomError.Header.PROCESS_ERROR.getName())
                .message(idempotencyKeyReusedException.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    protected ResponseEntity<@NonNull CustomError> handleExchangeRateUnavailable(ExchangeRateUnavailableException ex) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .header(CustomError.Header.PROCESS_ERROR.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<@NonNull CustomError> handleHttpMessageNotReadable(HttpMessageNotReadableException httpMessageNotReadableException) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Malformed request body")
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<@NonNull CustomError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Invalid value for " + methodArgumentTypeMismatchException.getName())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<@NonNull CustomError> handleResponseStatusException(ResponseStatusException responseStatusException) {

        HttpStatus httpStatus = HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        CustomError customError = CustomError.builder()
                .httpStatus(httpStatus)
                .header(CustomError.Header.API_ERROR.getName())
                .message(responseStatusException.getReason() != null ? responseStatusException.getReason() : httpStatus.getReasonPhrase())
                .build();

        return new ResponseEntity<>(customError, httpStatus);
    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<@NonNull CustomError> handleRuntimeException(RuntimeException runtimeException) {
        log.error("Unexpected error", runtimeException);

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CustomError.Header.API_ERROR.getName())
                .message("Unexpected error")
                .build();

        return new ResponseEntity<>(customError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    protected ResponseEntity<@NonNull CustomError> handlePaymentNotFoundException(PaymentNotFoundException ex) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.NOT_FOUND)
                .header(CustomError.Header.API_ERROR.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<@NonNull CustomError> handleAccessDenied(AccessDeniedException accessDeniedException) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.FORBIDDEN)
                .header(CustomError.Header.AUTH_ERROR.getName())
                .message("Access denied")
                .build();

        return new ResponseEntity<>(customError, HttpStatus.FORBIDDEN);
    }
}
