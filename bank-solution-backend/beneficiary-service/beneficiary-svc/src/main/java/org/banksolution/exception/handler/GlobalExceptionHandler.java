package org.banksolution.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.banksolution.exception.BeneficiaryCoordinateNotFoundException;
import org.banksolution.exception.BeneficiaryNotFoundException;
import org.banksolution.exception.BeneficiaryValidationException;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.exception.CustomerServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<@NonNull CustomError> handleMethodArgumentNotValid(MethodArgumentNotValidException methodArgumentNotValidException) {
        List<CustomError.CustomSubError> subErrors = methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldErrorSubError)
                .toList();

        return toCustomErrorResponse(HttpStatus.BAD_REQUEST, CustomError.Header.VALIDATION_ERROR, "Validation failed", subErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<@NonNull CustomError> handleConstraintViolation(ConstraintViolationException constraintViolationException) {
        List<CustomError.CustomSubError> subErrors = constraintViolationException.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toConstraintViolationSubError)
                .toList();

        return toCustomErrorResponse(HttpStatus.BAD_REQUEST, CustomError.Header.VALIDATION_ERROR, "Constraint violation", subErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    protected ResponseEntity<@NonNull CustomError> handleMalformedRequest(Exception malformedRequestException) {
        return toCustomErrorResponse(HttpStatus.BAD_REQUEST, CustomError.Header.VALIDATION_ERROR, malformedRequestException.getMessage(), null);
    }

    @ExceptionHandler(BeneficiaryValidationException.class)
    protected ResponseEntity<@NonNull CustomError> handleBeneficiaryValidation(BeneficiaryValidationException beneficiaryValidationException) {
        List<CustomError.CustomSubError> subErrors = beneficiaryValidationException.getViolations().stream()
                .map(violation -> CustomError.CustomSubError.builder().message(violation).build())
                .toList();

        return toCustomErrorResponse(HttpStatus.BAD_REQUEST, CustomError.Header.VALIDATION_ERROR, "Beneficiary is invalid", subErrors);
    }

    @ExceptionHandler({BeneficiaryNotFoundException.class, BeneficiaryCoordinateNotFoundException.class})
    protected ResponseEntity<@NonNull CustomError> handleNotFound(RuntimeException notFoundException) {
        return toCustomErrorResponse(HttpStatus.NOT_FOUND, CustomError.Header.NOT_FOUND, notFoundException.getMessage(), null);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    protected ResponseEntity<@NonNull CustomError> handleCustomerNotFound(CustomerNotFoundException customerNotFoundException) {
        return toCustomErrorResponse(HttpStatus.UNPROCESSABLE_CONTENT, CustomError.Header.API_ERROR, customerNotFoundException.getMessage(), null);
    }

    @ExceptionHandler(CustomerServiceUnavailableException.class)
    protected ResponseEntity<@NonNull CustomError> handleCustomerServiceUnavailable(
            CustomerServiceUnavailableException customerServiceUnavailableException) {

        return toCustomErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, CustomError.Header.API_ERROR, customerServiceUnavailableException.getMessage(), null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    protected ResponseEntity<@NonNull CustomError> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException objectOptimisticLockingFailureException) {

        return toCustomErrorResponse(HttpStatus.CONFLICT, CustomError.Header.API_ERROR,
                "Beneficiary was modified concurrently, retry with the latest version", null);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<@NonNull CustomError> handleUnexpectedException(Exception unexpectedException) {
        log.error("Unexpected error", unexpectedException);
        return toCustomErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, CustomError.Header.API_ERROR, "Unexpected error", null);
    }

    private static ResponseEntity<@NonNull CustomError> toCustomErrorResponse(
            HttpStatus httpStatus,
            CustomError.Header header,
            String message,
            List<CustomError.CustomSubError> subErrors) {

        CustomError customError = CustomError.builder()
                .httpStatus(httpStatus)
                .header(header.getName())
                .message(message)
                .subErrors(subErrors)
                .build();

        return new ResponseEntity<>(customError, httpStatus);
    }

    private static CustomError.CustomSubError toFieldErrorSubError(FieldError fieldError) {
        return CustomError.CustomSubError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
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
}
