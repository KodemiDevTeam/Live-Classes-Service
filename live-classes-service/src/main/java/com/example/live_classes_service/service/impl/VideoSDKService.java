package com.example.live_classes_service.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;



@Slf4j
@Service
public class VideoSDKService {

    private static final String AUTHORIZATION = "Authorization";
    private static final String ROOM_ID = "roomId";

    @Value("${videosdk.api.key}")
    private String apiKey;

    @Value("${videosdk.api.secret}")
    private String apiSecret;

    @Value("${videosdk.api.endpoint}")
    private String apiEndpoint;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    @Value("${aws.s3.region}")
    private String s3Region;

    @Value("${aws.s3.accessKey}")
    private String s3AccessKey;

    @Value("${aws.s3.secretKey}")
    private String s3SecretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateToken() {

        long now = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("apikey", apiKey);
        claims.put("permissions", List.of("allow_join", "allow_mod"));
        claims.put("version", 2);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 24 * 60 * 60 * 1000))
                .signWith(
                        Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    public String createRoom() {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AUTHORIZATION, token);

        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiEndpoint + "/rooms", HttpMethod.POST, request,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        Map<String, Object> body = response.getBody();

        if (body == null) {
            throw new IllegalStateException("Empty response body from VideoSDK createRoom");
        }

        String roomId = (String) body.get(ROOM_ID);

        log.info("VideoSDK room created: {}", roomId);

        return roomId;
    }

    public boolean validateRoom(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {

            restTemplate.exchange(
                    apiEndpoint + "/rooms/validate/" + roomId,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            log.info("Room validated {}", roomId);
            return true;

        } catch (Exception e) {

            log.error("Room validation failed {}", roomId);
            return false;
        }
    }

    public String startRecording(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AUTHORIZATION, token);

        // S3 storage config must be flat inside "storage", not nested under "config"
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "s3");
        storageConfig.put("accessKey", s3AccessKey);
        storageConfig.put("secretKey", s3SecretKey);
        storageConfig.put("bucket", s3Bucket);
        storageConfig.put("region", s3Region);

        Map<String, Object> config = new HashMap<>();
        config.put("storage", storageConfig);

        // VideoSDK requires layout config
        Map<String, Object> layoutConfig = new HashMap<>();
        layoutConfig.put("type", "GRID");
        layoutConfig.put("priority", "SPEAKER");
        layoutConfig.put("gridSize", 4);
        config.put("layout", layoutConfig);

        Map<String, Object> body = new HashMap<>();
        body.put(ROOM_ID, roomId);
        body.put("config", config);

        log.info("Starting recording for roomId={} with bucket={} region={}", roomId, s3Bucket, s3Region);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // Use String response to avoid deserialization issues with VideoSDK response format
            ResponseEntity<String> response = restTemplate.exchange(
                    apiEndpoint + "/recordings/start", HttpMethod.POST, request,
                    String.class);

            String responseBody = response.getBody();
            log.info("VideoSDK startRecording raw response | roomId={} | body={}", roomId, responseBody);

            if (responseBody == null) {
                throw new IllegalStateException("Empty response body from VideoSDK startRecording");
            }

            // Parse the recording ID from JSON response
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(responseBody);
            String recordingId = jsonNode.has("id") ? jsonNode.get("id").asText() : "unknown";

            log.info("Recording started | recordingId={} | roomId={}", recordingId, roomId);

            return recordingId;
        } catch (Exception e) {
            log.error("VideoSDK startRecording failed | roomId={} | error={}", roomId, e.getMessage(), e);
            throw new RuntimeException("Failed to start recording: " + e.getMessage(), e);
        }
    }

    public void stopRecording(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AUTHORIZATION, token);

        Map<String, Object> body = new HashMap<>();
        body.put(ROOM_ID, roomId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Stopping recording | roomId={}", roomId);

        try {
            restTemplate.postForEntity(
                    apiEndpoint + "/recordings/end", request, String.class);
            log.info("Recording stopped | roomId={}", roomId);
        } catch (Exception e) {
            log.error("VideoSDK stopRecording failed | roomId={} | error={}", roomId, e.getMessage(), e);
            throw new RuntimeException("Failed to stop recording: " + e.getMessage(), e);
        }
    }

    public void endRoom(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AUTHORIZATION, token);

        Map<String, Object> body = new HashMap<>();
        body.put(ROOM_ID, roomId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                apiEndpoint + "/rooms/deactivate", request, Map.class);

        log.info("Room ended {}", roomId);
    }
}