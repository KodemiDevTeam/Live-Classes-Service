package com.example.live_classes_service.dto.request;

import lombok.Data;

@Data
public class CreateSessionRequest {

    private String title;
    private String description;
    private String scheduledAt;
    private int maxParticipants;
}
