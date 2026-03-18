package com.example.live_classes_service.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionJoinResponseDTO {
    private String token;
    private String roomId;
    private String sessionId;
    private String role;
}
