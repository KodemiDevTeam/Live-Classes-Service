package com.example.live_classes_service.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void badRequestException_message() {
        BadRequestException ex = new BadRequestException("bad request");
        assertEquals("bad request", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void nullBodyException_message() {
        NullBodyException ex = new NullBodyException("null body");
        assertEquals("null body", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void unauthorizedException_message() {
        UnauthorizedException ex = new UnauthorizedException("unauthorized");
        assertEquals("unauthorized", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void tokenNotFoundException_message() {
        TokenNotFoundException ex = new TokenNotFoundException("token missing");
        assertEquals("token missing", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void s3UploadException_messageAndCause() {
        Throwable cause = new RuntimeException("io error");
        S3UploadException ex = new S3UploadException("upload failed", cause);
        assertEquals("upload failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
