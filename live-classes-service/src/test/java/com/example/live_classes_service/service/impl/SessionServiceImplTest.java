package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.SessionJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.SessionEntity;
import com.example.live_classes_service.repository.SessionRepository;
import com.example.live_classes_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock private SessionRepository repository;
    @Mock private JwtUtil jwtUtil;
    @Mock private VideoSDKService videoSDKService;
    @Mock private EnrollmentClient enrollmentClient;
    @InjectMocks private SessionServiceImpl service;

    private static final String TOKEN = "Bearer test-token";
    private static final String ORGANIZER_ID = "organizer-001";
    private static final String ORGANIZER_NAME = "Alice";
    private static final String SESSION_ID = "session-001";
    private static final String ROOM_ID = "room-001";

    private SessionEntity entity;

    @BeforeEach
    void setUp() {
        entity = SessionEntity.builder()
                .sessionId(SESSION_ID).organizerId(ORGANIZER_ID)
                .organizerName(ORGANIZER_NAME).roomId(ROOM_ID)
                .title("Test Session").status("SCHEDULED")
                .sessionType("SESSION").actionType("CREATED")
                .isRecording(false).maxParticipants(30).build();
    }

    @Test
    void createSession_success() {
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(videoSDKService.createRoom()).thenReturn(ROOM_ID);
        CreateSessionRequest req = new CreateSessionRequest();
        req.setTitle("Test Session"); req.setMaxParticipants(30);
        SessionResponseDTO result = service.createSession(req, TOKEN);
        assertNotNull(result);
        assertEquals("Test Session", result.getTitle());
        verify(repository).save(any(SessionEntity.class));
    }

    @Test
    void createSession_notTrainer_throwsUnauthorized() {
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        assertThrows(UnauthorizedException.class, () -> service.createSession(new CreateSessionRequest(), TOKEN));
        verify(repository, never()).save(any());
    }

    @Test
    void createSession_videoSDKFails_throwsBadRequest() {
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(videoSDKService.createRoom()).thenThrow(new RuntimeException("down"));
        assertThrows(BadRequestException.class, () -> service.createSession(new CreateSessionRequest(), TOKEN));
    }

    @Test
    void startSession_success() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(repository.updateStatusAtomically(eq(SESSION_ID), eq("SCHEDULED"), eq("SESSION_STARTED"), eq("startedAt"), anyString())).thenReturn(true);
        SessionResponseDTO result = service.startSession(SESSION_ID, TOKEN);
        assertNotNull(result);
        assertEquals("SESSION_STARTED", result.getStatus());
    }

    @Test
    void startSession_notFound_throwsBadRequest() {
        when(repository.findById(SESSION_ID)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.startSession(SESSION_ID, TOKEN));
    }

    @Test
    void startSession_alreadyEnded_throwsBadRequest() {
        entity.setStatus("ENDED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        assertThrows(BadRequestException.class, () -> service.startSession(SESSION_ID, TOKEN));
    }

    @Test
    void startSession_alreadyStarted_returnsCurrentState() {
        entity.setStatus("SESSION_STARTED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(repository.updateStatusAtomically(eq(SESSION_ID), eq("SCHEDULED"), eq("SESSION_STARTED"), eq("startedAt"), anyString())).thenReturn(false);
        assertNotNull(service.startSession(SESSION_ID, TOKEN));
    }

    @Test
    void startSession_notOrganizer_throwsUnauthorized() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other");
        when(jwtUtil.extractName(TOKEN)).thenReturn("Other");
        assertThrows(UnauthorizedException.class, () -> service.startSession(SESSION_ID, TOKEN));
    }

    @Test
    void joinSession_asTrainer_success() {
        entity.setStatus("SESSION_STARTED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        when(videoSDKService.generateToken()).thenReturn("sdk-token");
        SessionJoinResponseDTO result = service.joinSession(SESSION_ID, TOKEN);
        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
    }

    @Test
    void joinSession_notStarted_throwsBadRequest() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        assertThrows(BadRequestException.class, () -> service.joinSession(SESSION_ID, TOKEN));
    }

    @Test
    void joinSession_asLearner_enrolled_success() {
        entity.setStatus("SESSION_STARTED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        when(videoSDKService.generateToken()).thenReturn("sdk-token");
        SessionStatusResponse s = new SessionStatusResponse();
        s.setEnrolled(true);
        when(enrollmentClient.getSessionEnrollmentStatus(SESSION_ID, TOKEN)).thenReturn(s);
        assertEquals("LEARNER", service.joinSession(SESSION_ID, TOKEN).getRole());
    }

    @Test
    void joinSession_asLearner_notEnrolled_throwsBadRequest() {
        entity.setStatus("SESSION_STARTED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        SessionStatusResponse s = new SessionStatusResponse();
        s.setEnrolled(false);
        when(enrollmentClient.getSessionEnrollmentStatus(SESSION_ID, TOKEN)).thenReturn(s);
        assertThrows(BadRequestException.class, () -> service.joinSession(SESSION_ID, TOKEN));
    }

    @Test
    void joinSession_asLearner_nullStatus_throwsBadRequest() {
        entity.setStatus("SESSION_STARTED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        when(enrollmentClient.getSessionEnrollmentStatus(SESSION_ID, TOKEN)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.joinSession(SESSION_ID, TOKEN));
    }

    @Test
    void joinSession_invalidRole_throwsUnauthorized() {
        entity.setStatus("SESSION_STARTED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("ADMIN");
        assertThrows(UnauthorizedException.class, () -> service.joinSession(SESSION_ID, TOKEN));
    }

    @Test
    void joinSession_asTrainer_notOrganizer_throwsUnauthorized() {
        entity.setStatus("SESSION_STARTED");
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other-trainer");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        assertThrows(UnauthorizedException.class, () -> service.joinSession(SESSION_ID, TOKEN));
    }

    @Test
    void startRecording_success() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(videoSDKService.startRecording(ROOM_ID)).thenReturn("rec-001");
        assertEquals("Recording Started", service.startRecording(SESSION_ID, TOKEN));
        assertTrue(entity.getIsRecording());
        verify(repository).save(entity);
    }

    @Test
    void startRecording_notOrganizer_throwsUnauthorized() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other");
        assertThrows(UnauthorizedException.class, () -> service.startRecording(SESSION_ID, TOKEN));
    }

    @Test
    void stopRecording_success() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        assertEquals("Recording Stopped", service.stopRecording(SESSION_ID, TOKEN));
        assertFalse(entity.getIsRecording());
        verify(videoSDKService).stopRecording(ROOM_ID);
    }

    @Test
    void endSession_success() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        assertEquals("Session Ended", service.endSession(SESSION_ID, TOKEN));
        assertEquals("ENDED", entity.getStatus());
        verify(repository).save(entity);
    }

    @Test
    void endSession_notOrganizer_throwsUnauthorized() {
        when(repository.findById(SESSION_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other");
        assertThrows(UnauthorizedException.class, () -> service.endSession(SESSION_ID, TOKEN));
    }

    @Test
    void endSession_notFound_throwsBadRequest() {
        when(repository.findById(SESSION_ID)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.endSession(SESSION_ID, TOKEN));
    }
}
