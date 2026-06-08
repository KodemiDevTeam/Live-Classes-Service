package com.example.live_classes_service.exception;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Data
    @Builder
    public static class ErrorResponse {
        private String timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(EnrollmentServiceException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentServiceException(EnrollmentServiceException ex, WebRequest request) {
        log.error("Enrollment Service unavailable: {}", ex.getMessage(), ex);
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE, 
                "Enrollment service is currently unavailable. Access denied.", 
                request
        );
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCallNotPermittedException(CallNotPermittedException ex, WebRequest request) {
        log.error("Circuit Breaker is open: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE, 
                "Downstream enrollment service circuit breaker is open. Please try again later.", 
                request
        );
    }

    @ExceptionHandler({FeignException.class, RetryableException.class})
    public ResponseEntity<ErrorResponse> handleFeignExceptions(FeignException ex, WebRequest request) {
        log.error("Feign client communication failure: status={}, message={}", ex.status(), ex.getMessage(), ex);
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE, 
                "Communication with internal enrollment service failed.", 
                request
        );
    }

    @ExceptionHandler({ConnectException.class, SocketTimeoutException.class})
    public ResponseEntity<ErrorResponse> handleNetworkExceptions(Exception ex, WebRequest request) {
        log.error("Network connection error: {}", ex.getMessage(), ex);
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE, 
                "Downstream connectivity issues. Please try again.", 
                request
        );
    }

    @ExceptionHandler({BadRequestException.class, NullBodyException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestException(RuntimeException ex, WebRequest request) {
        log.warn("Bad Request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({UnauthorizedException.class, TokenNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(RuntimeException ex, WebRequest request) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<ErrorResponse> handleS3UploadException(S3UploadException ex, WebRequest request) {
        log.error("S3 upload failure: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected internal error occurred.", 
                request
        );
    }
}
