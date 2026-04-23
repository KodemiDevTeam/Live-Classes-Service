package com.example.live_classes_service.exception;

public class S3UploadException extends RuntimeException {
    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
