package com.example.live_classes_service.service.notification;

import com.example.live_classes_service.client.NotificationClient;
import com.example.live_classes_service.dto.notification.NotificationRequest;
import com.example.live_classes_service.dto.notification.BroadcastNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationPublisher {

    private final NotificationClient notificationClient;

    @Value("${internal.service.key:default-secret}")
    private String serviceKey;

    public NotificationPublisher(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void publish(NotificationRequest request) {
        try {
            notificationClient.sendInternalNotification(serviceKey, request);
            log.info("Notification sent successfully. type={}, userId={}, referenceId={}",
                    request.getType(), request.getUserId(), request.getReferenceId());
        } catch (Exception ex) {
            log.error("Failed to send notification. type={}, userId={}, referenceId={}",
                    request.getType(), request.getUserId(), request.getReferenceId(), ex);
        }
    }

    public void publishBroadcast(BroadcastNotificationRequest request) {
        try {
            notificationClient.broadcastNotification(serviceKey, request);
            log.info("Broadcast notification sent successfully. type={}, targetRole={}, referenceId={}",
                    request.getType(), request.getTargetRole(), request.getReferenceId());
        } catch (Exception ex) {
            log.error("Failed to send broadcast notification. type={}, targetRole={}, referenceId={}",
                    request.getType(), request.getTargetRole(), request.getReferenceId(), ex);
        }
    }

    public void publishToUsers(java.util.List<String> userIds, NotificationRequest request) {
        if (userIds == null || userIds.isEmpty()) return;
        for (String userId : userIds) {
            try {
                request.setUserId(userId);
                notificationClient.sendInternalNotification(serviceKey, request);
                log.info("Notification sent successfully. type={}, userId={}, referenceId={}",
                        request.getType(), userId, request.getReferenceId());
            } catch (Exception ex) {
                log.error("Failed to send notification. type={}, userId={}, referenceId={}",
                        request.getType(), userId, request.getReferenceId(), ex);
            }
        }
    }
}
