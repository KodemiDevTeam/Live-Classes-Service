package com.example.live_classes_service.service.scheduler;

import com.example.live_classes_service.dto.notification.NotificationChannel;
import com.example.live_classes_service.dto.notification.NotificationRequest;
import com.example.live_classes_service.dto.notification.NotificationType;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.service.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveClassReminderScheduler {

    private final LiveClassRepository liveClassRepository;
    private final NotificationPublisher notificationPublisher;
    private final EnrollmentClient enrollmentClient;

    // Run every minute
    @Scheduled(cron = "0 * * * * *")
    public void sendReminders() {
        // Find classes scheduled to start between 15 and 16 minutes from now
        Instant now = Instant.now();
        Instant startWindow = now.plus(15, ChronoUnit.MINUTES);
        Instant endWindow = now.plus(16, ChronoUnit.MINUTES);

        List<LiveClassEntity> upcomingClasses = liveClassRepository.findUpcomingLiveClasses(
                startWindow.toString(),
                endWindow.toString()
        );

        if (upcomingClasses == null || upcomingClasses.isEmpty()) {
            return;
        }

        for (LiveClassEntity liveClass : upcomingClasses) {
            try {
                List<String> learners = enrollmentClient.getEnrolledLearners(liveClass.getCourseId());
                if (learners != null && !learners.isEmpty()) {
                    NotificationRequest notif = NotificationRequest.builder()
                            .title("Live Class Reminder")
                            .message("Your live class '" + liveClass.getTitle() + "' is starting in 15 minutes!")
                            .type(NotificationType.LIVE_CLASS_REMINDER)
                            .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                            .referenceId(liveClass.getLiveClassId())
                            .referenceType("LIVE_CLASS")
                            .build();
                    notificationPublisher.publishToUsers(learners, notif);
                    log.info("Sent reminder for live class: {}", liveClass.getLiveClassId());
                }
            } catch (Exception ex) {
                log.error("Failed to send reminder for live class: {}", liveClass.getLiveClassId(), ex);
            }
        }
    }
}
