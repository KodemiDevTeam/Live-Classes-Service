package com.example.live_classes_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "LiveClass")
public class LiveClassEntity {

    private String liveClassId;
    private String courseId;
    private String trainerId;
    private String trainerName;
    private String title;
    private String description;
    private String sessionType;
    private String actionType;
    private String roomId;
    private String status;
    private Boolean isRecording;
    private String scheduledAt;
    private String startedAt;
    private String endedAt;
    private Integer maxParticipants;
    private String recordingUrl;
    private String createdAt;

    @DynamoDBHashKey(attributeName = "liveClassId")
    public String getLiveClassId() {
        return liveClassId;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "course-index", attributeName = "courseId")
    public String getCourseId() {
        return courseId;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "trainer-index", attributeName = "trainerId")
    public String getTrainerId() {
        return trainerId;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "room-index", attributeName = "roomId")
    public String getRoomId() {
        return roomId;
    }

    @DynamoDBAttribute
    public Boolean getIsRecording() {
        return isRecording;
    }

    @DynamoDBAttribute
    public String getRecordingUrl() {
        return recordingUrl;
    }
}