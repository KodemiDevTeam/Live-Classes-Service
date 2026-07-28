package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.exception.ConflictException;
import com.example.live_classes_service.exception.ForbiddenException;
import com.example.live_classes_service.exception.ResourceNotFoundException;
import com.example.live_classes_service.exception.UnauthorizedException;
import com.example.live_classes_service.exception.VideoSDKException;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.service.LiveClassService;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.service.notification.NotificationPublisher;
import com.example.live_classes_service.dto.notification.NotificationRequest;
import com.example.live_classes_service.dto.notification.NotificationType;
import com.example.live_classes_service.dto.notification.NotificationChannel;
import com.example.live_classes_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveClassServiceImpl implements LiveClassService {

    private final LiveClassRepository repository;
    private final EnrollmentClient enrollmentClient;
    private final JwtUtil jwtUtil;
    private final VideoSDKService videoSDKService;
    private final NotificationPublisher notificationPublisher;

    private static final String ROLE_TRAINER = "TRAINER";
    private static final String ROLE_LEARNER = "LEARNER";
    private static final String STATUS_STARTED = "LIVE_STARTED";
    private static final String STATUS_ENDED = "ENDED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String ACTION_CREATED = "CREATED";
    private static final String ACTION_STARTED = "STARTED";
    private static final String ACTION_ENDED = "ENDED";


    @Override
    public LiveClassResponseDTO createLiveClass(CreateLiveClassRequest request, String token) {

        String role = jwtUtil.extractRole(token);
        if (!ROLE_TRAINER.equals(role)) {
            throw new ForbiddenException("Only trainers are allowed to create live classes");
        }

        String trainerId = jwtUtil.extractUserId(token);
        String trainerName = jwtUtil.extractName(token);

        String roomId = retry(new Supplier<String>() {
            @Override
            public String get() {
                return videoSDKService.createRoom();
            }
        });

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

        try {
            List<String> learners = enrollmentClient.getEnrolledLearners(request.getCourseId());
            if (learners != null && !learners.isEmpty()) {
                NotificationRequest notif = NotificationRequest.builder()
                        .title("Live Class Scheduled")
                        .message("A new live class '" + request.getTitle() + "' has been scheduled for your course by " + trainerName)
                        .type(NotificationType.LIVE_CLASS_SCHEDULED)
                        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                        .referenceId(entity.getLiveClassId())
                        .referenceType("LIVE_CLASS")
                        .build();
                notificationPublisher.publishToUsers(learners, notif);
            }
        } catch (Exception ex) {
            log.error("Failed to send LIVE_CLASS_SCHEDULED notification", ex);
        }

        return mapToResponse(entity);
    }

    @Override
    public LiveClassResponseDTO startLiveClass(String sessionId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(sessionId);

        String trainerId = jwtUtil.extractUserId(token);
        String trainerName = jwtUtil.extractName(token);

        validateTrainer(entity, trainerId);

        if (STATUS_ENDED.equals(entity.getStatus())) {
            throw new ConflictException("Live class has already ended and cannot be started again");
        }

        String startedAt = Instant.now().toString();

        boolean updated = repository.updateStatusIfNotStarted(sessionId, startedAt, ACTION_STARTED);

        if (!updated) {
            throw new ConflictException("Live class has already been started");
        }

        entity.setStatus(STATUS_STARTED);
        entity.setStartedAt(startedAt);
        entity.setActionType(ACTION_STARTED);

        log.info("Live class started: {}", sessionId);

        try {
            List<String> learners = enrollmentClient.getEnrolledLearners(entity.getCourseId());
            if (learners != null && !learners.isEmpty()) {
                NotificationRequest notif = NotificationRequest.builder()
                        .title("Live Class Started")
                        .message("The live class '" + entity.getTitle() + "' has just started! Join now.")
                        .type(NotificationType.LIVE_CLASS_STARTED)
                        .channels(List.of(NotificationChannel.IN_APP))
                        .referenceId(entity.getLiveClassId())
                        .referenceType("LIVE_CLASS")
                        .build();
                notificationPublisher.publishToUsers(learners, notif);
            }
        } catch (Exception ex) {
            log.error("Failed to send LIVE_CLASS_STARTED notification", ex);
        }

        return mapToResponse(entity, trainerId, trainerName);
    }


    @Override
    public LiveClassJoinResponseDTO joinLiveClass(String sessionId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(sessionId);

        if (!STATUS_STARTED.equals(entity.getStatus())) {
            throw new ConflictException("Live class is not started yet. Current status: " + entity.getStatus());
        }

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

        List<LiveClassEntity> entities = repository.findByCourseId(courseId);
        List<LiveClassResponseDTO> dtos = new java.util.ArrayList<LiveClassResponseDTO>();
        for (LiveClassEntity entity : entities) {
            dtos.add(mapToResponse(entity));
        }
        return dtos;
    }


    @Override
    public String startRecording(String liveClassId, String token) {

        LiveClassEntity entity = getLiveClassOrThrow(liveClassId);

        String userId = jwtUtil.extractUserId(token);
        validateTrainer(entity, userId);

        final LiveClassEntity finalEntity = entity;
        String recordingId = retry(new Supplier<String>() {
            @Override
            public String get() {
                return videoSDKService.startRecording(finalEntity.getRoomId());
            }
        });

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

        // Notify enrolled learners that recording is now available
        try {
            List<String> learners = enrollmentClient.getEnrolledLearners(entity.getCourseId());
            if (learners != null && !learners.isEmpty()) {
                NotificationRequest notif = NotificationRequest.builder()
                        .title("Recording Available")
                        .message("The recording for '" + entity.getTitle() + "' is now available. You can watch it anytime.")
                        .type(NotificationType.RECORDING_AVAILABLE)
                        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                        .referenceId(entity.getLiveClassId())
                        .referenceType("LIVE_CLASS")
                        .build();
                notificationPublisher.publishToUsers(learners, notif);
            }
        } catch (Exception ex) {
            log.error("Failed to send RECORDING_AVAILABLE notification", ex);
        }

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
        entity.setActionType(ACTION_ENDED);

        repository.save(entity);
        log.info("Live class ended: {}", sessionId);

        // Notify enrolled learners that the class has ended
        try {
            List<String> learners = enrollmentClient.getEnrolledLearners(entity.getCourseId());
            if (learners != null && !learners.isEmpty()) {
                NotificationRequest notif = NotificationRequest.builder()
                        .title("Live Class Ended")
                        .message("The live class '" + entity.getTitle() + "' has ended. Recording will be available soon.")
                        .type(NotificationType.LIVE_CLASS_CANCELLED)
                        .channels(List.of(NotificationChannel.IN_APP))
                        .referenceId(entity.getLiveClassId())
                        .referenceType("LIVE_CLASS")
                        .build();
                notificationPublisher.publishToUsers(learners, notif);
            }
        } catch (Exception ex) {
            log.error("Failed to send LIVE_CLASS_ENDED notification", ex);
        }

        return "Live Class Ended";
    }

    private LiveClassEntity getLiveClassOrThrow(String id) {
        LiveClassEntity entity = repository.findById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Live class not found with id: " + id);
        }
        return entity;
    }

    private void validateTrainer(LiveClassEntity entity, String userId) {
        if (!entity.getTrainerId().equals(userId)) {
            throw new ForbiddenException("Only the assigned trainer can perform this action on the live class");
        }
    }

    private void validateJoinAccess(LiveClassEntity entity, String userId, String role, String token) {

        if (ROLE_TRAINER.equals(role)) {
            if (!entity.getTrainerId().equals(userId)) {
                throw new ForbiddenException("You are not the trainer of this live class");
            }
            return;
        }

        if (ROLE_LEARNER.equals(role)) {
            try {
                SessionStatusResponse status =
                        enrollmentClient.getCourseEnrollmentStatus(entity.getCourseId(), token, "LIVE_COURSE");

                if (status == null || !status.isEnrolled()) {
                    throw new BadRequestException("User not enrolled");
                }

            } catch (BadRequestException | UnauthorizedException e) {
                throw e;
            } catch (feign.FeignException e) {
                log.error("Enrollment service failed for courseId={}", entity.getCourseId(), e);
                throw new BadRequestException("Enrollment validation failed");
            }
            return;
        }

        throw new ForbiddenException("Users with role '" + role + "' are not allowed to join live classes");
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
                    throw new VideoSDKException("VideoSDK service operation failed after " + attempts + " attempts", e);
                }
            }
        }
        throw new VideoSDKException("VideoSDK service operation failed after exhausting all retries");
    }
}