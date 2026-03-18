package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
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

    public ConferenceEntity findById(String id) {
        return dynamoDBMapper.load(ConferenceEntity.class, id);
    }
}