package com.example.live_classes_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionResponseDTO {
    private String sessionId;
    private String courseId;
    private String startTime;
    private String endTime;
    private String joinLink;
    private String status;
}
