package com.example.live_classes_service.contoller;

import com.example.live_classes_service.dto.response.LiveSessionResponseDTO;
import com.example.live_classes_service.model.ConferenceEntity;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.model.SessionEntity;
import com.example.live_classes_service.repository.ConferenceRepository;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/internal/session")
@RequiredArgsConstructor
@Slf4j
public class LiveClassInternalController {

    private final LiveClassRepository liveClassRepository;
    private final SessionRepository sessionRepository;
    private final ConferenceRepository conferenceRepository;

    @GetMapping("/{sessionId}")
    public ResponseEntity<LiveSessionResponseDTO> getSessionById(@PathVariable String sessionId) {
        log.info("Internal request to fetch session by id: {}", sessionId);

        // 1. Try LiveClass
        LiveClassEntity liveClass = liveClassRepository.findById(sessionId);
        if (liveClass != null) {
            return ResponseEntity.ok(LiveSessionResponseDTO.builder()
                    .sessionId(liveClass.getLiveClassId())
                    .courseId(liveClass.getCourseId())
                    .startTime(liveClass.getStartedAt() != null ? liveClass.getStartedAt() : liveClass.getScheduledAt())
                    .endTime(liveClass.getEndedAt())
                    .joinLink("/api/v1/live-classes/" + liveClass.getLiveClassId() + "/join")
                    .status(liveClass.getStatus())
                    .build());
        }

        // 2. Try Session
        SessionEntity session = sessionRepository.findById(sessionId);
        if (session != null) {
            return ResponseEntity.ok(LiveSessionResponseDTO.builder()
                    .sessionId(session.getSessionId())
                    .courseId(null)
                    .startTime(session.getStartedAt() != null ? session.getStartedAt() : session.getScheduledAt())
                    .endTime(session.getEndedAt())
                    .joinLink("/api/v1/sessions/" + session.getSessionId() + "/join")
                    .status(session.getStatus())
                    .build());
        }

        // 3. Try Conference
        ConferenceEntity conference = conferenceRepository.findById(sessionId);
        if (conference != null) {
            return ResponseEntity.ok(LiveSessionResponseDTO.builder()
                    .sessionId(conference.getConferenceId())
                    .courseId(null)
                    .startTime(conference.getStartedAt() != null ? conference.getStartedAt() : conference.getScheduledAt())
                    .endTime(conference.getEndedAt())
                    .joinLink("/api/v1/conferences/" + conference.getConferenceId() + "/join")
                    .status(conference.getStatus())
                    .build());
        }

        log.warn("Session with id {} not found in LiveClass, Session, or Conference", sessionId);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<List<LiveSessionResponseDTO>> getSessionsByCourse(@PathVariable String courseId) {
        log.info("Internal request to fetch sessions by course: {}", courseId);
        List<LiveClassEntity> liveClasses = liveClassRepository.findByCourseId(courseId);
        List<LiveSessionResponseDTO> response = new ArrayList<>();

        if (liveClasses != null) {
            for (LiveClassEntity lc : liveClasses) {
                response.add(LiveSessionResponseDTO.builder()
                        .sessionId(lc.getLiveClassId())
                        .courseId(lc.getCourseId())
                        .startTime(lc.getStartedAt() != null ? lc.getStartedAt() : lc.getScheduledAt())
                        .endTime(lc.getEndedAt())
                        .joinLink("/api/v1/live-classes/" + lc.getLiveClassId() + "/join")
                        .status(lc.getStatus())
                        .build());
            }
        }
        return ResponseEntity.ok(response);
    }
}
