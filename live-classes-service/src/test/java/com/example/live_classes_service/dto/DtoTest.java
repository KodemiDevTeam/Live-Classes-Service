package com.example.live_classes_service.dto;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void createConferenceRequest_gettersSetters() {
        CreateConferenceRequest r = new CreateConferenceRequest();
        r.setTitle("Conf"); r.setDescription("Desc");
        r.setScheduledAt("2026-05-01"); r.setMaxParticipants(100);
        assertEquals("Conf", r.getTitle());
        assertEquals("Desc", r.getDescription());
        assertEquals("2026-05-01", r.getScheduledAt());
        assertEquals(100, r.getMaxParticipants());
    }

    @Test
    void createLiveClassRequest_gettersSetters() {
        CreateLiveClassRequest r = new CreateLiveClassRequest();
        r.setTitle("Class"); r.setDescription("Desc");
        r.setCourseId("c1"); r.setScheduledAt("2026-05-01"); r.setMaxParticipants(50);
        assertEquals("Class", r.getTitle());
        assertEquals("c1", r.getCourseId());
        assertEquals(50, r.getMaxParticipants());
    }

    @Test
    void createSessionRequest_gettersSetters() {
        CreateSessionRequest r = new CreateSessionRequest();
        r.setTitle("Session"); r.setDescription("Desc");
        r.setScheduledAt("2026-05-01"); r.setMaxParticipants(30);
        assertEquals("Session", r.getTitle());
        assertEquals(30, r.getMaxParticipants());
    }

    @Test
    void conferenceResponseDTO_builder() {
        ConferenceResponseDTO dto = ConferenceResponseDTO.builder()
                .conferenceId("c1").organizerId("o1").organizerName("Alice")
                .title("Conf").status("SCHEDULED").roomId("r1")
                .sessionType("SESSION").actionType("CREATED")
                .isRecording(false).maxParticipants(100).build();
        assertEquals("c1", dto.getConferenceId());
        assertEquals("Alice", dto.getOrganizerName());
        assertEquals("SCHEDULED", dto.getStatus());
        assertFalse(dto.getIsRecording());
    }

    @Test
    void conferenceJoinResponseDTO_builder() {
        ConferenceJoinResponseDTO dto = ConferenceJoinResponseDTO.builder()
                .conferenceId("c1").roomId("r1").token("tok").role("TRAINER").build();
        assertEquals("c1", dto.getConferenceId());
        assertEquals("r1", dto.getRoomId());
        assertEquals("tok", dto.getToken());
        assertEquals("TRAINER", dto.getRole());
    }

    @Test
    void liveClassResponseDTO_builder() {
        LiveClassResponseDTO dto = LiveClassResponseDTO.builder()
                .liveClassId("lc1").courseId("course1").trainerId("t1")
                .trainerName("Bob").title("Class").status("SCHEDULED")
                .roomId("r1").sessionType("LIVE_CLASS").actionType("CREATED")
                .isRecording(false).maxParticipants(50).build();
        assertEquals("lc1", dto.getLiveClassId());
        assertEquals("course1", dto.getCourseId());
        assertEquals("SCHEDULED", dto.getStatus());
    }

    @Test
    void liveClassJoinResponseDTO_builder() {
        LiveClassJoinResponseDTO dto = LiveClassJoinResponseDTO.builder()
                .liveClassId("lc1").roomId("r1").token("tok").role("LEARNER").build();
        assertEquals("lc1", dto.getLiveClassId());
        assertEquals("LEARNER", dto.getRole());
    }

    @Test
    void sessionResponseDTO_builder() {
        SessionResponseDTO dto = SessionResponseDTO.builder()
                .sessionId("s1").organizerId("o1").organizerName("Carol")
                .title("Session").status("SCHEDULED").roomId("r1")
                .sessionType("SESSION").actionType("CREATED")
                .isRecording(false).maxParticipants(30).build();
        assertEquals("s1", dto.getSessionId());
        assertEquals("SCHEDULED", dto.getStatus());
    }

    @Test
    void sessionJoinResponseDTO_builder() {
        SessionJoinResponseDTO dto = SessionJoinResponseDTO.builder()
                .sessionId("s1").roomId("r1").token("tok").role("TRAINER").build();
        assertEquals("s1", dto.getSessionId());
        assertEquals("TRAINER", dto.getRole());
    }

    @Test
    void enrollmentStatusResponse_getterSetter() {
        EnrollmentStatusResponse r = new EnrollmentStatusResponse();
        r.setEnrolled(true);
        assertTrue(r.isEnrolled());
    }

    @Test
    void sessionStatusResponse_getterSetter() {
        SessionStatusResponse r = new SessionStatusResponse();
        r.setEnrolled(true);
        assertTrue(r.isEnrolled());
    }
}
