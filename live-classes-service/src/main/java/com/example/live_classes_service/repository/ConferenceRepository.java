package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.example.live_classes_service.model.ConferenceEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConferenceRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public ConferenceRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(ConferenceEntity entity) {
        dynamoDBMapper.save(entity);
    }

    public boolean updateStatusIfNotStarted(String conferenceId, String startedAt, String actionType) {
        try {
            ConferenceEntity entity = new ConferenceEntity();
            entity.setConferenceId(conferenceId);
            entity.setStatus("CONFERENCE_STARTED");
            entity.setStartedAt(startedAt);
            entity.setActionType(actionType);

            DynamoDBSaveExpression expression = new DynamoDBSaveExpression()
                    .withExpectedEntry("status",
                            new ExpectedAttributeValue()
                                    .withValue(new AttributeValue().withS("SCHEDULED"))
                    );

            // Use UPDATE_SKIP_NULL_ATTRIBUTES to only update status & startedAt
            // without deleting existing fields like title, description, organizerId, etc.
            DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                    .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES)
                    .build();

            dynamoDBMapper.save(entity, expression, config);
            return true;

        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public ConferenceEntity findById(String id) {
        return dynamoDBMapper.load(ConferenceEntity.class, id);
    }

    public List<ConferenceEntity> findByOrganizerId(String organizerId) {
        ConferenceEntity hashKey = new ConferenceEntity();
        hashKey.setOrganizerId(organizerId);

        DynamoDBQueryExpression<ConferenceEntity> query =
                new DynamoDBQueryExpression<ConferenceEntity>()
                        .withIndexName("organizer-index")
                        .withHashKeyValues(hashKey)
                        .withConsistentRead(false);

        return dynamoDBMapper.query(ConferenceEntity.class, query);
    }

    public List<ConferenceEntity> findAll() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return dynamoDBMapper.scan(ConferenceEntity.class, scanExpression);
    }
}