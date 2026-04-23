package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.exception.NullBodyException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class VideoSDKService {

    private static final String RECORDINGS_START_PATH = "/recordings/start";
    private static final String RECORDINGS_STOP_PATH = "/recordings/stop";
    private static final String ROOMS_PATH = "/rooms";
    private static final String ROOMS_DEACTIVATE_PATH = "/rooms/deactivate";
    private static final String ROOMS_VALIDATE_PATH = "/rooms/validate/";
    private static final String AUTHORIZATION_HEADER = "Authorization";

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
        headers.set(AUTHORIZATION_HEADER, token);

        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiEndpoint + ROOMS_PATH, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new NullBodyException("VideoSDK createRoom returned null body");
        }

        String roomId = (String) body.get("roomId");

        log.info("VideoSDK room created: {}", roomId);

        return roomId;
    }

    public boolean validateRoom(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {

            restTemplate.exchange(
                    apiEndpoint + ROOMS_VALIDATE_PATH + roomId,
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
        headers.set(AUTHORIZATION_HEADER, token);

        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "s3");

        Map<String, String> s3Config = new HashMap<>();
        s3Config.put("accessKey", s3AccessKey);
        s3Config.put("secretKey", s3SecretKey);
        s3Config.put("bucket", s3Bucket);
        s3Config.put("region", s3Region);

        storageConfig.put("config", s3Config);

        Map<String, Object> config = new HashMap<>();
        config.put("storage", storageConfig);

        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);
        body.put("config", config);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiEndpoint + RECORDINGS_START_PATH, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new NullBodyException("VideoSDK startRecording returned null body");
        }

        String recordingId = (String) responseBody.get("id");

        log.info("Recording started {}", recordingId);

        return recordingId;
    }

    public void stopRecording(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AUTHORIZATION_HEADER, token);

        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                apiEndpoint + RECORDINGS_STOP_PATH, request, Map.class);

        log.info("Recording stop requested for room {}", roomId);
    }

    public void endRoom(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(AUTHORIZATION_HEADER, token);

        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                apiEndpoint + ROOMS_DEACTIVATE_PATH, request, Map.class);

        log.info("Room ended {}", roomId);
    }
}