package com.example.live_classes_service.service;

import com.example.live_classes_service.dto.request.CreateSessionRequest;
import com.example.live_classes_service.dto.response.LiveClassJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionJoinResponseDTO;
import com.example.live_classes_service.dto.response.SessionResponseDTO;

public interface SessionService {

    SessionResponseDTO createSession(CreateSessionRequest request, String token);

    SessionResponseDTO startSession(String sessionId, String token);

    SessionJoinResponseDTO joinSession(String sessionId, String token);

    String startRecording(String sessionId, String token);

    String stopRecording(String sessionId, String token);

    String endSession(String sessionId, String token);
}