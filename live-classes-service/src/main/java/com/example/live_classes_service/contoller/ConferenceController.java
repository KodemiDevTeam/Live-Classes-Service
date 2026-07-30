package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;
import com.example.live_classes_service.service.ConferenceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conferences")
@RequiredArgsConstructor
@Slf4j
public class ConferenceController {

    private final ConferenceService service;

    @PostMapping("/create")
    public ResponseEntity<ConferenceResponseDTO> createConference(
            @Valid @RequestBody CreateConferenceRequest request,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Create conference request received | title={} | scheduledAt={}",
                request.getTitle(), request.getScheduledAt());

        ConferenceResponseDTO response = service.createConference(request, token);

        log.info("Conference created successfully | conferenceId={}", response.getConferenceId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conferenceId}/start")
    public ResponseEntity<ConferenceResponseDTO> startConference(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Start conference request | conferenceId={}", conferenceId);

        ConferenceResponseDTO response = service.startConference(conferenceId, token);

        log.info("Conference started successfully | conferenceId={}", conferenceId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conferenceId}/join")
    public ResponseEntity<ConferenceJoinResponseDTO> joinConference(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Join conference request | conferenceId={}", conferenceId);

        ConferenceJoinResponseDTO response = service.joinConference(conferenceId, token);

        log.info("Conference join token generated | conferenceId={}", conferenceId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conferenceId}/recording/start")
    public ResponseEntity<String> startRecording(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Start recording request | conferenceId={}", conferenceId);

        String response = service.startRecording(conferenceId, token);

        log.info("Recording started | conferenceId is={}", conferenceId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conferenceId}/recording/stop")
    public ResponseEntity<String> stopRecording(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("Stop recording request | conferenceId={}", conferenceId);

        String response = service.stopRecording(conferenceId, token);

        log.info("Recording stopped | conferenceId={}", conferenceId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conferenceId}/end")
    public ResponseEntity<String> endConference(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        log.info("End conference request | conferenceId={}", conferenceId);


        String response = service.endConference(conferenceId, token);

        log.info("Conference ended | conferenceId={}", conferenceId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-all-ByOrganizer")
    public ResponseEntity<List<ConferenceResponseDTO>> getConferencesByOrganizer(
            @RequestHeader("Authorization") String token
    ) {
        log.info("Get all conferences by organizer request");

        List<ConferenceResponseDTO> response = service.getConferencesByOrganizer(token);

        log.info("Returning {} conferences for organizer", response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<ConferenceResponseDTO>> getAllConferences() {
        log.info("Get all conferences request");

        List<ConferenceResponseDTO> response = service.getAllConferences();

        log.info("Returning {} total conferences", response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-conference/{conferenceId}")
    public ResponseEntity<ConferenceResponseDTO> getConference(
            @PathVariable String conferenceId
    ) {
        log.info("Get conference request | conferenceId={}", conferenceId);

        ConferenceResponseDTO response = service.getConference(conferenceId);

        log.info("Conference found | conferenceId={}", conferenceId);

        return ResponseEntity.ok(response);
    }
}
