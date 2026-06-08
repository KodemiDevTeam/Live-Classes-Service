package com.example.live_classes_service.exception;

public class EnrollmentServiceException extends RuntimeException {
    public EnrollmentServiceException(String message) {
        super(message);
    }

    public EnrollmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
