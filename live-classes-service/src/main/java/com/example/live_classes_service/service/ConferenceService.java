package com.example.live_classes_service.service;

import com.example.live_classes_service.dto.request.CreateConferenceRequest;
import com.example.live_classes_service.dto.response.ConferenceJoinResponseDTO;
import com.example.live_classes_service.dto.response.ConferenceResponseDTO;

import java.util.List;

public interface ConferenceService {

    ConferenceResponseDTO createConference(CreateConferenceRequest request, String token);

    ConferenceResponseDTO startConference(String conferenceId, String token);

    ConferenceJoinResponseDTO joinConference(String conferenceId, String token);

    String startRecording(String conferenceId, String token);

    String stopRecording(String conferenceId, String token);

    String endConference (String conferenceId, String token);

    List<ConferenceResponseDTO> getConferencesByOrganizer(String token);

    List<ConferenceResponseDTO> getAllConferences();

    ConferenceResponseDTO getConference(String conferenceId);
}