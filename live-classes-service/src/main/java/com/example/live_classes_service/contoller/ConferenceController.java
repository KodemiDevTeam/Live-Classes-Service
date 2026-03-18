package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;
import com.example.live_classes_service.service.ConferenceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conference")
public class ConferenceController {

    private final ConferenceService service;

    public ConferenceController(ConferenceService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ConferenceResponseDTO createConference(
            @RequestBody CreateConferenceRequest request,
            @RequestHeader("Authorization") String token
    ) {
        return service.createConference(request, token);
    }

    @PostMapping("/start/{conferenceId}")
    public ConferenceResponseDTO startConference(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        return service.startConference(conferenceId, token);
    }

    @GetMapping("/join/{conferenceId}")
    public ConferenceJoinResponseDTO joinConference(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        return service.joinConference(conferenceId, token);
    }

    @PostMapping("/start/recording//{conferenceId}")
    public String startRecording(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        return service.startRecording(conferenceId, token);
    }

    @PostMapping("stop/recording/{conferenceId}")
    public String stopRecording(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        return service.stopRecording(conferenceId, token);
    }
    @PostMapping("/end/{conferenceId}")
    public String endLiveClass(
            @PathVariable String conferenceId,
            @RequestHeader("Authorization") String token
    ) {
        return service.endConference(conferenceId, token);
    }
}