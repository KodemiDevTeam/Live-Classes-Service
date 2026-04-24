package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.SessionJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionResponseDTO;
import com.example.live_classes_service.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private SessionService service;

    private static final String TOKEN = "Bearer test-token";

    private SessionResponseDTO sampleResponse() {
        return SessionResponseDTO.builder()
                .sessionId("session-001").title("Test Session")
                .organizerId("org-001").status("SCHEDULED").build();
    }

    @Test
    void createSession_returns200() throws Exception {
        when(service.createSession(any(), any())).thenReturn(sampleResponse());
        CreateSessionRequest request = new CreateSessionRequest();
        request.setTitle("Test Session");
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-001"));
    }

    @Test
    void startSession_returns200() throws Exception {
        when(service.startSession(any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/api/v1/sessions/session-001/start")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-001"));
    }

    @Test
    void joinSession_returns200() throws Exception {
        SessionJoinResponseDTO joinResponse = SessionJoinResponseDTO.builder()
                .sessionId("session-001").roomId("room-001").token("sdk-token").role("TRAINER").build();
        when(service.joinSession(any(), any())).thenReturn(joinResponse);
        mockMvc.perform(get("/api/v1/sessions/session-001/join")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("sdk-token"));
    }

    @Test
    void startRecording_returns200() throws Exception {
        when(service.startRecording(any(), any())).thenReturn("Recording Started");
        mockMvc.perform(post("/api/v1/sessions/session-001/recording/start")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Recording Started"));
    }

    @Test
    void stopRecording_returns200() throws Exception {
        when(service.stopRecording(any(), any())).thenReturn("Recording Stopped");
        mockMvc.perform(post("/api/v1/sessions/session-001/recording/stop")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Recording Stopped"));
    }

    @Test
    void endSession_returns200() throws Exception {
        when(service.endSession(any(), any())).thenReturn("Session Ended");
        mockMvc.perform(post("/api/v1/sessions/session-001/end")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Session Ended"));
    }
}
