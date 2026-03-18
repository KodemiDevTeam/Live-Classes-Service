package com.example.live_classes_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConferenceJoinResponseDTO {
    private String token;
    private String roomId;
    private String conferenceId;
    private String role;
}
