package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;
import com.example.live_classes_service.service.LiveClassService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/live-class")
public class LiveClassController {

    private final LiveClassService service;

    public LiveClassController(LiveClassService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public LiveClassResponseDTO createLiveClass(
            @RequestBody CreateLiveClassRequest request,
            @RequestHeader("Authorization") String token
    ) {

        return service.createLiveClass(request, token);
    }

    @PostMapping("/start/{liveClassId}")
    public LiveClassResponseDTO startLiveClass(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        return service.startLiveClass(liveClassId, token);
    }

    @GetMapping("/join/{liveClassId}")
    public LiveClassJoinResponseDTO joinLiveClass(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        return service.joinLiveClass(liveClassId, token);
    }

    @GetMapping("/course/{courseId}")
    public List<LiveClassResponseDTO> getLiveClassesByCourse(
            @PathVariable String courseId
    ) {
        return service.getLiveClassesByCourse(courseId);
    }

    @PostMapping("start/recording/{liveClassId}")
    public String startRecording(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        return service.startRecording(liveClassId, token);
    }

    @PostMapping("/stop/recording/{liveClassId}")
    public String stopRecording(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        return service.stopRecording(liveClassId, token);
    }
    @PostMapping("/end/{liveClassId}")
    public String endLiveClass(
            @PathVariable String liveClassId,
            @RequestHeader("Authorization") String token
    ) {
        return service.endLiveClass(liveClassId, token);
    }
}