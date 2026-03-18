package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.live_classes_service.model.SessionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SessionRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public SessionRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(SessionEntity entity) {
        dynamoDBMapper.save(entity);
    }

    public SessionEntity findById(String sessionId) {
        return dynamoDBMapper.load(SessionEntity.class, sessionId);
    }

    public SessionEntity findByRoomId(String roomId) {

        SessionEntity hashKey = new SessionEntity();
        hashKey.setRoomId(roomId);

        DynamoDBQueryExpression<SessionEntity> query =
                new DynamoDBQueryExpression<SessionEntity>()
                        .withIndexName("room-index")
                        .withHashKeyValues(hashKey)
                        .withConsistentRead(false);

        List<SessionEntity> result =
                dynamoDBMapper.query(SessionEntity.class, query);

        return result.isEmpty() ? null : result.get(0);
    }
}