package com.example.live_classes_service.contoller;

import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.service.impl.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoSDKWebhookController.class)
class VideoSDKWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveClassRepository repository;

    @MockBean
    private S3Service s3Service;

    @Test
    void handleWebhook_recordingStopped_sessionFound() throws Exception {
        LiveClassEntity entity = LiveClassEntity.builder()
                .liveClassId("lc-001").roomId("room-001").build();

        when(repository.findByRoomId("room-001")).thenReturn(entity);
        when(s3Service.uploadRecording(anyString(), eq("lc-001"))).thenReturn("https://s3.amazonaws.com/recordings/lc-001.mp4");

        Map<String, Object> body = Map.of(
                "webhookType", "recording-stopped",
                "data", Map.of("roomId", "room-001", "fileUrl", "https://cdn.videosdk.live/rec.mp4")
        );

        mockMvc.perform(post("/webhooks/videosdk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));

        verify(repository).save(entity);
    }

    @Test
    void handleWebhook_recordingStopped_sessionNotFound() throws Exception {
        when(repository.findByRoomId("room-999")).thenReturn(null);

        Map<String, Object> body = Map.of(
                "webhookType", "recording-stopped",
                "data", Map.of("roomId", "room-999", "fileUrl", "https://cdn.videosdk.live/rec.mp4")
        );

        mockMvc.perform(post("/webhooks/videosdk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));

        verify(repository, never()).save(any());
    }

    @Test
    void handleWebhook_otherType_ignored() throws Exception {
        Map<String, Object> body = Map.of("webhookType", "participant-joined");

        mockMvc.perform(post("/webhooks/videosdk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));

        verify(repository, never()).findByRoomId(any());
    }

    @Test
    void handleWebhook_exception_returns500() throws Exception {
        when(repository.findByRoomId(any())).thenThrow(new RuntimeException("DB error"));

        Map<String, Object> body = Map.of(
                "webhookType", "recording-stopped",
                "data", Map.of("roomId", "room-001", "fileUrl", "https://cdn.videosdk.live/rec.mp4")
        );

        mockMvc.perform(post("/webhooks/videosdk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Webhook error"));
    }
}
