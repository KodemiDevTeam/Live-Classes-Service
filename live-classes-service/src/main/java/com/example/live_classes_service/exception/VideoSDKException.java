package com.example.live_classes_service.exception;

public class VideoSDKException extends RuntimeException {
    public VideoSDKException(String message) {
        super(message);
    }

    public VideoSDKException(String message, Throwable cause) {
        super(message, cause);
    }
}
