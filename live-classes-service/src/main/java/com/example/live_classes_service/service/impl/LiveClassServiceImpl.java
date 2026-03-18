package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;
import com.example.live_classes_service.dto.response.EnrollmentStatusResponse;
import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.service.LiveClassService;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.util.JwtUtil;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LiveClassServiceImpl implements LiveClassService {

    private final LiveClassRepository repository;
    private final EnrollmentClient enrollmentClient;
    private final JwtUtil jwtUtil;
    private final VideoSDKService videoSDKService;

    public LiveClassServiceImpl(
            LiveClassRepository repository,
            EnrollmentClient enrollmentClient,
            JwtUtil jwtUtil,
            VideoSDKService videoSDKService
    ) {
        this.repository = repository;
        this.enrollmentClient = enrollmentClient;
        this.jwtUtil = jwtUtil;
        this.videoSDKService = videoSDKService;
    }

    @Override
    public LiveClassResponseDTO createLiveClass(CreateLiveClassRequest request, String token) {

        String Role = jwtUtil.extractRole(token);

        if (!"TRAINER".equals(Role)){
            throw new RuntimeException("Only trainer can create live class");
        }


        String trainerId = jwtUtil.extractUserId(token);
        String trainerName = jwtUtil.extractName(token);
        String liveClassId = UUID.randomUUID().toString();

        String roomId = videoSDKService.createRoom();

        LiveClassEntity entity = LiveClassEntity.builder()
                .liveClassId(liveClassId)
                .courseId(request.getCourseId())
                .trainerId(trainerId)
                .trainerName(trainerName)
                .roomId(roomId)
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .actionType("CREATED")
                .status("SCHEDULED")
                .sessionType("LIVE_CLASS")
                .maxParticipants(request.getMaxParticipants())
                .isRecording(false)
                .createdAt(Instant.now().toString())
                .build();

        repository.save(entity);

        return LiveClassResponseDTO.builder()
                .liveClassId(liveClassId)
                .courseId(entity.getCourseId())
                .trainerId(entity.getTrainerId())
                .trainerName(entity.getTrainerName())
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
    public LiveClassResponseDTO startLiveClass(String sessionId, String token) {

        LiveClassEntity entity = repository.findById(sessionId);
        String trainerId = jwtUtil.extractUserId(token);
        String trainerName = jwtUtil.extractName(token);

        if (!entity.getTrainerId().equals(trainerId)) {
            throw new RuntimeException("Only creator can start class");
        }
        if ("LIVE_STARTED".equals(entity.getStatus())) {
            throw new RuntimeException("Conference is already started");
        }

        if ("ENDED".equals(entity.getStatus())) {
            throw new RuntimeException("Conference has already ended");
        }
        entity.setActionType("STARTED");
        entity.setStatus("LIVE_STARTED");
        entity.setStartedAt(Instant.now().toString());

        repository.save(entity);

        return LiveClassResponseDTO.builder()
                .liveClassId(entity.getLiveClassId())
                .courseId(entity.getCourseId())
                .sessionType(entity.getSessionType())
                .description(entity.getDescription())
                .startedAt(entity.getStartedAt())
                .maxParticipants(entity.getMaxParticipants())
                .roomId(entity.getRoomId())
                .trainerId(trainerId)
                .trainerName(trainerName)
                .title(entity.getTitle())
                .status(entity.getStatus())
                .actionType(entity.getActionType())
                .isRecording(entity.getIsRecording())
                .build();
    }

    @Override
    public LiveClassJoinResponseDTO joinLiveClass(String sessionId, String token) {

        LiveClassEntity entity = repository.findById(sessionId);
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        if (role.equals("TRAINER")) {
            if (!entity.getTrainerId().equals(userId)) {
                throw new RuntimeException("You are not the trainer of this class");
            }
        }
        else if (role.equals("LEARNER")) {
            try {
                EnrollmentStatusResponse status =
                        enrollmentClient.getEnrollmentStatus(entity.getCourseId(), token);
                if (!status.isEnrolled()) {
                    throw new RuntimeException("User not enrolled in this course");
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("User not enrolled in this course or feign Client Issue ");
            }
        }
        else {
            throw new RuntimeException("Unauthorized role: " + role);
        }

        String videoToken = videoSDKService.generateToken();

        return LiveClassJoinResponseDTO.builder()
                .liveClassId(entity.getLiveClassId())
                .roomId(entity.getRoomId())
                .token(videoToken)
                .role(role)
                .build();
    }

    @Override
    public List<LiveClassResponseDTO> getLiveClassesByCourse(String courseId) {

        return repository.findByCourseId(courseId)
                .stream()
                .map(e -> LiveClassResponseDTO.builder()
                        .liveClassId(e.getLiveClassId())
                        .courseId(e.getCourseId())
                        .trainerId(e.getTrainerId())
                        .trainerName(e.getTrainerName())
                        .description(e.getDescription())
                        .sessionType(e.getSessionType())
                        .maxParticipants(e.getMaxParticipants())
                        .createdAt(e.getCreatedAt())
                        .roomId(e.getRoomId())
                        .title(e.getTitle())
                        .status(e.getStatus())
                        .scheduledAt(e.getScheduledAt())
                        .isRecording(e.getIsRecording())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public String startRecording(String liveClassId, String token) {

        LiveClassEntity entity = repository.findById(liveClassId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getTrainerId().equals(userId)) {
            throw new RuntimeException("Only creator can record");
        }

        String recordingId = videoSDKService.startRecording(entity.getRoomId());

        entity.setIsRecording(true);
        entity.setRecordingUrl(recordingId);

        repository.save(entity);

        return "Recording Started";
    }

    @Override
    public String stopRecording(String sessionId, String token) {

        LiveClassEntity entity = repository.findById(sessionId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getTrainerId().equals(userId)) {
            throw new RuntimeException("Only creator can stop recording");
        }

        videoSDKService.stopRecording(entity.getRoomId());

        entity.setIsRecording(false);

        repository.save(entity);

        return "Recording Stopped";
    }

    public String endLiveClass(String sessionId, String token) {

        LiveClassEntity entity = repository.findById(sessionId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getTrainerId().equals(userId)) {
            throw new RuntimeException("Only creator can end class");
        }

        videoSDKService.endRoom(entity.getRoomId());

        entity.setStatus("ENDED");
        entity.setEndedAt(Instant.now().toString());

        repository.save(entity);

        return "Live Class Ended";
    }
}