package com.example.live_classes_service.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveClassResponseDTO {
    private String liveClassId;
    private String courseId;
    private String trainerId;
    private String trainerName;
    private String title;
    private String description;
    private String actionType;
    private String sessionType;
    private String roomId;
    private String status;
    private Boolean isRecording;
    private String scheduledAt;
    private String startedAt;
    private String endedAt;
    private Integer maxParticipants;
    private String recordingUrl;
    private String createdAt;
}
