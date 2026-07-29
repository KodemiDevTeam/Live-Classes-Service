package com.example.live_classes_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "Conference")
public class ConferenceEntity {

    private String conferenceId;
    private String organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String actionType;
    private String sessionType;
    private String scheduledAt;
    private String startedAt;
    private String endedAt;
    private Integer maxParticipants;
    private String roomId;
    private String status;
    private Boolean isRecording;
    private String recordingUrl;
    private String createdAt;

    // Added fields for Live Course integration
    private String courseId;
    private String moduleId;
    private String lessonId;
    private String sourceType;
    private Double price;

    @DynamoDBHashKey(attributeName = "conferenceId")
    public String getConferenceId() {
        return conferenceId;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "organizer-index", attributeName = "organizerId")
    public String getOrganizerId() {
        return organizerId;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "room-index", attributeName = "roomId")
    public String getRoomId() {
        return roomId;
    }
}