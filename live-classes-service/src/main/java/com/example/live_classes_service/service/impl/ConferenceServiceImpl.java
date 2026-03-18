package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.feign.EnrollmentClient;
import com.example.live_classes_service.model.ConferenceEntity;
import com.example.live_classes_service.repository.ConferenceRepository;
import com.example.live_classes_service.service.ConferenceService;
import com.example.live_classes_service.util.JwtUtil;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConferenceServiceImpl implements ConferenceService {

    private final ConferenceRepository repository;
    private final JwtUtil jwtUtil;
    private final VideoSDKService videoSDKService;
    private final EnrollmentClient enrollmentClient;

    public ConferenceServiceImpl(
            ConferenceRepository repository,
            JwtUtil jwtUtil,
            VideoSDKService videoSDKService, EnrollmentClient enrollmentClient
    ) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.videoSDKService = videoSDKService;
        this.enrollmentClient = enrollmentClient;
    }

    @Override
    public ConferenceResponseDTO createConference(CreateConferenceRequest request, String token) {

        String organizerId = jwtUtil.extractUserId(token);

        String organizerName = jwtUtil.extractName(token);

        String conferenceId = UUID.randomUUID().toString();

        String roomId = videoSDKService.createRoom();

        ConferenceEntity entity = ConferenceEntity.builder()
                .conferenceId(conferenceId)
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

        return ConferenceResponseDTO.builder()
                .conferenceId(conferenceId)
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
    public ConferenceResponseDTO startConference(String conferenceId, String token) {

        ConferenceEntity entity = repository.findById(conferenceId);
        if (entity == null) {
            throw new RuntimeException("Conference not found: " + conferenceId);
        }


        String organizerId = jwtUtil.extractUserId(token);
        String organizerName = jwtUtil.extractName(token);

        if (!entity.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("Only organizer can start conference");
        }

        if ("CONFERENCE_STARTED".equals(entity.getStatus())) {
            throw new RuntimeException("Conference is already started");
        }

        if ("ENDED".equals(entity.getStatus())) {
            throw new RuntimeException("Conference has already ended");
        }

        entity.setStatus("CONFERENCE_STARTED");
        entity.setStartedAt(Instant.now().toString());

        repository.save(entity);

        return ConferenceResponseDTO.builder()
                .conferenceId(entity.getConferenceId())
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
                .isRecording(false)
                .build();
    }

    @Override
    public ConferenceJoinResponseDTO joinConference(String conferenceId, String token) {

        ConferenceEntity entity = repository.findById(conferenceId);
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        if (role.equals("TRAINER")) {
            if (!entity.getOrganizerId().equals(userId)) {
                throw new RuntimeException("You are not the Organizer of this Conference");
            }
        }
        else if (role.equals("LEARNER")) {
            try {
                SessionStatusResponse status =
                        enrollmentClient.getConferenceEnrollmentStatus(entity.getConferenceId(), token);
                if (!status.isEnrolled()) {
                    throw new RuntimeException("User not enrolled in this Conference");
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("User not enrolled in this Conference or feign Client Issue ");
            }
        }
        else {
            throw new RuntimeException("Unauthorized role: " + role);
        }


        String videoToken = videoSDKService.generateToken();

        return ConferenceJoinResponseDTO.builder()
                .roomId(entity.getRoomId())
                .conferenceId(entity.getConferenceId())
                .token(videoToken)
                .role(role)
                .build();
    }

    @Override
    public String startRecording(String conferenceId, String token) {

        ConferenceEntity entity = repository.findById(conferenceId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Only organizer can start recording");
        }

        String recordingId = videoSDKService.startRecording(entity.getRoomId());

        entity.setIsRecording(true);
        entity.setRecordingUrl(recordingId);

        repository.save(entity);

        return "Recording Started";
    }

    @Override
    public String stopRecording(String conferenceId, String token) {

        ConferenceEntity entity = repository.findById(conferenceId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Only organizer can stop recording");
        }

        videoSDKService.stopRecording(entity.getRoomId());

        entity.setIsRecording(false);

        repository.save(entity);

        return "Recording Stopped";
    }

    public String endConference(String conferenceId, String token) {

        ConferenceEntity entity = repository.findById(conferenceId);

        String userId = jwtUtil.extractUserId(token);

        if (!entity.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Only organizer can end conference");
        }

        videoSDKService.endRoom(entity.getRoomId());

        entity.setStatus("ENDED");
        entity.setEndedAt(Instant.now().toString());

        repository.save(entity);

        return "Conference Ended";
    }
}