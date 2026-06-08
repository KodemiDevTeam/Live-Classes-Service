package com.example.live_classes_service.feign;

import com.example.live_classes_service.dto.response.EnrollmentStatusResponse;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "enrollment-progress-service", fallbackFactory = EnrollmentClientFallbackFactory.class)
public interface EnrollmentClient {

    @GetMapping("/api/v1/enrollment/status/{courseId}")
    EnrollmentStatusResponse getEnrollmentStatus(
            @PathVariable("courseId") String courseId,
            @RequestHeader("Authorization") String token
    );
    @GetMapping("/api/v1/enrollments/session/{sessionId}/status")
    SessionStatusResponse getSessionEnrollmentStatus(@PathVariable String sessionId,
                                                     @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/enrollments/conference/{conferenceId}/status")
    SessionStatusResponse getConferenceEnrollmentStatus(@PathVariable String conferenceId,
                                                     @RequestHeader("Authorization") String token);
}