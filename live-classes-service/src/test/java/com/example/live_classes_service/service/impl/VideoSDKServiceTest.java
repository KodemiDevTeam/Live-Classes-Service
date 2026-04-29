package com.example.live_classes_service.service.impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoSDKServiceTest {

    @Mock private RestTemplate restTemplate;
    @InjectMocks private VideoSDKService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiSecret", "test-secret-key-for-jwt-signing-minimum-256-bits");
        ReflectionTestUtils.setField(service, "apiEndpoint", "https://api.test.com");
        ReflectionTestUtils.setField(service, "s3Bucket", "test-bucket");
        ReflectionTestUtils.setField(service, "s3Region", "us-east-1");
        ReflectionTestUtils.setField(service, "s3AccessKey", "test-access");
        ReflectionTestUtils.setField(service, "s3SecretKey", "test-secret");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = service.generateToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createRoom_success() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("roomId", "room-123"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(response);
        assertEquals("room-123", service.createRoom());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createRoom_nullBody_throwsIllegalState() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(response);
        assertThrows(IllegalStateException.class, () -> service.createRoom());
    }

    @Test
    void validateRoom_success() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
        assertTrue(service.validateRoom("room-123"));
    }

    @Test
    void validateRoom_apiError_returnsFalse() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("API error"));
        assertFalse(service.validateRoom("room-123"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void startRecording_success() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("id", "rec-123"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(response);
        assertEquals("rec-123", service.startRecording("room-123"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void startRecording_nullBody_throwsIllegalState() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(response);
        assertThrows(IllegalStateException.class, () -> service.startRecording("room-123"));
    }

    @Test
    void stopRecording_success() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
        assertDoesNotThrow(() -> service.stopRecording("room-123"));
    }

    @Test
    void endRoom_success() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
        assertDoesNotThrow(() -> service.endRoom("room-123"));
    }
}
