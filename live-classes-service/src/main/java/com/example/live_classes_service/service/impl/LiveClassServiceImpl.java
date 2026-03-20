package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;
import com.example.live_classes_service.dto.response.EnrollmentStatusResponse;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.service.LiveClassService;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveClassServiceImpl implements LiveClassService {

    private final LiveClassRepository repository;
    private final EnrollmentClient enrollmentClient;
    private final JwtUtil jwtUtil;
    private final VideoSDKService videoSDKService;

    private static final String ROLE_TRAINER = "TRAINER";
    private static final String ROLE_LEARNER = "LEARNER";

    private static final String STATUS_STARTED = "LIVE_STARTED";
    private static final String STATUS_ENDED = "ENDED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";

    private static final String ACTION_CREATED = "CREATED";
    private static final String ACTION_STARTED = "STARTED";


    @Override
    public LiveClassResponseDTO createLiveClass(CreateLiveClassRequest request, String token) {

        String role = jwtUtil.extractRole(token);
        if (!ROLE_TRAINER.equals(role)) {
            throw new UnauthorizedException("Only trainer can create live class");
        }

        String trainerId = jwtUtil.extractUserId(token);
        String trainerName = jwtUtil.extractName(token);

        String roomId = retry(() -> videoSDKService.createRoom());

        LiveClassEntity entity = LiveClassEntity.builder()
                .liveClassId(UUID.randomUUID().toString())
                .courseId(request.getCourseId())
                .trainerId(trainerId)
                .trainerName(trainerName)
                .roomId(roomId)
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .actionType(ACTION_CREATED)
                .status(STATUS_SCHEDULED)
                .sessionType("LIVE_CLASS")
                .maxParticipants(request.getMaxParticipants())
                .isRecording(false)
                .createdAt(Instant.now().toString())
                .build();

        repository.save(entity);
        log.info("Live class created: {}", entity.getLiveClassId());

        return mapToResponse(entity);
    }

    @Override
    public LiveClassResponseDTO startLiveClass(String sessionId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(sessionId);

        String trainerId = jwtUtil.extractUserId(token);
        String trainerName = jwtUtil.extractName(token);

        validateTrainer(entity, trainerId);

        if (STATUS_ENDED.equals(entity.getStatus())) {
            throw new BadRequestException("Live class already ended");
        }

        String startedAt = Instant.now().toString();

        boolean updated = repository.updateStatusIfNotStarted(sessionId, startedAt);

        if (!updated) {
            throw new BadRequestException("Live class already started");
        }

        entity.setStatus(STATUS_STARTED);
        entity.setStartedAt(startedAt);
        entity.setActionType(ACTION_STARTED);

        log.info("Live class started: {}", sessionId);

        return mapToResponse(entity, trainerId, trainerName);
    }


    @Override
    public LiveClassJoinResponseDTO joinLiveClass(String sessionId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(sessionId);

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        validateJoinAccess(entity, userId, role, token);

        return LiveClassJoinResponseDTO.builder()
                .liveClassId(entity.getLiveClassId())
                .roomId(entity.getRoomId())
                .token(videoSDKService.generateToken())
                .role(role)
                .build();
    }

    @Override
    public List<LiveClassResponseDTO> getLiveClassesByCourse(String courseId) {

        return repository.findByCourseId(courseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Override
    public String startRecording(String liveClassId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(liveClassId);

        String userId = jwtUtil.extractUserId(token);
        validateTrainer(entity, userId);

        String recordingId = retry(() -> videoSDKService.startRecording(entity.getRoomId()));

        entity.setIsRecording(true);
        entity.setRecordingUrl(recordingId);

        repository.save(entity);
        log.info("Recording started: {}", liveClassId);

        return "Recording Started";
    }

    @Override
    public String stopRecording(String sessionId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(sessionId);

        String userId = jwtUtil.extractUserId(token);
        validateTrainer(entity, userId);

        videoSDKService.stopRecording(entity.getRoomId());

        entity.setIsRecording(false);

        repository.save(entity);
        log.info("Recording stopped: {}", sessionId);

        return "Recording Stopped";
    }
    @Override
    public String endLiveClass(String sessionId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(sessionId);

        String userId = jwtUtil.extractUserId(token);
        validateTrainer(entity, userId);

        videoSDKService.endRoom(entity.getRoomId());

        entity.setStatus(STATUS_ENDED);
        entity.setEndedAt(Instant.now().toString());

        repository.save(entity);
        log.info("Live class ended: {}", sessionId);

        return "Live Class Ended";
    }

    private LiveClassEntity getLiveClassOrThrow(String id) {
        LiveClassEntity entity = repository.findById(id);
        if (entity == null) {
            throw new BadRequestException("Live class not found");
        }
        return entity;
    }

    private void validateTrainer(LiveClassEntity entity, String userId) {
        if (!entity.getTrainerId().equals(userId)) {
            throw new UnauthorizedException("Only trainer allowed");
        }
    }

    private void validateJoinAccess(LiveClassEntity entity, String userId, String role, String token) {

        if (ROLE_TRAINER.equals(role)) {
            if (!entity.getTrainerId().equals(userId)) {
                throw new UnauthorizedException("Not trainer of this class");
            }
            return;
        }

        if (ROLE_LEARNER.equals(role)) {
            try {
                EnrollmentStatusResponse status =
                        enrollmentClient.getEnrollmentStatus(entity.getCourseId(), token);

                if (status == null || !status.isEnrolled()) {
                    throw new BadRequestException("User not enrolled");
                }

            } catch (Exception e) {
                log.error("Enrollment service failed", e);
                throw new BadRequestException("Enrollment validation failed");
            }
            return;
        }

        throw new UnauthorizedException("Invalid role");
    }

    private LiveClassResponseDTO mapToResponse(LiveClassEntity entity) {
        return mapToResponse(entity, entity.getTrainerId(), entity.getTrainerName());
    }

    private LiveClassResponseDTO mapToResponse(LiveClassEntity entity, String trainerId, String trainerName) {
        return LiveClassResponseDTO.builder()
                .liveClassId(entity.getLiveClassId())
                .courseId(entity.getCourseId())
                .trainerId(trainerId)
                .trainerName(trainerName)
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