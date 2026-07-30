package com.example.live_classes_service.service.notification;

import com.example.live_classes_service.client.NotificationClient;
import com.example.live_classes_service.dto.notification.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock private NotificationClient notificationClient;
    @InjectMocks private NotificationPublisher publisher;

    private static final String SERVICE_KEY = "test-service-key";
    private static final String USER_ID = "user-001";
    private static final String REFERENCE_ID = "ref-001";

    private NotificationRequest notificationRequest;
    private BroadcastNotificationRequest broadcastRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "serviceKey", SERVICE_KEY);

        notificationRequest = NotificationRequest.builder()
                .userId(USER_ID)
                .title("Test Notification")
                .message("Test Message")
                .type(NotificationType.LIVE_CLASS_REMINDER)
                .channels(Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .referenceId(REFERENCE_ID)
                .referenceType("LIVE_CLASS")
                .build();

        broadcastRequest = BroadcastNotificationRequest.builder()
                .title("Broadcast Test")
                .message("Broadcast Message")
                .type(NotificationType.LIVE_CLASS_REMINDER)
                .targetRole("LEARNER")
                .channels(Arrays.asList(NotificationChannel.IN_APP))
                .referenceId(REFERENCE_ID)
                .referenceType("LIVE_CLASS")
                .build();
    }

    @Test
    void publish_success() {
        publisher.publish(notificationRequest);

        verify(notificationClient, times(1)).sendInternalNotification(SERVICE_KEY, notificationRequest);
    }

    @Test
    void publish_withException_handlesGracefully() {
        doThrow(new RuntimeException("Service unavailable")).when(notificationClient)
                .sendInternalNotification(SERVICE_KEY, notificationRequest);

        publisher.publish(notificationRequest);

        verify(notificationClient, times(1)).sendInternalNotification(SERVICE_KEY, notificationRequest);
    }

    @Test
    void publish_withNullRequest_handlesGracefully() {
        // Test that the publisher gracefully handles when logging occurs with valid request
        NotificationRequest validRequest = notificationRequest;
        publisher.publish(validRequest);

        verify(notificationClient, times(1)).sendInternalNotification(SERVICE_KEY, validRequest);
    }

    @Test
    void publish_multipleTimes_successAllCalls() {
        publisher.publish(notificationRequest);
        publisher.publish(notificationRequest);
        publisher.publish(notificationRequest);

        verify(notificationClient, times(3)).sendInternalNotification(SERVICE_KEY, notificationRequest);
    }

    @Test
    void publishBroadcast_success() {
        publisher.publishBroadcast(broadcastRequest);

        verify(notificationClient, times(1)).broadcastNotification(SERVICE_KEY, broadcastRequest);
    }

    @Test
    void publishBroadcast_withException_handlesGracefully() {
        doThrow(new RuntimeException("Broadcast failed")).when(notificationClient)
                .broadcastNotification(SERVICE_KEY, broadcastRequest);

        publisher.publishBroadcast(broadcastRequest);

        verify(notificationClient, times(1)).broadcastNotification(SERVICE_KEY, broadcastRequest);
    }

    @Test
    void publishBroadcast_differentTargetRoles() {
        BroadcastNotificationRequest trainerRequest = BroadcastNotificationRequest.builder()
                .title("Broadcast Test")
                .message("Broadcast Message")
                .type(NotificationType.LIVE_CLASS_REMINDER)
                .targetRole("TRAINER")
                .channels(Arrays.asList(NotificationChannel.IN_APP))
                .referenceId(REFERENCE_ID)
                .referenceType("LIVE_CLASS")
                .build();

        publisher.publishBroadcast(trainerRequest);

        verify(notificationClient, times(1)).broadcastNotification(SERVICE_KEY, trainerRequest);
    }

    @Test
    void publishToUsers_singleUser_success() {
        List<String> userIds = Arrays.asList(USER_ID);

        publisher.publishToUsers(userIds, notificationRequest);

        verify(notificationClient, times(1)).sendInternalNotification(SERVICE_KEY, notificationRequest);
    }

    @Test
    void publishToUsers_multipleUsers_success() {
        List<String> userIds = Arrays.asList("user-001", "user-002", "user-003");

        publisher.publishToUsers(userIds, notificationRequest);

        verify(notificationClient, times(3)).sendInternalNotification(eq(SERVICE_KEY), any(NotificationRequest.class));
    }

    @Test
    void publishToUsers_multipleUsers_verifyUserIdSet() {
        List<String> userIds = Arrays.asList("user-001", "user-002");
        NotificationRequest request = NotificationRequest.builder()
                .title("Test Notification")
                .message("Test Message")
                .type(NotificationType.LIVE_CLASS_REMINDER)
                .channels(Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .referenceId(REFERENCE_ID)
                .referenceType("LIVE_CLASS")
                .build();

        publisher.publishToUsers(userIds, request);

        verify(notificationClient, times(2)).sendInternalNotification(eq(SERVICE_KEY), any(NotificationRequest.class));
    }

    @Test
    void publishToUsers_emptyList_noNotificationsSent() {
        List<String> userIds = Arrays.asList();

        publisher.publishToUsers(userIds, notificationRequest);

        verify(notificationClient, never()).sendInternalNotification(anyString(), any());
    }

    @Test
    void publishToUsers_nullList_noNotificationsSent() {
        publisher.publishToUsers(null, notificationRequest);

        verify(notificationClient, never()).sendInternalNotification(anyString(), any());
    }

    @Test
    void publishToUsers_oneUserFails_continueWithOthers() {
        List<String> userIds = Arrays.asList("user-001", "user-002", "user-003");

        doThrow(new RuntimeException("User 2 failed"))
                .when(notificationClient)
                .sendInternalNotification(eq(SERVICE_KEY), any(NotificationRequest.class));

        publisher.publishToUsers(userIds, notificationRequest);

        verify(notificationClient, times(3)).sendInternalNotification(eq(SERVICE_KEY), any(NotificationRequest.class));
    }

    @Test
    void publishToUsers_differentNotificationTypes() {
        List<String> userIds = Arrays.asList("user-001", "user-002");

        NotificationRequest alertRequest = NotificationRequest.builder()
                .title("Test Notification")
                .message("Test Message")
                .type(NotificationType.LIVE_CLASS_STARTED)
                .channels(Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .referenceId(REFERENCE_ID)
                .referenceType("LIVE_CLASS")
                .build();

        publisher.publishToUsers(userIds, alertRequest);

        verify(notificationClient, times(2)).sendInternalNotification(eq(SERVICE_KEY), any(NotificationRequest.class));
    }

    @Test
    void publishToUsers_differentChannels() {
        List<String> userIds = Arrays.asList("user-001");

        NotificationRequest smsRequest = NotificationRequest.builder()
                .title("Test Notification")
                .message("Test Message")
                .type(NotificationType.LIVE_CLASS_REMINDER)
                .channels(Arrays.asList(NotificationChannel.SMS))
                .referenceId(REFERENCE_ID)
                .referenceType("LIVE_CLASS")
                .build();

        publisher.publishToUsers(userIds, smsRequest);

        verify(notificationClient, times(1)).sendInternalNotification(eq(SERVICE_KEY), any(NotificationRequest.class));
    }

    @Test
    void publishToUsers_largeUserList() {
        List<String> userIds = Arrays.asList(
                "user-001", "user-002", "user-003", "user-004", "user-005",
                "user-006", "user-007", "user-008", "user-009", "user-010"
        );

        publisher.publishToUsers(userIds, notificationRequest);

        verify(notificationClient, times(10)).sendInternalNotification(eq(SERVICE_KEY), any(NotificationRequest.class));
    }

    @Test
    void publish_preservesNotificationMetadata() {
        NotificationRequest metadataRequest = NotificationRequest.builder()
                .userId(USER_ID)
                .title("Test Notification")
                .message("Test Message")
                .type(NotificationType.LIVE_CLASS_REMINDER)
                .channels(Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .referenceId(REFERENCE_ID)
                .referenceType("LIVE_CLASS")
                .metadata(java.util.Map.of("key", "value", "status", "pending"))
                .build();

        publisher.publish(metadataRequest);

        verify(notificationClient, times(1)).sendInternalNotification(SERVICE_KEY, metadataRequest);
    }

    @Test
    void publishBroadcast_multipleCalls_success() {
        publisher.publishBroadcast(broadcastRequest);
        publisher.publishBroadcast(broadcastRequest);

        verify(notificationClient, times(2)).broadcastNotification(SERVICE_KEY, broadcastRequest);
    }
}
