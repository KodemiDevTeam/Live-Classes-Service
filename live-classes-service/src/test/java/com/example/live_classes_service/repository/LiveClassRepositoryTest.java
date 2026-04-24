package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.example.live_classes_service.model.LiveClassEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveClassRepositoryTest {

    @Mock private DynamoDBMapper dynamoDBMapper;
    @InjectMocks private LiveClassRepository repository;

    private LiveClassEntity entity;

    @BeforeEach
    void setUp() {
        entity = LiveClassEntity.builder()
                .liveClassId("lc-001").courseId("course-001")
                .trainerId("trainer-001").roomId("room-001")
                .status("SCHEDULED").build();
    }

    @Test
    void save_callsDynamoDBMapper() {
        repository.save(entity);
        verify(dynamoDBMapper).save(entity);
    }

    @Test
    void findById_returnsEntity() {
        when(dynamoDBMapper.load(LiveClassEntity.class, "lc-001")).thenReturn(entity);
        assertEquals("lc-001", repository.findById("lc-001").getLiveClassId());
    }

    @Test
    void findById_returnsNull_whenNotFound() {
        when(dynamoDBMapper.load(LiveClassEntity.class, "missing")).thenReturn(null);
        assertNull(repository.findById("missing"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCourseId_returnsList() {
        PaginatedQueryList<LiveClassEntity> mockList = mock(PaginatedQueryList.class);
        when(dynamoDBMapper.query(eq(LiveClassEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockList);
        List<LiveClassEntity> result = repository.findByCourseId("course-001");
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByTrainerId_returnsList() {
        PaginatedQueryList<LiveClassEntity> mockList = mock(PaginatedQueryList.class);
        when(dynamoDBMapper.query(eq(LiveClassEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockList);
        List<LiveClassEntity> result = repository.findByTrainerId("trainer-001");
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRoomId_returnsEntity_whenFound() {
        PaginatedQueryList<LiveClassEntity> mockList = mock(PaginatedQueryList.class);
        when(mockList.isEmpty()).thenReturn(false);
        when(mockList.get(0)).thenReturn(entity);
        when(dynamoDBMapper.query(eq(LiveClassEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockList);
        assertEquals("lc-001", repository.findByRoomId("room-001").getLiveClassId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRoomId_returnsNull_whenNotFound() {
        PaginatedQueryList<LiveClassEntity> mockList = mock(PaginatedQueryList.class);
        when(mockList.isEmpty()).thenReturn(true);
        when(dynamoDBMapper.query(eq(LiveClassEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockList);
        assertNull(repository.findByRoomId("room-999"));
    }

    @Test
    void updateStatusIfNotStarted_returnsTrue_onSuccess() {
        doNothing().when(dynamoDBMapper).save(any(LiveClassEntity.class), any(DynamoDBSaveExpression.class));
        assertTrue(repository.updateStatusIfNotStarted("lc-001", "2026-05-01T10:00:00Z"));
    }

    @Test
    void updateStatusIfNotStarted_returnsFalse_onConditionalCheckFailed() {
        doThrow(new ConditionalCheckFailedException("already started"))
                .when(dynamoDBMapper).save(any(LiveClassEntity.class), any(DynamoDBSaveExpression.class));
        assertFalse(repository.updateStatusIfNotStarted("lc-001", "2026-05-01T10:00:00Z"));
    }
}
