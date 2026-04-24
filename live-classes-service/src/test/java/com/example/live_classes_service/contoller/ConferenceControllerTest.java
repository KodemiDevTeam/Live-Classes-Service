package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;
import com.example.live_classes_service.service.ConferenceService;
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

@WebMvcTest(ConferenceController.class)
class ConferenceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ConferenceService service;

    private static final String TOKEN = "Bearer test-token";

    private ConferenceResponseDTO sampleResponse() {
        return ConferenceResponseDTO.builder()
                .conferenceId("conf-001").title("Test Conference")
                .organizerId("org-001").status("SCHEDULED").build();
    }

    @Test
    void createConference_returns200() throws Exception {
        when(service.createConference(any(), any())).thenReturn(sampleResponse());
        CreateConferenceRequest request = new CreateConferenceRequest();
        request.setTitle("Test Conference");
        mockMvc.perform(post("/api/v1/conferences/create")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conferenceId").value("conf-001"));
    }

    @Test
    void startConference_returns200() throws Exception {
        when(service.startConference(any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/api/v1/conferences/conf-001/start")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conferenceId").value("conf-001"));
    }

    @Test
    void joinConference_returns200() throws Exception {
        ConferenceJoinResponseDTO joinResponse = ConferenceJoinResponseDTO.builder()
                .conferenceId("conf-001").roomId("room-001").token("sdk-token").role("TRAINER").build();
        when(service.joinConference(any(), any())).thenReturn(joinResponse);
        mockMvc.perform(get("/api/v1/conferences/conf-001/join")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("sdk-token"));
    }

    @Test
    void startRecording_returns200() throws Exception {
        when(service.startRecording(any(), any())).thenReturn("Recording Started");
        mockMvc.perform(post("/api/v1/conferences/conf-001/recording/start")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Recording Started"));
    }

    @Test
    void stopRecording_returns200() throws Exception {
        when(service.stopRecording(any(), any())).thenReturn("Recording Stopped");
        mockMvc.perform(post("/api/v1/conferences/conf-001/recording/stop")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Recording Stopped"));
    }

    @Test
    void endConference_returns200() throws Exception {
        when(service.endConference(any(), any())).thenReturn("Conference Ended");
        mockMvc.perform(post("/api/v1/conferences/conf-001/end")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Conference Ended"));
    }
}
