package com.example.live_classes_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponseDTO {
    private String sessionId;
    private String organizerId;
    private String organizerName;
    private String actionType;
    private String title;
    private String description;
    private String sessionType;
    private String roomId;
    private String status;
    private String scheduledAt;
    private String startedAt;
    private String endedAt;
    private Integer maxParticipants;
    private Boolean isRecording;
    private String recordingUrl;
    private String createdAt;
}