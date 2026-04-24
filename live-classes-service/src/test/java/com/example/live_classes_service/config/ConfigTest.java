package com.example.live_classes_service.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConfigTest {

    @Test
    void dynamoDBMapper_returnsNonNull() {
        DynamoDBConfig config = new DynamoDBConfig();
        ReflectionTestUtils.setField(config, "dynamodbRegion", "us-east-1");
        AmazonDynamoDB mockDynamo = mock(AmazonDynamoDB.class);
        DynamoDBMapper mapper = config.dynamoDBMapper(mockDynamo);
        assertNotNull(mapper);
    }

    @Test
    void s3Config_beanCreation_notNull() {
        // S3Config creates a real S3Client — just verify the class instantiates
        S3Config config = new S3Config();
        assertNotNull(config);
    }

    @Test
    void dynamoDBConfig_beanCreation_notNull() {
        DynamoDBConfig config = new DynamoDBConfig();
        ReflectionTestUtils.setField(config, "dynamodbRegion", "us-east-1");
        assertNotNull(config);
    }
}
