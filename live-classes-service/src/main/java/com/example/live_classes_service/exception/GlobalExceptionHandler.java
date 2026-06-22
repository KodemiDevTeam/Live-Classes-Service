package com.example.live_classes_service.exception;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.stream.Collectors;

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

    // ========================
    // 400 - Bad Request
    // ========================

    @ExceptionHandler({BadRequestException.class, NullBodyException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestException(RuntimeException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request body. Please check the request format.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing.";
        log.warn("Missing request parameter: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = "Parameter '" + ex.getName() + "' has invalid value: '" + ex.getValue() + "'.";
        log.warn("Type mismatch: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // ========================
    // 401 - Unauthorized
    // ========================

    @ExceptionHandler({UnauthorizedException.class, TokenNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(RuntimeException ex, WebRequest request) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, WebRequest request) {
        if ("Authorization".equalsIgnoreCase(ex.getHeaderName())) {
            log.warn("Missing Authorization header");
            return buildResponse(HttpStatus.UNAUTHORIZED, "Authorization header is required.", request);
        }
        log.warn("Missing required header: {}", ex.getHeaderName());
        return buildResponse(HttpStatus.BAD_REQUEST, "Required header '" + ex.getHeaderName() + "' is missing.", request);
    }

    // ========================
    // 403 - Forbidden
    // ========================

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        log.warn("Access forbidden: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // ========================
    // 404 - Not Found
    // ========================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        log.warn("No endpoint found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "The requested endpoint was not found.", request);
    }

    // ========================
    // 405 - Method Not Allowed
    // ========================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.";
        log.warn("Method not allowed: {}", message);
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, message, request);
    }

    // ========================
    // 409 - Conflict
    // ========================

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex, WebRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // ========================
    // 415 - Unsupported Media Type
    // ========================

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        String message = "Content type '" + ex.getContentType() + "' is not supported. Please use 'application/json'.";
        log.warn("Unsupported media type: {}", message);
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, request);
    }

    // ========================
    // 500 - Internal Server Error
    // ========================

    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<ErrorResponse> handleS3UploadException(S3UploadException ex, WebRequest request) {
        log.error("S3 upload failure: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload recording. Please try again later.", request);
    }

    @ExceptionHandler(VideoSDKException.class)
    public ResponseEntity<ErrorResponse> handleVideoSDKException(VideoSDKException ex, WebRequest request) {
        log.error("VideoSDK API failure: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Video service operation failed. Please try again later.", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.error("Illegal state: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected state error occurred. Please try again.", request);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException ex, WebRequest request) {
        log.error("Null pointer exception at: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while processing your request.", request);
    }

    // ========================
    // 503 - Service Unavailable
    // ========================

    @ExceptionHandler(EnrollmentServiceException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentServiceException(EnrollmentServiceException ex, WebRequest request) {
        log.error("Enrollment service unavailable: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Enrollment service is currently unavailable. Please try again later.", request);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCallNotPermittedException(CallNotPermittedException ex, WebRequest request) {
        log.error("Circuit breaker is open: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service is temporarily unavailable due to high failure rate. Please try again later.", request);
    }

    @ExceptionHandler({FeignException.class, RetryableException.class})
    public ResponseEntity<ErrorResponse> handleFeignExceptions(FeignException ex, WebRequest request) {
        log.error("Feign client failure: status={}, message={}", ex.status(), ex.getMessage(), ex);

        if (ex.status() == 404) {
            return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found in the downstream service.", request);
        }
        if (ex.status() == 401 || ex.status() == 403) {
            return buildResponse(HttpStatus.FORBIDDEN, "Access denied by downstream service.", request);
        }

        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Communication with internal service failed. Please try again later.", request);
    }

    @ExceptionHandler({ConnectException.class, SocketTimeoutException.class})
    public ResponseEntity<ErrorResponse> handleNetworkExceptions(Exception ex, WebRequest request) {
        log.error("Network connection error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Downstream service connectivity issue. Please try again later.", request);
    }

    // ========================
    // Catch-all fallback
    // ========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, WebRequest request) {
        log.error("Unhandled exception [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred. Please try again later.", request);
    }
}
