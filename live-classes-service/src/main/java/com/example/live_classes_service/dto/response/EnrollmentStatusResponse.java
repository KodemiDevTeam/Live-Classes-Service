package com.example.live_classes_service.dto.response;


import lombok.Data;

@Data
public class EnrollmentStatusResponse {
    private boolean enrolled;
    private String role;
}
