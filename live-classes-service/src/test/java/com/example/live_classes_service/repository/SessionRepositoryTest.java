package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.example.live_classes_service.exception.BadRequestException;
import com.example.live_classes_service.model.SessionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRepositoryTest {

    @Mock private DynamoDBMapper dynamoDBMapper;
    @Mock private AmazonDynamoDB amazonDynamoDB;
    @InjectMocks private SessionRepository repository;

    private SessionEntity entity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repository, "tableName", "Session");
        entity = SessionEntity.builder()
                .sessionId("session-001").organizerId("org-001")
                .roomId("room-001").status("SCHEDULED").build();
    }

    @Test
    void save_callsDynamoDBMapper() {
        repository.save(entity);
        verify(dynamoDBMapper).save(entity);
    }

    @Test
    void findById_returnsEntity() {
        when(dynamoDBMapper.load(SessionEntity.class, "session-001")).thenReturn(entity);
        assertEquals("session-001", repository.findById("session-001").getSessionId());
    }

    @Test
    void findById_returnsNull_whenNotFound() {
        when(dynamoDBMapper.load(SessionEntity.class, "missing")).thenReturn(null);
        assertNull(repository.findById("missing"));
    }

    @Test
    void updateStatusAtomically_returnsTrue_onSuccess() {
        when(amazonDynamoDB.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(new UpdateItemResult());
        assertTrue(repository.updateStatusAtomically(
                "session-001", "SCHEDULED", "SESSION_STARTED", "startedAt", "2026-05-01T10:00:00Z"));
    }

    @Test
    void updateStatusAtomically_returnsFalse_onConditionalCheckFailed() {
        when(amazonDynamoDB.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(new ConditionalCheckFailedException("already updated"));
        assertFalse(repository.updateStatusAtomically(
                "session-001", "SCHEDULED", "SESSION_STARTED", "startedAt", "2026-05-01T10:00:00Z"));
    }

    @Test
    void updateStatusAtomically_throwsBadRequest_onOtherException() {
        when(amazonDynamoDB.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(new RuntimeException("DB error"));
        assertThrows(BadRequestException.class, () ->
                repository.updateStatusAtomically(
                        "session-001", "SCHEDULED", "SESSION_STARTED", "startedAt", "2026-05-01T10:00:00Z"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRoomId_returnsEntity_whenFound() {
        PaginatedQueryList<SessionEntity> mockList = mock(PaginatedQueryList.class);
        when(mockList.isEmpty()).thenReturn(false);
        when(mockList.get(0)).thenReturn(entity);
        when(dynamoDBMapper.query(eq(SessionEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockList);
        assertEquals("session-001", repository.findByRoomId("room-001").getSessionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRoomId_returnsNull_whenNotFound() {
        PaginatedQueryList<SessionEntity> mockList = mock(PaginatedQueryList.class);
        when(mockList.isEmpty()).thenReturn(true);
        when(dynamoDBMapper.query(eq(SessionEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockList);
        assertNull(repository.findByRoomId("room-999"));
    }
}
