package org.banksolution.exception.handler;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.OnboardingRequiredException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Set<Integer> FORWARDED_DOWNSTREAM_STATUSES = Set.of(400, 404, 409, 422);

    @ExceptionHandler(OnboardingRequiredException.class)
    protected ResponseEntity<@NonNull CustomError> handleOnboardingRequired(OnboardingRequiredException onboardingRequiredException) {
        return toCustomErrorResponse(HttpStatus.FORBIDDEN, CustomError.Header.PROCESS_ERROR, onboardingRequiredException.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    protected ResponseEntity<@NonNull CustomError> handleAccountNotFound(AccountNotFoundException accountNotFoundException) {
        return toCustomErrorResponse(HttpStatus.NOT_FOUND, CustomError.Header.NOT_FOUND, accountNotFoundException.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    protected ResponseEntity<@NonNull CustomError> handleInvalidRequest(Exception invalidRequestException) {
        return toCustomErrorResponse(HttpStatus.BAD_REQUEST, CustomError.Header.VALIDATION_ERROR, "Invalid request");
    }

    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<@NonNull CustomError> handleResponseStatus(ResponseStatusException responseStatusException) {
        HttpStatus httpStatus = HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        return toCustomErrorResponse(httpStatus, CustomError.Header.API_ERROR, responseStatusException.getReason());
    }

    @ExceptionHandler(FeignException.class)
    protected ResponseEntity<@NonNull String> handleDownstreamFailure(FeignException feignException) {
        if (FORWARDED_DOWNSTREAM_STATUSES.contains(feignException.status())) {
            return ResponseEntity.status(HttpStatusCode.valueOf(feignException.status()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(feignException.contentUTF8());
        }
        log.error("Downstream call failed with status {}", feignException.status(), feignException);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"A banking service is unavailable\"}");
    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<@NonNull CustomError> handleUnexpected(RuntimeException unexpectedException) {
        log.error("Unexpected error", unexpectedException);
        return toCustomErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, CustomError.Header.API_ERROR, "Unexpected error");
    }

    private static ResponseEntity<@NonNull CustomError> toCustomErrorResponse(
            HttpStatus httpStatus,
            CustomError.Header header,
            String message) {

        CustomError customError = CustomError.builder()
                .httpStatus(httpStatus)
                .header(header.getName())
                .message(message)
                .build();

        return new ResponseEntity<>(customError, httpStatus);
    }
}
