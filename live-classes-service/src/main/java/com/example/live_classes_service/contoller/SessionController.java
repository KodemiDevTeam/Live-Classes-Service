package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.SessionJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionResponseDTO;
import com.example.live_classes_service.service.SessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService service;

    @PostMapping
    public ResponseEntity<SessionResponseDTO> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Create session | title={} | scheduledAt={}",
                request.getTitle(), request.getScheduledAt());

        SessionResponseDTO response = service.createSession(request, token);

        log.info("Session created | sessionId={}", response.getSessionId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/start")
    public ResponseEntity<SessionResponseDTO> startSession(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Start session | sessionId={}", sessionId);

        SessionResponseDTO response = service.startSession(sessionId, token);

        log.info("Session started | sessionId={}", sessionId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}/join")
    public ResponseEntity<SessionJoinResponseDTO> joinSession(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Join session | sessionId={}", sessionId);

        SessionJoinResponseDTO response = service.joinSession(sessionId, token);

        log.info("Join token generated | sessionId={}", sessionId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/recording/start")
    public ResponseEntity<String> startRecording(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Start recording | sessionId={}", sessionId);

        String response = service.startRecording(sessionId, token);

        log.info("Recording started | sessionId={}", sessionId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/recording/stop")
    public ResponseEntity<String> stopRecording(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Stop recording | sessionId={}", sessionId);

        String response = service.stopRecording(sessionId, token);

        log.info("Recording stopped | sessionId={}", sessionId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<String> endSession(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("End session | sessionId={}", sessionId);

        String response = service.endSession(sessionId, token);

        log.info("Session ended | sessionId={}", sessionId);

        return ResponseEntity.ok(response);
    }
}