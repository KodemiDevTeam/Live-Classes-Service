package com.example.live_classes_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveClassJoinResponseDTO {
    private String token;
    private String roomId;
    private String liveClassId;
    private String role;
}