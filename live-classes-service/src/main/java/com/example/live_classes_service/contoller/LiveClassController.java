package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;
import com.example.live_classes_service.service.LiveClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/live-classes")
@RequiredArgsConstructor
@Slf4j
public class LiveClassController {

    private final LiveClassService service;

    @PostMapping
    public ResponseEntity<LiveClassResponseDTO> createLiveClass(
            @Valid @RequestBody CreateLiveClassRequest request,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Create live class request | courseId={} | title={}",
                request.getCourseId(), request.getTitle());

        LiveClassResponseDTO response = service.createLiveClass(request, token);

        log.info("Live class created | liveClassId={}", response.getLiveClassId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{liveClassId}/start")
    public ResponseEntity<LiveClassResponseDTO> startLiveClass(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Start live class | liveClassId={}", liveClassId);

        LiveClassResponseDTO response = service.startLiveClass(liveClassId, token);

        log.info("Live class started | liveClassId={}", liveClassId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{liveClassId}/join")
    public ResponseEntity<LiveClassJoinResponseDTO> joinLiveClass(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Join live class | liveClassId={}", liveClassId);

        LiveClassJoinResponseDTO response = service.joinLiveClass(liveClassId, token);

        log.info("Join token generated | liveClassId={}", liveClassId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LiveClassResponseDTO>> getLiveClassesByCourse(
            @PathVariable String courseId
    ) {
        log.info("Fetch live classes by course | courseId={}", courseId);

        List<LiveClassResponseDTO> response = service.getLiveClassesByCourse(courseId);

        log.info("Live classes fetched | courseId={} | count={}", courseId, response.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{liveClassId}/recording/start")
    public ResponseEntity<String> startRecording(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Start recording | liveClassId={}", liveClassId);

        String response = service.startRecording(liveClassId, token);

        log.info("Recording started | liveClassId={}", liveClassId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{liveClassId}/recording/stop")
    public ResponseEntity<String> stopRecording(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Stop recording | liveClassId={}", liveClassId);

        String response = service.stopRecording(liveClassId, token);

        log.info("Recording stopped | liveClassId={}", liveClassId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{liveClassId}/end")
    public ResponseEntity<String> endLiveClass(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("End live class | liveClassId={}", liveClassId);

        String response = service.endLiveClass(liveClassId, token);

        log.info("Live class ended | liveClassId={}", liveClassId);

        return ResponseEntity.ok(response);
    }
}