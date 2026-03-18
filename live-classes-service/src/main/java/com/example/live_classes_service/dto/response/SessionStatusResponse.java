package com.example.live_classes_service.dto.response;

import lombok.Data;

@Data
public class SessionStatusResponse {
    private boolean enrolled;
    private String role;
}
