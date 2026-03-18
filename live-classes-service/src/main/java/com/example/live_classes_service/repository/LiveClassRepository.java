package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.live_classes_service.model.LiveClassEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LiveClassRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public LiveClassRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(LiveClassEntity entity) {
        dynamoDBMapper.save(entity);
    }

    public LiveClassEntity findById(String liveClassId) {
        return dynamoDBMapper.load(LiveClassEntity.class, liveClassId);
    }

    public List<LiveClassEntity> findByCourseId(String courseId) {

        LiveClassEntity hashKey = new LiveClassEntity();
        hashKey.setCourseId(courseId);

        DynamoDBQueryExpression<LiveClassEntity> query =
                new DynamoDBQueryExpression<LiveClassEntity>()
                        .withIndexName("course-index")
                        .withHashKeyValues(hashKey)
                        .withConsistentRead(false);

        return dynamoDBMapper.query(LiveClassEntity.class, query);
    }

    public List<LiveClassEntity> findByTrainerId(String trainerId) {

        LiveClassEntity hashKey = new LiveClassEntity();
        hashKey.setTrainerId(trainerId);

        DynamoDBQueryExpression<LiveClassEntity> query =
                new DynamoDBQueryExpression<LiveClassEntity>()
                        .withIndexName("trainer-index")
                        .withHashKeyValues(hashKey)
                        .withConsistentRead(false);

        return dynamoDBMapper.query(LiveClassEntity.class, query);
    }

    public LiveClassEntity findByRoomId(String roomId) {

        LiveClassEntity hashKey = new LiveClassEntity();
        hashKey.setRoomId(roomId);

        DynamoDBQueryExpression<LiveClassEntity> query =
                new DynamoDBQueryExpression<LiveClassEntity>()
                        .withIndexName("room-index")
                        .withHashKeyValues(hashKey)
                        .withConsistentRead(false);

        List<LiveClassEntity> result =
                dynamoDBMapper.query(LiveClassEntity.class, query);

        return result.isEmpty() ? null : result.get(0);
    }
}