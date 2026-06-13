package com.example.live_classes_service.client.notification;

import com.example.live_classes_service.dto.notification.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${notification.service.url:}")
public interface NotificationClient {
    @PostMapping("/api/v1/notifications/internal/send")
    Map<String, String> sendInternalNotification(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody NotificationRequest request
    );

    @PostMapping("/api/v1/notifications/internal/broadcast")
    Map<String, Object> broadcastNotification(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody com.example.live_classes_service.dto.notification.BroadcastNotificationRequest request
    );
}
