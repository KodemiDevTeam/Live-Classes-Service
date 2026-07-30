package com.example.live_classes_service.service.scheduler;

import com.example.live_classes_service.dto.notification.NotificationChannel;
import com.example.live_classes_service.dto.notification.NotificationRequest;
import com.example.live_classes_service.dto.notification.NotificationType;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.service.notification.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveClassReminderSchedulerTest {

    @Mock private LiveClassRepository liveClassRepository;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private EnrollmentClient enrollmentClient;
    @InjectMocks private LiveClassReminderScheduler scheduler;

    private static final String LIVE_CLASS_ID = "lc-001";
    private static final String COURSE_ID = "course-001";
    private static final String TITLE = "Java Basics Class";

    private LiveClassEntity liveClassEntity;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        String scheduledTime = now.plus(15, ChronoUnit.MINUTES).toString();

        liveClassEntity = LiveClassEntity.builder()
                .liveClassId(LIVE_CLASS_ID)
                .courseId(COURSE_ID)
                .title(TITLE)
                .status("SCHEDULED")
                .scheduledAt(scheduledTime)
                .build();
    }

    @Test
    void sendReminders_withUpcomingClass_success() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);
        List<String> learners = Arrays.asList("learner-001", "learner-002", "learner-003");

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(learners);

        scheduler.sendReminders();

        verify(liveClassRepository).findUpcomingLiveClasses(anyString(), anyString());
        verify(enrollmentClient).getEnrolledLearners(COURSE_ID);
        verify(notificationPublisher).publishToUsers(eq(learners), any(NotificationRequest.class));
    }


    @Test
    void sendReminders_verifyNotificationContent() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);
        List<String> learners = Arrays.asList("learner-001");

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(learners);

        scheduler.sendReminders();

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationPublisher).publishToUsers(eq(learners), captor.capture());

        NotificationRequest sentRequest = captor.getValue();
        assertEquals("Live Class Reminder", sentRequest.getTitle());
        assertNotNull(sentRequest.getMessage());
        assertTrue(sentRequest.getMessage().contains(TITLE));
        assertTrue(sentRequest.getMessage().contains("15 minutes"));
        assertEquals(NotificationType.LIVE_CLASS_REMINDER, sentRequest.getType());
        assertEquals(LIVE_CLASS_ID, sentRequest.getReferenceId());
        assertEquals("LIVE_CLASS", sentRequest.getReferenceType());
        assertTrue(sentRequest.getChannels().contains(NotificationChannel.IN_APP));
        assertTrue(sentRequest.getChannels().contains(NotificationChannel.EMAIL));
    }

    @Test
    void sendReminders_noUpcomingClasses_noNotificationsSent() {
        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        scheduler.sendReminders();

        verify(enrollmentClient, never()).getEnrolledLearners(anyString());
        verify(notificationPublisher, never()).publishToUsers(anyList(), any());
    }

    @Test
    void sendReminders_nullUpcomingClasses_noNotificationsSent() {
        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(null);

        scheduler.sendReminders();

        verify(enrollmentClient, never()).getEnrolledLearners(anyString());
        verify(notificationPublisher, never()).publishToUsers(anyList(), any());
    }

    @Test
    void sendReminders_noEnrolledLearners_noNotificationsSent() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(Collections.emptyList());

        scheduler.sendReminders();

        verify(enrollmentClient).getEnrolledLearners(COURSE_ID);
        verify(notificationPublisher, never()).publishToUsers(anyList(), any());
    }

    @Test
    void sendReminders_nullEnrolledLearners_noNotificationsSent() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(null);

        scheduler.sendReminders();

        verify(enrollmentClient).getEnrolledLearners(COURSE_ID);
        verify(notificationPublisher, never()).publishToUsers(anyList(), any());
    }

    @Test
    void sendReminders_enrollmentClientThrowsException_handlesGracefully() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID))
                .thenThrow(new RuntimeException("Enrollment service unavailable"));

        scheduler.sendReminders();

        verify(enrollmentClient).getEnrolledLearners(COURSE_ID);
        verify(notificationPublisher, never()).publishToUsers(anyList(), any());
    }

    @Test
    void sendReminders_multipleUpcomingClasses_notificationsSentForAll() {
        LiveClassEntity liveClass2 = LiveClassEntity.builder()
                .liveClassId("lc-002")
                .courseId("course-002")
                .title("Python Basics Class")
                .status("SCHEDULED")
                .scheduledAt(Instant.now().plus(15, ChronoUnit.MINUTES).toString())
                .build();

        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity, liveClass2);
        List<String> learnersForCourse1 = Arrays.asList("learner-001", "learner-002");
        List<String> learnersForCourse2 = Arrays.asList("learner-003", "learner-004");

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(learnersForCourse1);
        when(enrollmentClient.getEnrolledLearners("course-002")).thenReturn(learnersForCourse2);

        scheduler.sendReminders();

        verify(enrollmentClient, times(2)).getEnrolledLearners(anyString());
        verify(notificationPublisher, times(2)).publishToUsers(anyList(), any());
    }

    @Test
    void sendReminders_largeEnrolledLearnersList() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);
        List<String> largeLearnersGroup = Arrays.asList(
                "learner-001", "learner-002", "learner-003", "learner-004", "learner-005",
                "learner-006", "learner-007", "learner-008", "learner-009", "learner-010"
        );

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(largeLearnersGroup);

        scheduler.sendReminders();

        verify(notificationPublisher).publishToUsers(eq(largeLearnersGroup), any(NotificationRequest.class));
    }

    @Test
    void sendReminders_timeWindowBoundaryChecks() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);
        List<String> learners = Arrays.asList("learner-001");

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(learners);

        scheduler.sendReminders();

        verify(liveClassRepository).findUpcomingLiveClasses(anyString(), anyString());
        verify(notificationPublisher).publishToUsers(eq(learners), any(NotificationRequest.class));
    }

    @Test
    void sendReminders_notificationPublisherThrowsException_continuesExecution() {
        List<LiveClassEntity> upcomingClasses = Arrays.asList(liveClassEntity);
        List<String> learners = Arrays.asList("learner-001");

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners(COURSE_ID)).thenReturn(learners);
        doThrow(new RuntimeException("Publication failed")).when(notificationPublisher)
                .publishToUsers(eq(learners), any(NotificationRequest.class));

        scheduler.sendReminders();

        verify(notificationPublisher).publishToUsers(eq(learners), any(NotificationRequest.class));
    }

    @Test
    void sendReminders_integrationFlow_success() {
        Instant now = Instant.now();
        String scheduledTime1 = now.plus(15, ChronoUnit.MINUTES).toString();
        String scheduledTime2 = now.plus(15, ChronoUnit.MINUTES).plus(30, ChronoUnit.SECONDS).toString();

        LiveClassEntity class1 = LiveClassEntity.builder()
                .liveClassId("lc-001")
                .courseId("course-001")
                .title("Morning Session")
                .status("SCHEDULED")
                .scheduledAt(scheduledTime1)
                .build();

        LiveClassEntity class2 = LiveClassEntity.builder()
                .liveClassId("lc-002")
                .courseId("course-002")
                .title("Afternoon Session")
                .status("SCHEDULED")
                .scheduledAt(scheduledTime2)
                .build();

        List<LiveClassEntity> upcomingClasses = Arrays.asList(class1, class2);

        when(liveClassRepository.findUpcomingLiveClasses(anyString(), anyString()))
                .thenReturn(upcomingClasses);
        when(enrollmentClient.getEnrolledLearners("course-001"))
                .thenReturn(Arrays.asList("learner-001", "learner-002"));
        when(enrollmentClient.getEnrolledLearners("course-002"))
                .thenReturn(Arrays.asList("learner-003"));

        scheduler.sendReminders();

        verify(liveClassRepository).findUpcomingLiveClasses(anyString(), anyString());
        verify(enrollmentClient, times(2)).getEnrolledLearners(anyString());
        verify(notificationPublisher, times(2)).publishToUsers(anyList(), any());
    }
}
