package com.example.live_classes_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.example.live_classes_service.model.ConferenceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConferenceRepositoryTest {

    @Mock private DynamoDBMapper dynamoDBMapper;
    @InjectMocks private ConferenceRepository repository;

    private ConferenceEntity entity;

    @BeforeEach
    void setUp() {
        entity = ConferenceEntity.builder()
                .conferenceId("conf-001").status("SCHEDULED").build();
    }

    @Test
    void save_callsDynamoDBMapper() {
        repository.save(entity);
        verify(dynamoDBMapper).save(entity);
    }

    @Test
    void findById_returnsEntity() {
        when(dynamoDBMapper.load(ConferenceEntity.class, "conf-001")).thenReturn(entity);
        ConferenceEntity result = repository.findById("conf-001");
        assertNotNull(result);
        assertEquals("conf-001", result.getConferenceId());
    }

    @Test
    void findById_returnsNull_whenNotFound() {
        when(dynamoDBMapper.load(ConferenceEntity.class, "missing")).thenReturn(null);
        assertNull(repository.findById("missing"));
    }

    @Test
    void updateStatusIfNotStarted_returnsTrue_onSuccess() {
        doNothing().when(dynamoDBMapper).save(any(ConferenceEntity.class), any(DynamoDBSaveExpression.class), any(DynamoDBMapperConfig.class));
        assertTrue(repository.updateStatusIfNotStarted("conf-001", "2026-05-01T10:00:00Z", "STARTED"));
    }

    @Test
    void updateStatusIfNotStarted_returnsFalse_onConditionalCheckFailed() {
        doThrow(new ConditionalCheckFailedException("already started"))
                .when(dynamoDBMapper).save(any(ConferenceEntity.class), any(DynamoDBSaveExpression.class), any(DynamoDBMapperConfig.class));
        assertFalse(repository.updateStatusIfNotStarted("conf-001", "2026-05-01T10:00:00Z", "STARTED"));
    }
}
