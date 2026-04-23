package com.example.live_classes_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "dynamodb.region=eu-north-1",
        "aws.s3.bucket=test-bucket",
        "aws.s3.region=eu-north-1",
        "aws.s3.accessKey=test-key",
        "aws.s3.secretKey=test-secret",
        "videosdk.api.key=test-key",
        "videosdk.api.secret=test-secret",
        "videosdk.api.endpoint=https://api.videosdk.live/v2",
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seQ=="
})
class LiveClassesDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
