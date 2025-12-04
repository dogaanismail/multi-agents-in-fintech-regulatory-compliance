package org.banksolution.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
public class CustomResponse<T> {

    @Builder.Default
    private LocalDateTime time = LocalDateTime.now();

    private HttpStatus httpStatus;

    private Boolean isSuccess;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T response;

    public static final CustomResponse<Void> SUCCESS = CustomResponse.<Void>builder()
            .httpStatus(HttpStatus.OK)
            .isSuccess(true)
            .build();

    public static <T> CustomResponse<T> successOf(final T response) {
        return CustomResponse.<T>builder()
                .httpStatus(HttpStatus.OK)
                .isSuccess(true)
                .response(response)
                .build();
    }

    public static <T> CustomResponse<T> failOf(final T response) {
        return CustomResponse.<T>builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .isSuccess(false)
                .response(response)
                .build();
    }

}
