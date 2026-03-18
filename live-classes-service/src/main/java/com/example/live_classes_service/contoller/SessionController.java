package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionResponseDTO;
import com.example.live_classes_service.service.SessionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public SessionResponseDTO createSession(
            @RequestBody CreateSessionRequest request,
            @RequestHeader("Authorization") String token
    ) {
        return service.createSession(request, token);
    }

    @PostMapping("/start/{sessionId}")
    public SessionResponseDTO startSession(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        return service.startSession(sessionId, token);
    }

    @GetMapping("/join/{sessionId}")
    public SessionJoinResponseDTO joinSession(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        return service.joinSession(sessionId, token);
    }

    @PostMapping("/recording/start/{sessionId}")
    public String startRecording(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        return service.startRecording(sessionId, token);
    }

    @PostMapping("/recording/stop/{sessionId}")
    public String stopRecording(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        return service.stopRecording(sessionId, token);
    }

    @PostMapping("/end/{sessionId}")
    public String endLiveClass(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String token
    ) {
        return service.endSession(sessionId, token);
    }
}