package com.example.live_classes_service.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void conferenceEntity_builderAndGetters() {
        ConferenceEntity e = ConferenceEntity.builder()
                .conferenceId("c1").organizerId("o1").organizerName("Alice")
                .title("Conf").description("Desc").roomId("r1")
                .status("SCHEDULED").sessionType("SESSION").actionType("CREATED")
                .isRecording(false).maxParticipants(50).createdAt("2026-01-01")
                .scheduledAt("2026-05-01").build();

        assertEquals("c1", e.getConferenceId());
        assertEquals("o1", e.getOrganizerId());
        assertEquals("Alice", e.getOrganizerName());
        assertEquals("Conf", e.getTitle());
        assertEquals("r1", e.getRoomId());
        assertEquals("SCHEDULED", e.getStatus());
        assertFalse(e.getIsRecording());
        assertEquals(50, e.getMaxParticipants());
    }

    @Test
    void conferenceEntity_setters() {
        ConferenceEntity e = new ConferenceEntity();
        e.setConferenceId("c2");
        e.setStatus("STARTED");
        e.setIsRecording(true);
        e.setRecordingUrl("https://s3.example.com/rec.mp4");
        e.setStartedAt("2026-05-01T10:00:00Z");
        e.setEndedAt("2026-05-01T11:00:00Z");

        assertEquals("c2", e.getConferenceId());
        assertEquals("STARTED", e.getStatus());
        assertTrue(e.getIsRecording());
        assertEquals("https://s3.example.com/rec.mp4", e.getRecordingUrl());
    }

    @Test
    void liveClassEntity_builderAndGetters() {
        LiveClassEntity e = LiveClassEntity.builder()
                .liveClassId("lc1").courseId("course1").trainerId("t1")
                .trainerName("Bob").title("Class").description("Desc")
                .roomId("r1").status("SCHEDULED").sessionType("LIVE_CLASS")
                .actionType("CREATED").isRecording(false).maxParticipants(30)
                .createdAt("2026-01-01").scheduledAt("2026-05-01").build();

        assertEquals("lc1", e.getLiveClassId());
        assertEquals("course1", e.getCourseId());
        assertEquals("t1", e.getTrainerId());
        assertEquals("Bob", e.getTrainerName());
        assertEquals("r1", e.getRoomId());
        assertFalse(e.getIsRecording());
    }

    @Test
    void liveClassEntity_setters() {
        LiveClassEntity e = new LiveClassEntity();
        e.setLiveClassId("lc2");
        e.setStatus("LIVE_STARTED");
        e.setIsRecording(true);
        e.setRecordingUrl("https://s3.example.com/lc.mp4");
        e.setStartedAt("2026-05-01T10:00:00Z");
        e.setEndedAt("2026-05-01T11:00:00Z");

        assertEquals("lc2", e.getLiveClassId());
        assertEquals("LIVE_STARTED", e.getStatus());
        assertTrue(e.getIsRecording());
    }

    @Test
    void sessionEntity_builderAndGetters() {
        SessionEntity e = SessionEntity.builder()
                .sessionId("s1").organizerId("o1").organizerName("Carol")
                .title("Session").description("Desc").roomId("r1")
                .status("SCHEDULED").sessionType("SESSION").actionType("CREATED")
                .isRecording(false).maxParticipants(20).createdAt("2026-01-01")
                .scheduledAt("2026-05-01").build();

        assertEquals("s1", e.getSessionId());
        assertEquals("o1", e.getOrganizerId());
        assertEquals("Carol", e.getOrganizerName());
        assertEquals("r1", e.getRoomId());
        assertFalse(e.getIsRecording());
    }

    @Test
    void sessionEntity_setters() {
        SessionEntity e = new SessionEntity();
        e.setSessionId("s2");
        e.setStatus("SESSION_STARTED");
        e.setIsRecording(true);
        e.setRecordingUrl("https://s3.example.com/session.mp4");
        e.setStartedAt("2026-05-01T10:00:00Z");
        e.setEndedAt("2026-05-01T11:00:00Z");

        assertEquals("s2", e.getSessionId());
        assertEquals("SESSION_STARTED", e.getStatus());
        assertTrue(e.getIsRecording());
    }
}
