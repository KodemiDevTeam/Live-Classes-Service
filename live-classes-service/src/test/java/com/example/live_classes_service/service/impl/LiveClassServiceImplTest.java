package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveClassServiceImplTest {

    @Mock private LiveClassRepository repository;
    @Mock private EnrollmentClient enrollmentClient;
    @Mock private JwtUtil jwtUtil;
    @Mock private VideoSDKService videoSDKService;

    @InjectMocks
    private LiveClassServiceImpl service;

    private static final String TOKEN = "Bearer test-token";
    private static final String TRAINER_ID = "trainer-123";
    private static final String TRAINER_NAME = "John Doe";
    private static final String LIVE_CLASS_ID = "lc-001";
    private static final String ROOM_ID = "room-001";
    private static final String COURSE_ID = "course-001";

    private LiveClassEntity entity;

    @BeforeEach
    void setUp() {
        entity = LiveClassEntity.builder()
                .liveClassId(LIVE_CLASS_ID).courseId(COURSE_ID)
                .trainerId(TRAINER_ID).trainerName(TRAINER_NAME)
                .roomId(ROOM_ID).title("Test Class").description("Desc")
                .status("SCHEDULED").sessionType("LIVE_CLASS")
                .actionType("CREATED").isRecording(false).maxParticipants(50)
                .build();
    }

    // ── createLiveClass ──────────────────────────────────────────────────────

    @Test
    void createLiveClass_success() {
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(TRAINER_NAME);
        when(videoSDKService.createRoom()).thenReturn(ROOM_ID);

        CreateLiveClassRequest req = new CreateLiveClassRequest();
        req.setTitle("Test Class"); req.setCourseId(COURSE_ID);
        req.setScheduledAt("2026-05-01T10:00:00Z"); req.setMaxParticipants(50);

        LiveClassResponseDTO result = service.createLiveClass(req, TOKEN);

        assertNotNull(result);
        assertEquals("Test Class", result.getTitle());
        assertEquals(TRAINER_ID, result.getTrainerId());
        verify(repository).save(any(LiveClassEntity.class));
    }

    @Test
    void createLiveClass_notTrainer_throwsUnauthorized() {
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        CreateLiveClassRequest req = new CreateLiveClassRequest();
        assertThrows(UnauthorizedException.class,
                () -> service.createLiveClass(req, TOKEN));
        verify(repository, never()).save(any());
    }

    @Test
    void createLiveClass_videoSDKFails_retriesAndThrows() {
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(TRAINER_NAME);
        when(videoSDKService.createRoom()).thenThrow(new RuntimeException("API down"));
        CreateLiveClassRequest req = new CreateLiveClassRequest();
        assertThrows(BadRequestException.class,
                () -> service.createLiveClass(req, TOKEN));
    }

    // ── startLiveClass ───────────────────────────────────────────────────────

    @Test
    void startLiveClass_success() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(TRAINER_NAME);
        when(repository.updateStatusIfNotStarted(eq(LIVE_CLASS_ID), anyString(), anyString())).thenReturn(true);

        LiveClassResponseDTO result = service.startLiveClass(LIVE_CLASS_ID, TOKEN);

        assertNotNull(result);
        assertEquals("LIVE_STARTED", result.getStatus());
    }

    @Test
    void startLiveClass_notFound_throwsBadRequest() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.startLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void startLiveClass_alreadyEnded_throwsBadRequest() {
        entity.setStatus("ENDED");
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(TRAINER_NAME);
        assertThrows(BadRequestException.class, () -> service.startLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void startLiveClass_alreadyStarted_throwsBadRequest() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(TRAINER_NAME);
        when(repository.updateStatusIfNotStarted(eq(LIVE_CLASS_ID), anyString(), anyString())).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.startLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void startLiveClass_notTrainer_throwsUnauthorized() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other-user");
        when(jwtUtil.extractName(TOKEN)).thenReturn("Other");
        assertThrows(UnauthorizedException.class, () -> service.startLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    // ── joinLiveClass ────────────────────────────────────────────────────────

    @Test
    void joinLiveClass_asTrainer_success() {
        entity.setStatus("LIVE_STARTED");
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        when(videoSDKService.generateToken()).thenReturn("sdk-token");

        LiveClassJoinResponseDTO result = service.joinLiveClass(LIVE_CLASS_ID, TOKEN);

        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals("sdk-token", result.getToken());
        assertEquals("TRAINER", result.getRole());
    }

    @Test
    void joinLiveClass_notStarted_throwsBadRequest() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        assertThrows(BadRequestException.class, () -> service.joinLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void joinLiveClass_asLearner_enrolled_success() {
        entity.setStatus("LIVE_STARTED");
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        when(videoSDKService.generateToken()).thenReturn("sdk-token");

        SessionStatusResponse status = new SessionStatusResponse();
        status.setEnrolled(true);
        when(enrollmentClient.getCourseEnrollmentStatus(COURSE_ID, TOKEN)).thenReturn(status);

        LiveClassJoinResponseDTO result = service.joinLiveClass(LIVE_CLASS_ID, TOKEN);

        assertNotNull(result);
        assertEquals("LEARNER", result.getRole());
    }

    @Test
    void joinLiveClass_asLearner_notEnrolled_throwsBadRequest() {
        entity.setStatus("LIVE_STARTED");
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");

        SessionStatusResponse status = new SessionStatusResponse();
        status.setEnrolled(false);
        when(enrollmentClient.getCourseEnrollmentStatus(COURSE_ID, TOKEN)).thenReturn(status);

        assertThrows(BadRequestException.class, () -> service.joinLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void joinLiveClass_asLearner_nullStatus_throwsBadRequest() {
        entity.setStatus("LIVE_STARTED");
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        when(enrollmentClient.getCourseEnrollmentStatus(COURSE_ID, TOKEN)).thenReturn(null);

        assertThrows(BadRequestException.class, () -> service.joinLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void joinLiveClass_invalidRole_throwsUnauthorized() {
        entity.setStatus("LIVE_STARTED");
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("ADMIN");

        assertThrows(UnauthorizedException.class, () -> service.joinLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void joinLiveClass_asTrainer_wrongTrainer_throwsUnauthorized() {
        entity.setStatus("LIVE_STARTED");
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other-trainer");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");

        assertThrows(UnauthorizedException.class, () -> service.joinLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    // ── getLiveClassesByCourse ───────────────────────────────────────────────

    @Test
    void getLiveClassesByCourse_returnsList() {
        when(repository.findByCourseId(COURSE_ID)).thenReturn(List.of(entity));

        List<LiveClassResponseDTO> result = service.getLiveClassesByCourse(COURSE_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(LIVE_CLASS_ID, result.get(0).getLiveClassId());
    }

    @Test
    void getLiveClassesByCourse_emptyList() {
        when(repository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        assertTrue(service.getLiveClassesByCourse(COURSE_ID).isEmpty());
    }

    // ── startRecording ───────────────────────────────────────────────────────

    @Test
    void startRecording_success() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);
        when(videoSDKService.startRecording(ROOM_ID)).thenReturn("rec-001");

        assertEquals("Recording Started", service.startRecording(LIVE_CLASS_ID, TOKEN));
        assertTrue(entity.getIsRecording());
        verify(repository).save(entity);
    }

    @Test
    void startRecording_notTrainer_throwsUnauthorized() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other-user");
        assertThrows(UnauthorizedException.class, () -> service.startRecording(LIVE_CLASS_ID, TOKEN));
    }

    // ── stopRecording ────────────────────────────────────────────────────────

    @Test
    void stopRecording_success() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);

        assertEquals("Recording Stopped", service.stopRecording(LIVE_CLASS_ID, TOKEN));
        assertFalse(entity.getIsRecording());
        verify(videoSDKService).stopRecording(ROOM_ID);
        verify(repository).save(entity);
    }

    // ── endLiveClass ─────────────────────────────────────────────────────────

    @Test
    void endLiveClass_success() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(TRAINER_ID);

        assertEquals("Live Class Ended", service.endLiveClass(LIVE_CLASS_ID, TOKEN));
        assertEquals("ENDED", entity.getStatus());
        verify(repository).save(entity);
    }

    @Test
    void endLiveClass_notTrainer_throwsUnauthorized() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other-user");
        assertThrows(UnauthorizedException.class, () -> service.endLiveClass(LIVE_CLASS_ID, TOKEN));
    }

    @Test
    void endLiveClass_notFound_throwsBadRequest() {
        when(repository.findById(LIVE_CLASS_ID)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.endLiveClass(LIVE_CLASS_ID, TOKEN));
    }
}
