package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.response.LiveSessionResponseDTO;
import com.example.live_classes_service.model.ConferenceEntity;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.model.SessionEntity;
import com.example.live_classes_service.repository.ConferenceRepository;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LiveClassInternalController.class)
class LiveClassInternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private LiveClassRepository liveClassRepository;
    @MockBean private SessionRepository sessionRepository;
    @MockBean private ConferenceRepository conferenceRepository;

    private static final String SESSION_ID = "session-001";
    private static final String COURSE_ID = "course-001";

    private LiveClassEntity liveClassEntity;
    private SessionEntity sessionEntity;
    private ConferenceEntity conferenceEntity;

    @BeforeEach
    void setUp() {
        liveClassEntity = LiveClassEntity.builder()
                .liveClassId(SESSION_ID)
                .courseId(COURSE_ID)
                .title("Test Live Class")
                .status("SCHEDULED")
                .scheduledAt("2024-01-15T10:00:00Z")
                .startedAt(null)
                .endedAt(null)
                .build();

        sessionEntity = SessionEntity.builder()
                .sessionId("sess-001")
                .title("Test Session")
                .status("SCHEDULED")
                .scheduledAt("2024-01-15T10:00:00Z")
                .startedAt(null)
                .endedAt(null)
                .build();

        conferenceEntity = ConferenceEntity.builder()
                .conferenceId("conf-001")
                .title("Test Conference")
                .status("SCHEDULED")
                .scheduledAt("2024-01-15T10:00:00Z")
                .startedAt(null)
                .endedAt(null)
                .build();
    }

    @Test
    void getSessionById_withLiveClass_success() throws Exception {
        when(liveClassRepository.findById(SESSION_ID)).thenReturn(liveClassEntity);

        mockMvc.perform(get("/internal/session/{sessionId}", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.courseId").value(COURSE_ID))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.joinLink").value("/api/v1/live-classes/" + SESSION_ID + "/join"));

        verify(liveClassRepository).findById(SESSION_ID);
        verify(sessionRepository, never()).findById(anyString());
        verify(conferenceRepository, never()).findById(anyString());
    }

    @Test
    void getSessionById_withSession_success() throws Exception {
        when(liveClassRepository.findById(SESSION_ID)).thenReturn(null);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(sessionEntity);

        mockMvc.perform(get("/internal/session/{sessionId}", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-001"))
                .andExpect(jsonPath("$.courseId").doesNotExist())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.joinLink").value("/api/v1/sessions/sess-001/join"));

        verify(liveClassRepository).findById(SESSION_ID);
        verify(sessionRepository).findById(SESSION_ID);
        verify(conferenceRepository, never()).findById(anyString());
    }

    @Test
    void getSessionById_withConference_success() throws Exception {
        when(liveClassRepository.findById(SESSION_ID)).thenReturn(null);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(null);
        when(conferenceRepository.findById(SESSION_ID)).thenReturn(conferenceEntity);

        mockMvc.perform(get("/internal/session/{sessionId}", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("conf-001"))
                .andExpect(jsonPath("$.courseId").doesNotExist())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.joinLink").value("/api/v1/conferences/conf-001/join"));

        verify(liveClassRepository).findById(SESSION_ID);
        verify(sessionRepository).findById(SESSION_ID);
        verify(conferenceRepository).findById(SESSION_ID);
    }

    @Test
    void getSessionById_notFound_returns404() throws Exception {
        when(liveClassRepository.findById(SESSION_ID)).thenReturn(null);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(null);
        when(conferenceRepository.findById(SESSION_ID)).thenReturn(null);

        mockMvc.perform(get("/internal/session/{sessionId}", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(liveClassRepository).findById(SESSION_ID);
        verify(sessionRepository).findById(SESSION_ID);
        verify(conferenceRepository).findById(SESSION_ID);
    }

    @Test
    void getSessionById_withStartedTime_returnsStartedTime() throws Exception {
        liveClassEntity.setStartedAt("2024-01-15T10:05:00Z");
        when(liveClassRepository.findById(SESSION_ID)).thenReturn(liveClassEntity);

        mockMvc.perform(get("/internal/session/{sessionId}", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("2024-01-15T10:05:00Z"));

        verify(liveClassRepository).findById(SESSION_ID);
    }

    @Test
    void getSessionById_withEndTime_returnsEndTime() throws Exception {
        liveClassEntity.setEndedAt("2024-01-15T11:05:00Z");
        when(liveClassRepository.findById(SESSION_ID)).thenReturn(liveClassEntity);

        mockMvc.perform(get("/internal/session/{sessionId}", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime").value("2024-01-15T11:05:00Z"));

        verify(liveClassRepository).findById(SESSION_ID);
    }

    @Test
    void getSessionsByCourse_success() throws Exception {
        LiveClassEntity liveClass2 = LiveClassEntity.builder()
                .liveClassId("session-002")
                .courseId(COURSE_ID)
                .title("Second Live Class")
                .status("ENDED")
                .scheduledAt("2024-01-14T10:00:00Z")
                .endedAt("2024-01-14T11:00:00Z")
                .build();

        List<LiveClassEntity> liveClasses = Arrays.asList(liveClassEntity, liveClass2);
        when(liveClassRepository.findByCourseId(COURSE_ID)).thenReturn(liveClasses);

        mockMvc.perform(get("/internal/session/by-course/{courseId}", COURSE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$[0].courseId").value(COURSE_ID))
                .andExpect(jsonPath("$[1].sessionId").value("session-002"))
                .andExpect(jsonPath("$[1].status").value("ENDED"));

        verify(liveClassRepository).findByCourseId(COURSE_ID);
    }

    @Test
    void getSessionsByCourse_empty() throws Exception {
        when(liveClassRepository.findByCourseId(COURSE_ID)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/internal/session/by-course/{courseId}", COURSE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(liveClassRepository).findByCourseId(COURSE_ID);
    }

    @Test
    void getSessionsByCourse_null() throws Exception {
        when(liveClassRepository.findByCourseId(COURSE_ID)).thenReturn(null);

        mockMvc.perform(get("/internal/session/by-course/{courseId}", COURSE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(liveClassRepository).findByCourseId(COURSE_ID);
    }

    @Test
    void getSessionsByCourse_multipleWithDifferentStatuses() throws Exception {
        LiveClassEntity scheduled = LiveClassEntity.builder()
                .liveClassId("session-001")
                .courseId(COURSE_ID)
                .title("Scheduled Class")
                .status("SCHEDULED")
                .scheduledAt("2024-01-15T10:00:00Z")
                .build();

        LiveClassEntity started = LiveClassEntity.builder()
                .liveClassId("session-002")
                .courseId(COURSE_ID)
                .title("Started Class")
                .status("STARTED")
                .scheduledAt("2024-01-14T10:00:00Z")
                .startedAt("2024-01-14T10:05:00Z")
                .build();

        LiveClassEntity ended = LiveClassEntity.builder()
                .liveClassId("session-003")
                .courseId(COURSE_ID)
                .title("Ended Class")
                .status("ENDED")
                .scheduledAt("2024-01-13T10:00:00Z")
                .startedAt("2024-01-13T10:05:00Z")
                .endedAt("2024-01-13T11:00:00Z")
                .build();

        List<LiveClassEntity> liveClasses = Arrays.asList(scheduled, started, ended);
        when(liveClassRepository.findByCourseId(COURSE_ID)).thenReturn(liveClasses);

        mockMvc.perform(get("/internal/session/by-course/{courseId}", COURSE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[1].status").value("STARTED"))
                .andExpect(jsonPath("$[2].status").value("ENDED"));
    }
}
