package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.ConflictException;
import com.example.live_classes_service.exception.ForbiddenException;
import com.example.live_classes_service.exception.NullBodyException;
import com.example.live_classes_service.exception.ResourceNotFoundException;
import com.example.live_classes_service.exception.TokenNotFoundException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.exception.VideoSDKException;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.ConferenceEntity;
import com.example.live_classes_service.repository.ConferenceRepository;
import com.example.live_classes_service.service.ConferenceService;
import com.example.live_classes_service.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private static final String ACTION_STARTED = "STARTED";
    private static final String ACTION_ENDED = "ENDED";

    private String extractUserIdOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new TokenNotFoundException("JWT token is missing");
        }

        try {
            String userId = jwtUtil.extractUserId(token);
            if (userId == null) {
                throw new UnauthorizedException("Invalid JWT token");
            }
            return userId;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired JWT token");
        }
    }

    private String extractUserName(String token) {
        try {
            return jwtUtil.extractName(token);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String extractRole(String token) {
        try {
            return jwtUtil.extractRole(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid role in token");
        }
    }

    @Override
    public ConferenceResponseDTO createConference(CreateConferenceRequest request, String token) {
        if (request == null) throw new NullBodyException("Request body cannot be null");

        String organizerId = extractUserIdOrThrow(token);
        log.info("Creating conference for organizer: {}", organizerId);
        String organizerName = extractUserName(token);

        String roomId = retry(new Supplier<String>() {
            @Override
            public String get() {
                return videoSDKService.createRoom();
            }
        });

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
                .courseId(request.getCourseId())
                .moduleId(request.getModuleId())
                .lessonId(request.getLessonId())
                .sourceType(request.getSourceType())
                .build();

        repository.save(entity);
        log.info("Conference created: {}", entity.getConferenceId());
        log.info("Conference created sourceType={}, courseId={}, conferenceId={}", request.getSourceType(), request.getCourseId(), entity.getConferenceId());

        return mapToResponse(entity);
    }

    @Override
    public ConferenceResponseDTO startConference(String conferenceId, String token) {
        if (conferenceId == null) throw new NullBodyException("ConferenceId cannot be null");

        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String userId = extractUserIdOrThrow(token);
        String userName = extractUserName(token);

        validateOrganizer(entity, userId);

        if (STATUS_ENDED.equals(entity.getStatus())) {
            throw new ConflictException("Conference has already ended and cannot be started again");
        }

        String startedAt = Instant.now().toString();
        boolean updated = repository.updateStatusIfNotStarted(conferenceId, startedAt, ACTION_STARTED);

        if (!updated) {
            throw new ConflictException("Conference has already been started");
        }

        entity.setStatus(STATUS_STARTED);
        entity.setStartedAt(startedAt);
        entity.setActionType(ACTION_STARTED);

        log.info("Conference started: {}", conferenceId);

        return mapToResponse(entity, userId, userName);
    }

    @Override
    public ConferenceJoinResponseDTO joinConference(String conferenceId, String token) {
        if (conferenceId == null) throw new NullBodyException("ConferenceId cannot be null");

        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        if (!STATUS_STARTED.equals(entity.getStatus())) {
            throw new ConflictException("Conference is not started yet. Current status: " + entity.getStatus());
        }

        String userId = extractUserIdOrThrow(token);
        String role = extractRole(token);

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

        String userId = extractUserIdOrThrow(token);
        validateOrganizer(entity, userId);

        final ConferenceEntity finalEntity = entity;
        String recordingId = retry(new Supplier<String>() {
            @Override
            public String get() {
                return videoSDKService.startRecording(finalEntity.getRoomId());
            }
        });

        entity.setIsRecording(true);
        entity.setRecordingUrl(recordingId);

        repository.save(entity);

        return "Recording Started";
    }

    @Override
    public String stopRecording(String conferenceId, String token) {
        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String userId = extractUserIdOrThrow(token);
        validateOrganizer(entity, userId);

        videoSDKService.stopRecording(entity.getRoomId());

        entity.setIsRecording(false);
        repository.save(entity);

        return "Recording Stopped";
    }

    @Override
    public String endConference(String conferenceId, String token) {
        ConferenceEntity entity = getConferenceOrThrow(conferenceId);

        String userId = extractUserIdOrThrow(token);
        validateOrganizer(entity, userId);

        videoSDKService.endRoom(entity.getRoomId());

        entity.setStatus(STATUS_ENDED);
        entity.setEndedAt(Instant.now().toString());
        entity.setActionType(ACTION_ENDED);

        repository.save(entity);

        return "Conference Ended";
    }

    private ConferenceEntity getConferenceOrThrow(String conferenceId) {
        ConferenceEntity entity = repository.findById(conferenceId);
        if (entity == null) {
            throw new ResourceNotFoundException("Conference not found with id: " + conferenceId);
        }
        return entity;
    }

    private void validateOrganizer(ConferenceEntity entity, String userId) {
        if (entity.getOrganizerId() == null) {
            throw new IllegalStateException("Organizer ID is missing in the database for conference: " + entity.getConferenceId());
        }

        if (!Objects.equals(entity.getOrganizerId(), userId)) {
            throw new ForbiddenException("Only the organizer can perform this action on the conference");
        }
    }

    private void validateJoinAccess(ConferenceEntity entity, String userId, String role, String token) {

        if (ROLE_TRAINER.equals(role)) {
            if (!Objects.equals(entity.getOrganizerId(), userId)) {
                throw new ForbiddenException("You are not the organizer of this conference");
            }
            return;
        }

        if (ROLE_LEARNER.equals(role)) {
            log.info("Join validation conferenceId={}, sourceType={}, courseId={}, role={}", entity.getConferenceId(), entity.getSourceType(), entity.getCourseId(), role);
            try {
                boolean isLiveCourseConference =
                        (entity.getCourseId() != null && !entity.getCourseId().isBlank()) || 
                        "LIVE_COURSE".equals(entity.getSourceType());

                if (isLiveCourseConference) {
                    SessionStatusResponse status =
                            enrollmentClient.getCourseEnrollmentStatus(entity.getCourseId(), token);
            
                    if (status == null || !status.isEnrolled()) {
                        throw new BadRequestException("User not enrolled in this live course");
                    }
                    return;
                }

                SessionStatusResponse status =
                        enrollmentClient.getConferenceEnrollmentStatus(entity.getConferenceId(), token);

                if (status == null || !status.isEnrolled()) {
                    throw new BadRequestException("User not enrolled in this conference");
                }

            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.error("Enrollment check failed", e);
                throw new BadRequestException("Enrollment validation failed");
            }
            return;
        }

        throw new ForbiddenException("Users with role '" + role + "' are not allowed to join conferences");
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
                .courseId(entity.getCourseId())
                .sourceType(entity.getSourceType())
                .build();
    }

    @Override
    public List<ConferenceResponseDTO> getConferencesByOrganizer(String token) {
        String organizerId = extractUserIdOrThrow(token);
        log.info("Fetching conferences for organizer: {}", organizerId);

        List<ConferenceEntity> entities = repository.findByOrganizerId(organizerId);
        log.info("Found {} conferences for organizer: {}", entities.size(), organizerId);

        return entities.stream()
                .filter(e -> !isLiveCourseConference(e))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConferenceResponseDTO> getAllConferences() {
        log.info("Fetching all conferences");

        List<ConferenceEntity> entities = repository.findAll();
        log.info("Found {} total conferences", entities.size());

        return entities.stream()
                .filter(e -> !isLiveCourseConference(e))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ConferenceResponseDTO getConference(String conferenceId) {
        log.info("Fetching conference: {}", conferenceId);
        ConferenceEntity entity = getConferenceOrThrow(conferenceId);
        return mapToResponse(entity);
    }

    /**
     * Checks whether a conference belongs to a live course.
     * Handles both new conferences (sourceType = "LIVE_COURSE") and
     * older conferences (sourceType is null but courseId is set).
     */
    private boolean isLiveCourseConference(ConferenceEntity e) {
        if ("LIVE_COURSE".equals(e.getSourceType())) {
            return true;
        }
        return e.getCourseId() != null && !e.getCourseId().isBlank();
    }

    private <T> T retry(Supplier<T> action) {
        int attempts = 3;

        for (int i = 0; i < attempts; i++) {
            try {
                return action.get();
            } catch (Exception e) {
                log.warn("Retry attempt {} failed", i + 1, e);

                if (i == attempts - 1) {
                    throw new VideoSDKException("VideoSDK service operation failed after " + attempts + " attempts", e);
                }
            }
        }
        throw new VideoSDKException("VideoSDK service operation failed after exhausting all retries");
    }
}