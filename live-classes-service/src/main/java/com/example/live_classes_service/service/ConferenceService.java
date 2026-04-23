package com.example.live_classes_service.service;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;

public interface ConferenceService {

    ConferenceResponseDTO createConference(CreateConferenceRequest request, String token);

    ConferenceResponseDTO startConference(String conferenceId, String token);

    ConferenceJoinResponseDTO joinConference(String conferenceId, String token);

    String startRecording(String conferenceId, String token);

    String stopRecording(String conferenceId, String token);

    String endConference (String conferenceId, String token);


}