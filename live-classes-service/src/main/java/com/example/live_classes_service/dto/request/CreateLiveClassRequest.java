package com.example.live_classes_service.dto.request;

import lombok.Data;

@Data
public class CreateLiveClassRequest {
    private String title;
    private String description;
    private String courseId;
    private String scheduledAt;
    private Integer maxParticipants;
}