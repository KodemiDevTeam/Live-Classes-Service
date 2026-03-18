package com.example.live_classes_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "Session")
public class SessionEntity {

    private String sessionId;
    private String organizerId;
    private String actionType;
    private String organizerName;
    private String title;
    private String description;
    private String sessionType;
    private String roomId;
    private String status;
    private String scheduledAt;
    private String startedAt;
    private String endedAt;
    private Integer maxParticipants;
    private Boolean isRecording;
    private String recordingId;
    private String recordingUrl;
    private String createdAt;

    @DynamoDBHashKey(attributeName = "sessionId")
    public String getSessionId() { return sessionId; }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "organizer-index", attributeName = "organizerId")
    public String getOrganizerId() { return organizerId; }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "room-index", attributeName = "roomId")
    public String getRoomId() { return roomId; }

}