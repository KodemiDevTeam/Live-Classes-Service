package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.example.live_classes_service.model.ConferenceEntity;
import org.springframework.stereotype.Repository;

@Repository
public class ConferenceRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public ConferenceRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(ConferenceEntity entity) {
        dynamoDBMapper.save(entity);
    }

    public boolean updateStatusIfNotStarted(String conferenceId, String startedAt) {
        try {
            ConferenceEntity entity = new ConferenceEntity();
            entity.setConferenceId(conferenceId);
            entity.setStatus("CONFERENCE_STARTED");
            entity.setStartedAt(startedAt);

            DynamoDBSaveExpression expression = new DynamoDBSaveExpression()
                    .withExpectedEntry("status",
                            new ExpectedAttributeValue()
                                    .withValue(new AttributeValue().withS("SCHEDULED"))
                    );

            dynamoDBMapper.save(entity, expression);
            return true;

        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public ConferenceEntity findById(String id) {
        return dynamoDBMapper.load(ConferenceEntity.class, id);
    }
}