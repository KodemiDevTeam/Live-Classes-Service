package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.model.SessionEntity;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class SessionRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private final AmazonDynamoDB amazonDynamoDB;

    @Value("${aws.dynamodb.table.session:Session}")
    private String tableName;
    public SessionRepository(DynamoDBMapper dynamoDBMapper,
                             AmazonDynamoDB amazonDynamoDB) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.amazonDynamoDB = amazonDynamoDB;
    }

    // ================= BASIC =================

    public void save(SessionEntity entity) {
        dynamoDBMapper.save(entity);
    }

    public SessionEntity findById(String sessionId) {
        return dynamoDBMapper.load(SessionEntity.class, sessionId);
    }

    // ================= HIGH SCALE UPDATE =================

    public boolean updateStatusAtomically(
            String sessionId,
            String expectedStatus,
            String newStatus,
            String timeField,
            String timeValue,
            String actionType
    ) {
        try {

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("sessionId", new AttributeValue().withS(sessionId));

            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":expected", new AttributeValue().withS(expectedStatus));
            values.put(":newStatus", new AttributeValue().withS(newStatus));
            values.put(":time", new AttributeValue().withS(timeValue));
            values.put(":actionType", new AttributeValue().withS(actionType));

            Map<String, String> names = new HashMap<>();
            names.put("#status", "status");
            names.put("#timeField", timeField);
            names.put("#actionTypeField", "actionType");

            UpdateItemRequest request = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(key)
                    .withUpdateExpression("SET #status = :newStatus, #timeField = :time, #actionTypeField = :actionType")
                    .withConditionExpression("#status = :expected")
                    .withExpressionAttributeNames(names)
                    .withExpressionAttributeValues(values);

            amazonDynamoDB.updateItem(request);

            log.info("Atomic update success for sessionId: {}", sessionId);
            return true;

        } catch (ConditionalCheckFailedException e) {
            log.warn("Atomic update failed (already updated): {}", sessionId);
            return false;
        } catch (Exception e) {
            log.error("DynamoDB update failed", e);
            throw new BadRequestException("Database update failed");
        }
    }

    // ================= QUERY =================

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