package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;
import com.example.live_classes_service.service.LiveClassService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LiveClassController.class)
class LiveClassControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private LiveClassService service;

    private static final String TOKEN = "Bearer test-token";

    private LiveClassResponseDTO sampleResponse() {
        return LiveClassResponseDTO.builder()
                .liveClassId("lc-001").title("Test Class")
                .courseId("course-001").trainerId("trainer-001").status("SCHEDULED").build();
    }

    @Test
    void createLiveClass_returns200() throws Exception {
        when(service.createLiveClass(any(), any())).thenReturn(sampleResponse());
        CreateLiveClassRequest request = new CreateLiveClassRequest();
        request.setTitle("Test Class");
        request.setCourseId("course-001");
        mockMvc.perform(post("/api/v1/live-classes/create")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveClassId").value("lc-001"))
                .andExpect(jsonPath("$.title").value("Test Class"));
    }

    @Test
    void startLiveClass_returns200() throws Exception {
        when(service.startLiveClass(any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/api/v1/live-classes/lc-001/start")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveClassId").value("lc-001"));
    }

    @Test
    void joinLiveClass_returns200() throws Exception {
        LiveClassJoinResponseDTO joinResponse = LiveClassJoinResponseDTO.builder()
                .liveClassId("lc-001").roomId("room-001").token("sdk-token").role("LEARNER").build();
        when(service.joinLiveClass(any(), any())).thenReturn(joinResponse);
        mockMvc.perform(get("/api/v1/live-classes/lc-001/join")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("sdk-token"))
                .andExpect(jsonPath("$.role").value("LEARNER"));
    }

    @Test
    void getLiveClassesByCourse_returns200() throws Exception {
        when(service.getLiveClassesByCourse(any())).thenReturn(List.of(sampleResponse()));
        mockMvc.perform(get("/api/v1/live-classes/course/course-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].liveClassId").value("lc-001"));
    }

    @Test
    void startRecording_returns200() throws Exception {
        when(service.startRecording(any(), any())).thenReturn("Recording Started");
        mockMvc.perform(post("/api/v1/live-classes/lc-001/recording/start")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Recording Started"));
    }

    @Test
    void stopRecording_returns200() throws Exception {
        when(service.stopRecording(any(), any())).thenReturn("Recording Stopped");
        mockMvc.perform(post("/api/v1/live-classes/lc-001/recording/stop")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Recording Stopped"));
    }

    @Test
    void endLiveClass_returns200() throws Exception {
        when(service.endLiveClass(any(), any())).thenReturn("Live Class Ended");
        mockMvc.perform(post("/api/v1/live-classes/lc-001/end")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Live Class Ended"));
    }
}
