package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.SessionJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.SessionEntity;
import com.example.live_classes_service.repository.SessionRepository;
import com.example.live_classes_service.service.SessionService;
import com.example.live_classes_service.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final SessionRepository repository;
    private final JwtUtil jwtUtil;
    private final VideoSDKService videoSDKService;
    private final EnrollmentClient enrollmentClient;
    private static final String ROLE_TRAINER = "TRAINER";
    private static final String ROLE_LEARNER = "LEARNER";

    private static final String STATUS_STARTED = "SESSION_STARTED";
    private static final String STATUS_ENDED = "ENDED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";

    private static final String ACTION_CREATED = "CREATED";
    private static final String ACTION_STARTED = "STARTED";

    @Override
    public SessionResponseDTO createSession(CreateSessionRequest request, String token) {

        String role = jwtUtil.extractRole(token);
        if (!ROLE_TRAINER.equals(role)) {
            throw new UnauthorizedException("Only trainer can create session");
        }

        String organizerId = jwtUtil.extractUserId(token);
        String organizerName = jwtUtil.extractName(token);

        String roomId = retry(new Supplier<String>() {
            @Override
            public String get() {
                return videoSDKService.createRoom();
            }
        });

        SessionEntity entity = SessionEntity.builder()
                .sessionId(UUID.randomUUID().toString())
                .organizerId(organizerId)
                .organizerName(organizerName)
                .roomId(roomId)
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .actionType(ACTION_CREATED)
                .status(STATUS_SCHEDULED)
                .sessionType("SESSION")
                .maxParticipants(request.getMaxParticipants())
                .isRecording(false)
                .createdAt(Instant.now().toString())
                .build();

        repository.save(entity);
        log.info("Session created: {}", entity.getSessionId());

        return mapToResponse(entity);
    }

    @Override
    public SessionResponseDTO startSession(String sessionId, String token) {

        SessionEntity entity = getSessionOrThrow(sessionId);

        String organizerId = jwtUtil.extractUserId(token);
        String organizerName = jwtUtil.extractName(token);

        validateOrganizer(entity, organizerId);

        if (STATUS_ENDED.equals(entity.getStatus())) {
            throw new BadRequestException("Session already ended");
        }

        String startedAt = Instant.now().toString();

        boolean updated = repository.updateStatusAtomically(
                sessionId,
                STATUS_SCHEDULED,
                STATUS_STARTED,
                "startedAt",
                startedAt
        );

        if (!updated) {
            if (STATUS_STARTED.equals(entity.getStatus())) {
                return mapToResponse(entity, organizerId, organizerName);
            }
            throw new BadRequestException("Session already started or invalid state");
        }

        entity.setStatus(STATUS_STARTED);
        entity.setStartedAt(startedAt);
        entity.setActionType(ACTION_STARTED);

        log.info("Session started: {}", sessionId);
        return mapToResponse(entity, organizerId, organizerName);
    }

    @Override
    public SessionJoinResponseDTO joinSession(String sessionId, String token) {

        SessionEntity entity = getSessionOrThrow(sessionId);
        if (!STATUS_STARTED.equals(entity.getStatus())) {
            throw new BadRequestException("session is not started yet");
        }

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        validateJoinAccess(entity, userId, role, token);

        return SessionJoinResponseDTO.builder()
                .sessionId(entity.getSessionId())
                .roomId(entity.getRoomId())
                .token(videoSDKService.generateToken())
                .role(role)
                .build();
    }

    @Override
    public String startRecording(String sessionId, String token) {

        SessionEntity entity = getSessionOrThrow(sessionId);

        String userId = jwtUtil.extractUserId(token);
        validateOrganizer(entity, userId);

        final SessionEntity finalEntity = entity;
        String recordingId = retry(new Supplier<String>() {
            @Override
            public String get() {
                return videoSDKService.startRecording(finalEntity.getRoomId());
            }
        });

        entity.setIsRecording(true);
        entity.setRecordingUrl(recordingId);

        repository.save(entity);
        log.info("Recording started: {}", sessionId);

        return "Recording Started";
    }

    @Override
    public String stopRecording(String sessionId, String token) {

        SessionEntity entity = getSessionOrThrow(sessionId);

        String userId = jwtUtil.extractUserId(token);
        validateOrganizer(entity, userId);

        videoSDKService.stopRecording(entity.getRoomId());

        entity.setIsRecording(false);

        repository.save(entity);
        log.info("Recording stopped: {}", sessionId);

        return "Recording Stopped";
    }

    @Override
    public String endSession(String sessionId, String token) {

        SessionEntity entity = getSessionOrThrow(sessionId);

        String userId = jwtUtil.extractUserId(token);
        validateOrganizer(entity, userId);

        videoSDKService.endRoom(entity.getRoomId());

        entity.setStatus(STATUS_ENDED);
        entity.setEndedAt(Instant.now().toString());

        repository.save(entity);
        log.info("Session ended: {}", sessionId);

        return "Session Ended";
    }

    private SessionEntity getSessionOrThrow(String sessionId) {
        SessionEntity entity = repository.findById(sessionId);
        if (entity == null) {
            throw new BadRequestException("Session not found");
        }
        return entity;
    }

    private void validateOrganizer(SessionEntity entity, String userId) {
        if (!entity.getOrganizerId().equals(userId)) {
            throw new UnauthorizedException("Only organizer allowed");
        }
    }

    private void validateJoinAccess(SessionEntity entity, String userId, String role, String token) {

        if (ROLE_TRAINER.equals(role)) {
            if (!entity.getOrganizerId().equals(userId)) {
                throw new UnauthorizedException("Not organizer");
            }
            return;
        }

        if (ROLE_LEARNER.equals(role)) {
            try {
                SessionStatusResponse status =
                        enrollmentClient.getSessionEnrollmentStatus(entity.getSessionId(), token);

                if (status == null || !status.isEnrolled()) {
                    throw new BadRequestException("User not enrolled");
                }

            } catch (BadRequestException | UnauthorizedException e) {
                throw e;
            } catch (feign.FeignException e) {
                log.error("Enrollment check failed for sessionId={}", entity.getSessionId(), e);
                throw new BadRequestException("Enrollment validation failed");
            }
            return;
        }

        throw new UnauthorizedException("Invalid role");
    }

    private SessionResponseDTO mapToResponse(SessionEntity entity) {
        return mapToResponse(entity, entity.getOrganizerId(), entity.getOrganizerName());
    }

    private SessionResponseDTO mapToResponse(SessionEntity entity, String organizerId, String organizerName) {
        return SessionResponseDTO.builder()
                .sessionId(entity.getSessionId())
                .organizerId(organizerId)
                .organizerName(organizerName)
                .roomId(entity.getRoomId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .actionType(entity.getActionType())
                .sessionType(entity.getSessionType())
                .status(entity.getStatus())
                .scheduledAt(entity.getScheduledAt())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .maxParticipants(entity.getMaxParticipants())
                .isRecording(entity.getIsRecording())
                .build();
    }

    private <T> T retry(Supplier<T> action) {
        int attempts = 3;

        for (int i = 0; i < attempts; i++) {
            try {
                return action.get();
            } catch (Exception e) {
                log.warn("Retry attempt {} failed", i + 1, e);

                if (i == attempts - 1) {
                    throw new BadRequestException("External service failed");
                }
            }
        }
        throw new BadRequestException("Retry failed");
    }
}