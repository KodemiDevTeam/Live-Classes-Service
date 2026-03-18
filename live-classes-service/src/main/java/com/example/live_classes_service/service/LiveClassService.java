package com.example.live_classes_service.service;

import com.example.live_classes_service.dto.request.CreateLiveClassRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.LiveClassResponseDTO;

import java.util.List;

public interface LiveClassService {

    LiveClassResponseDTO createLiveClass(CreateLiveClassRequest request, String token);

    LiveClassResponseDTO startLiveClass(String sessionId, String token);

    LiveClassJoinResponseDTO joinLiveClass(String sessionId, String token);

    List<LiveClassResponseDTO> getLiveClassesByCourse(String courseId);

    String startRecording(String sessionId, String token);

    String stopRecording(String sessionId, String token);

    String endLiveClass(String sessionId, String token);
}