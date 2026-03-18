package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.SessionJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.SessionEntity;
import com.example.live_classes_service.repository.SessionRepository;
import com.example.live_classes_service.service.SessionService;
import com.example.live_classes_service.util.JwtUtil;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {

    private final SessionRepository repository;
    private final JwtUtil jwtUtil;
    private final VideoSDKService videoSDKService;
    private final EnrollmentClient enrollmentClient;

    public SessionServiceImpl(
            SessionRepository repository,
            JwtUtil jwtUtil,
            VideoSDKService videoSDKService, EnrollmentClient enrollmentClient
    ) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.videoSDKService = videoSDKService;
        this.enrollmentClient = enrollmentClient;
    }

    @Override
    public SessionResponseDTO createSession(CreateSessionRequest request, String token) {

        String Role = jwtUtil.extractRole(token);

        if (!"TRAINER".equals(Role)){
            throw new RuntimeException("Only trainer can create Sessions ");
        }
        String organizerId = jwtUtil.extractUserId(token);
        String sessionId = UUID.randomUUID().toString();
        String organizerName = jwtUtil.extractName(token);
        String roomId = videoSDKService.createRoom();

        SessionEntity entity = SessionEntity.builder()
                .sessionId(sessionId)
                .organizerId(organizerId)
                .organizerName(organizerName)
                .roomId(roomId)
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .actionType("CREATED")
                .status("SCHEDULED")
                .sessionType("SESSION")
                .maxParticipants(request.getMaxParticipants())
                .isRecording(false)
                .createdAt(Instant.now().toString())
                .build();

        repository.save(entity);

        return SessionResponseDTO.builder()
                .sessionId(sessionId)
                .organizerId(entity.getOrganizerId())
                .organizerName(entity.getOrganizerName())
                .roomId(roomId)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .actionType(entity.getActionType())
                .sessionType(entity.getSessionType())
                .status(entity.getStatus())
                .scheduledAt(entity.getScheduledAt())
                .createdAt(entity.getCreatedAt())
                .maxParticipants(entity.getMaxParticipants())
                .build();
    }

    @Override
    public SessionResponseDTO startSession(String sessionId, String token) {

        SessionEntity entity = repository.findById(sessionId);

        String organizerId = jwtUtil.extractUserId(token);
        String organizerName = jwtUtil.extractName(token);


        if (!entity.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("Only creator can start session");
        }
        if ("SESSION_STARTED".equals(entity.getStatus())) {
            throw new RuntimeException("Conference is already started");
        }

        if ("ENDED".equals(entity.getStatus())) {
            throw new RuntimeException("Conference has already ended");
        }

        entity.setActionType("STARTED");
        entity.setStatus("SESSION_STARTED");
        entity.setStartedAt(Instant.now().toString());

        repository.save(entity);

        return SessionResponseDTO.builder()
                .sessionId(entity.getSessionId())
                .sessionType(entity.getSessionType())
                .description(entity.getDescription())
                .startedAt(entity.getStartedAt())
                .maxParticipants(entity.getMaxParticipants())
                .roomId(entity.getRoomId())
                .organizerId(organizerId)
                .organizerName(organizerName)
                .title(entity.getTitle())
                .status(entity.getStatus())
                .actionType(entity.getActionType())
                .isRecording(entity.getIsRecording())
                .build();
    }

    @Override
    public SessionJoinResponseDTO joinSession(String sessionId, String token) {

        SessionEntity entity = repository.findById(sessionId);
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        if (role.equals("TRAINER")) {
            if (!entity.getOrganizerId().equals(userId)) {
                throw new RuntimeException("You are not the Organizer of this Session");
            }
        }
        else if (role.equals("LEARNER")) {
            try {
                SessionStatusResponse status =
                        enrollmentClient.getSessionEnrollmentStatus(entity.getSessionId(), token);
                if (!status.isEnrolled()) {
                    throw new RuntimeException("User not enrolled in this session");
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("User not enrolled in this session or feign Client Issue ");
            }
        }
        else {
            throw new RuntimeException("Unauthorized role: " + role);
        }

        String videoToken = videoSDKService.generateToken();

        return SessionJoinResponseDTO.builder()
                .sessionId(entity.getSessionId())
                .roomId(entity.getRoomId())
                .token(videoToken)
                .role(role)
                .build();
    }

    @Override
    public String startRecording(String sessionId, String token) {

        SessionEntity entity = repository.findById(sessionId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Only Organizer can start recording");
        }

        String recordingId = videoSDKService.startRecording(entity.getRoomId());

        entity.setIsRecording(true);
        entity.setRecordingUrl(recordingId);

        repository.save(entity);

        return "Recording Started";
    }

    @Override
    public String stopRecording(String sessionId, String token) {

        SessionEntity entity = repository.findById(sessionId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Only Organizer can stop recording");
        }

        videoSDKService.stopRecording(entity.getRoomId());

        entity.setIsRecording(false);

        repository.save(entity);

        return "Recording Stopped";
    }

    public String endSession(String sessionId, String token) {

        SessionEntity entity = repository.findById(sessionId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Only creator can end session");
        }

        videoSDKService.endRoom(entity.getRoomId());

        entity.setStatus("ENDED");
        entity.setEndedAt(Instant.now().toString());

        repository.save(entity);

        return "Session Ended";
    }

}