package com.example.live_classes_service.dto.notification;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BroadcastNotificationRequest {
    private String title;
    private String message;
    private NotificationType type;
    private List<NotificationChannel> channels;
    private String targetRole;
    private String sendMode;
    private String referenceId;
    private String referenceType;
}
