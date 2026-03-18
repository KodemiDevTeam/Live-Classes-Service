package com.example.live_classes_service.service.impl;

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

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 24 * 60 * 60 * 1000))
                .signWith(
                        Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();

        return token;
    }

    public String createRoom() {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiEndpoint + "/rooms", request, Map.class);

        Map<String, Object> body = response.getBody();

        String roomId = (String) body.get("roomId");

        log.info("VideoSDK room created: {}", roomId);

        return roomId;
    }

    public boolean validateRoom(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

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
        headers.set("Authorization", token);

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
                apiEndpoint + "/recordings/start", request, Map.class);

        Map<String, Object> responseBody = response.getBody();

        String recordingId = (String) responseBody.get("id");

        log.info("Recording started {}", recordingId);

        return recordingId;
    }

    public void stopRecording(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                apiEndpoint + "/recordings/stop", request, Map.class);

        log.info("Recording stop requested for room {}", roomId);
    }

    public void endRoom(String roomId) {

        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                apiEndpoint + "/rooms/deactivate", request, Map.class);

        log.info("Room ended {}", roomId);
    }
}