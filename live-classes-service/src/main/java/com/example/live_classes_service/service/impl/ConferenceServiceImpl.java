package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.ConferenceEntity;
import com.example.live_classes_service.repository.ConferenceRepository;
import com.example.live_classes_service.service.ConferenceService;
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
public class ConferenceServiceImpl implements ConferenceService {

    private final ConferenceRepository repository;
    private final JwtUtil jwtUtil;
    private final VideoSDKService videoSDKService;
    private final EnrollmentClient enrollmentClient;

    private static final String STATUS_STARTED = "CONFERENCE_STARTED";
    private static final String STATUS_ENDED = "ENDED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";

    private static final String ROLE_TRAINER = "TRAINER";
    private static final String ROLE_LEARNER = "LEARNER";

    private static final String ACTION_CREATED = "CREATED";


    @Override
    public ConferenceResponseDTO createConference(CreateConferenceRequest request, String token) {

        String organizerId = jwtUtil.extractUserId(token);
        String organizerName = jwtUtil.extractName(token);

        String roomId = retry(() -> videoSDKService.createRoom());

        ConferenceEntity entity = ConferenceEntity.builder()
                .conferenceId(UUID.randomUUID().toString())
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
        log.info("Conference created: {}", entity.getConferenceId());

        return mapToResponse(entity);
    }


    @Override
    public ConferenceResponseDTO startConference(String conferenceId, String token) {

        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String organizerId = jwtUtil.extractUserId(token);
        String organizerName = jwtUtil.extractName(token);

        validateOrganizer(entity, organizerId);

        if (STATUS_ENDED.equals(entity.getStatus())) {
            throw new BadRequestException("Conference already ended");
        }

        String startedAt = Instant.now().toString();

        boolean updated = repository.updateStatusIfNotStarted(conferenceId, startedAt);

        if (!updated) {
            throw new BadRequestException("Conference already started");
        }

        entity.setStatus(STATUS_STARTED);
        entity.setStartedAt(startedAt);

        log.info("Conference started: {}", conferenceId);

        return mapToResponse(entity, organizerId, organizerName);
    }

    @Override
    public ConferenceJoinResponseDTO joinConference(String conferenceId, String token) {

        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        validateJoinAccess(entity, userId, role, token);

        return ConferenceJoinResponseDTO.builder()
                .roomId(entity.getRoomId())
                .conferenceId(entity.getConferenceId())
                .token(videoSDKService.generateToken())
                .role(role)
                .build();
    }


    @Override
    public String startRecording(String conferenceId, String token) {

        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String userId = jwtUtil.extractUserId(token);
        validateOrganizer(entity, userId);

        String recordingId = retry(() -> videoSDKService.startRecording(entity.getRoomId()));

        entity.setIsRecording(true);
        entity.setRecordingUrl(recordingId);

        repository.save(entity);
        log.info("Recording started for conference: {}", conferenceId);

        return "Recording Started";
    }

    @Override
    public String stopRecording(String conferenceId, String token) {

        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String userId = jwtUtil.extractUserId(token);
        validateOrganizer(entity, userId);

        videoSDKService.stopRecording(entity.getRoomId());

        entity.setIsRecording(false);

        repository.save(entity);
        log.info("Recording stopped for conference: {}", conferenceId);

        return "Recording Stopped";
    }


    @Override
    public String endConference(String conferenceId, String token) {

        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String userId = jwtUtil.extractUserId(token);
        validateOrganizer(entity, userId);

        videoSDKService.endRoom(entity.getRoomId());

        entity.setStatus(STATUS_ENDED);
        entity.setEndedAt(Instant.now().toString());

        repository.save(entity);
        log.info("Conference ended: {}", conferenceId);

        return "Conference Ended";
    }

    private ConferenceEntity getConferenceOrThrow(String conferenceId) {
        ConferenceEntity entity = repository.findById(conferenceId);
        if (entity == null) {
            throw new BadRequestException("Conference not found");
        }
        return entity;
    }

    private void validateOrganizer(ConferenceEntity entity, String userId) {
        if (!entity.getOrganizerId().equals(userId)) {
            throw new UnauthorizedException("Only organizer allowed");
        }
    }

    private void validateJoinAccess(ConferenceEntity entity, String userId, String role, String token) {

        if (ROLE_TRAINER.equals(role)) {
            if (!entity.getOrganizerId().equals(userId)) {
                throw new UnauthorizedException("Not organizer");
            }
            return;
        }

        if (ROLE_LEARNER.equals(role)) {
            try {
                SessionStatusResponse status =
                        enrollmentClient.getConferenceEnrollmentStatus(entity.getConferenceId(), token);

                if (status == null || !status.isEnrolled()) {
                    throw new BadRequestException("User not enrolled");
                }

            } catch (Exception e) {
                log.error("Enrollment check failed", e);
                throw new BadRequestException("Enrollment validation failed");
            }
            return;
        }

        throw new UnauthorizedException("Invalid role");
    }

    private ConferenceResponseDTO mapToResponse(ConferenceEntity entity) {
        return mapToResponse(entity, entity.getOrganizerId(), entity.getOrganizerName());
    }

    private ConferenceResponseDTO mapToResponse(ConferenceEntity entity, String organizerId, String organizerName) {
        return ConferenceResponseDTO.builder()
                .conferenceId(entity.getConferenceId())
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