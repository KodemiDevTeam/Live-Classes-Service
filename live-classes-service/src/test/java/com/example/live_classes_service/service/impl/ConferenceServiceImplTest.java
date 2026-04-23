package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.NullBodyException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.ConferenceEntity;
import com.example.live_classes_service.repository.ConferenceRepository;
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
class ConferenceServiceImplTest {

    @Mock private ConferenceRepository repository;
    @Mock private JwtUtil jwtUtil;
    @Mock private VideoSDKService videoSDKService;
    @Mock private EnrollmentClient enrollmentClient;
    @InjectMocks private ConferenceServiceImpl service;

    private static final String TOKEN = "Bearer test-token";
    private static final String ORGANIZER_ID = "organizer-001";
    private static final String ORGANIZER_NAME = "Jane Smith";
    private static final String CONFERENCE_ID = "conf-001";
    private static final String ROOM_ID = "room-001";

    private ConferenceEntity entity;

    @BeforeEach
    void setUp() {
        entity = ConferenceEntity.builder()
                .conferenceId(CONFERENCE_ID).organizerId(ORGANIZER_ID)
                .organizerName(ORGANIZER_NAME).roomId(ROOM_ID)
                .title("Test Conference").status("SCHEDULED")
                .sessionType("SESSION").actionType("CREATED")
                .isRecording(false).maxParticipants(100).build();
    }

    @Test void createConference_success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(videoSDKService.createRoom()).thenReturn(ROOM_ID);
        CreateConferenceRequest req = new CreateConferenceRequest();
        req.setTitle("Test Conference"); req.setMaxParticipants(100);
        ConferenceResponseDTO result = service.createConference(req, TOKEN);
        assertNotNull(result);
        assertEquals("Test Conference", result.getTitle());
        verify(repository).save(any(ConferenceEntity.class));
    }

    @Test void createConference_nullRequest_throwsNullBodyException() {
        assertThrows(NullBodyException.class, () -> service.createConference(null, TOKEN));
        verify(repository, never()).save(any());
    }

    @Test void createConference_videoSDKFails_throwsBadRequest() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(videoSDKService.createRoom()).thenThrow(new RuntimeException("down"));
        assertThrows(BadRequestException.class, () -> service.createConference(new CreateConferenceRequest(), TOKEN));
    }

    @Test void startConference_success() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(repository.updateStatusIfNotStarted(eq(CONFERENCE_ID), anyString())).thenReturn(true);
        ConferenceResponseDTO result = service.startConference(CONFERENCE_ID, TOKEN);
        assertNotNull(result);
        assertEquals("CONFERENCE_STARTED", result.getStatus());
    }

    @Test void startConference_nullId_throwsNullBodyException() {
        assertThrows(NullBodyException.class, () -> service.startConference(null, TOKEN));
    }

    @Test void startConference_notFound_throwsBadRequest() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.startConference(CONFERENCE_ID, TOKEN));
    }

    @Test void startConference_alreadyEnded_throwsBadRequest() {
        entity.setStatus("ENDED");
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        assertThrows(BadRequestException.class, () -> service.startConference(CONFERENCE_ID, TOKEN));
    }

    @Test void startConference_alreadyStarted_throwsBadRequest() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractName(TOKEN)).thenReturn(ORGANIZER_NAME);
        when(repository.updateStatusIfNotStarted(eq(CONFERENCE_ID), anyString())).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.startConference(CONFERENCE_ID, TOKEN));
    }

    @Test void startConference_notOrganizer_throwsUnauthorized() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other");
        when(jwtUtil.extractName(TOKEN)).thenReturn("Other");
        assertThrows(UnauthorizedException.class, () -> service.startConference(CONFERENCE_ID, TOKEN));
    }

    @Test void joinConference_asTrainer_success() {
        entity.setStatus("CONFERENCE_STARTED");
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        when(videoSDKService.generateToken()).thenReturn("sdk-token");
        ConferenceJoinResponseDTO result = service.joinConference(CONFERENCE_ID, TOKEN);
        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
    }

    @Test void joinConference_notStarted_throwsBadRequest() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        assertThrows(BadRequestException.class, () -> service.joinConference(CONFERENCE_ID, TOKEN));
    }

    @Test void joinConference_asLearner_enrolled_success() {
        entity.setStatus("CONFERENCE_STARTED");
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        when(videoSDKService.generateToken()).thenReturn("sdk-token");
        SessionStatusResponse s = new SessionStatusResponse(); s.setEnrolled(true);
        when(enrollmentClient.getConferenceEnrollmentStatus(CONFERENCE_ID, TOKEN)).thenReturn(s);
        assertEquals("LEARNER", service.joinConference(CONFERENCE_ID, TOKEN).getRole());
    }

    @Test void joinConference_asLearner_notEnrolled_throwsBadRequest() {
        entity.setStatus("CONFERENCE_STARTED");
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        SessionStatusResponse s = new SessionStatusResponse(); s.setEnrolled(false);
        when(enrollmentClient.getConferenceEnrollmentStatus(CONFERENCE_ID, TOKEN)).thenReturn(s);
        assertThrows(BadRequestException.class, () -> service.joinConference(CONFERENCE_ID, TOKEN));
    }

    @Test void joinConference_asLearner_nullStatus_throwsBadRequest() {
        entity.setStatus("CONFERENCE_STARTED");
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("learner-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("LEARNER");
        when(enrollmentClient.getConferenceEnrollmentStatus(CONFERENCE_ID, TOKEN)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.joinConference(CONFERENCE_ID, TOKEN));
    }

    @Test void joinConference_invalidRole_throwsUnauthorized() {
        entity.setStatus("CONFERENCE_STARTED");
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("ADMIN");
        assertThrows(UnauthorizedException.class, () -> service.joinConference(CONFERENCE_ID, TOKEN));
    }

    @Test void joinConference_asTrainer_notOrganizer_throwsUnauthorized() {
        entity.setStatus("CONFERENCE_STARTED");
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other-trainer");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("TRAINER");
        assertThrows(UnauthorizedException.class, () -> service.joinConference(CONFERENCE_ID, TOKEN));
    }

    @Test void startRecording_success() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        when(videoSDKService.startRecording(ROOM_ID)).thenReturn("rec-001");
        assertEquals("Recording Started", service.startRecording(CONFERENCE_ID, TOKEN));
        verify(repository).save(entity);
    }

    @Test void startRecording_notOrganizer_throwsUnauthorized() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other");
        assertThrows(UnauthorizedException.class, () -> service.startRecording(CONFERENCE_ID, TOKEN));
    }

    @Test void stopRecording_success() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        assertEquals("Recording Stopped", service.stopRecording(CONFERENCE_ID, TOKEN));
        verify(videoSDKService).stopRecording(ROOM_ID);
    }

    @Test void endConference_success() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(ORGANIZER_ID);
        assertEquals("Conference Ended", service.endConference(CONFERENCE_ID, TOKEN));
        assertEquals("ENDED", entity.getStatus());
        verify(repository).save(entity);
    }

    @Test void endConference_notOrganizer_throwsUnauthorized() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(entity);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("other");
        assertThrows(UnauthorizedException.class, () -> service.endConference(CONFERENCE_ID, TOKEN));
    }

    @Test void endConference_notFound_throwsBadRequest() {
        when(repository.findById(CONFERENCE_ID)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.endConference(CONFERENCE_ID, TOKEN));
    }
}
