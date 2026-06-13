package com.example.live_classes_service.dto.notification;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private List<NotificationChannel> channels;
    private String referenceId;
    private String referenceType;
    private Map<String, Object> metadata;
}
