package com.example.chatbackend.exception;

import com.example.chatbackend.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        log.warn("Validation failure on path={}", request.getRequestURI());
        return ResponseEntity.badRequest().body(buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception on path={} type={}",
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request.getRequestURI()
        ));
    }

    private ApiErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
