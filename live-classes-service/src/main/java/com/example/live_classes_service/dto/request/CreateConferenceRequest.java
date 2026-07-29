package com.example.live_classes_service.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateConferenceRequest {

    private String title;
    private String description;
    private String scheduledAt;
    private Integer maxParticipants;
    
    // Optional fields for LIVE course creation flow
    private String courseId;
    private String moduleId;
    private String lessonId;
    private String sourceType;
    private Double price;
}