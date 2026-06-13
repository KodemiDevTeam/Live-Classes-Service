package com.example.live_classes_service.feign;

import com.example.live_classes_service.dto.response.EnrollmentStatusResponse;
import com.example.live_classes_service.dto.response.SessionStatusResponse;
import com.example.live_classes_service.exception.EnrollmentServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EnrollmentClientFallbackFactory implements FallbackFactory<EnrollmentClient> {

    @Override
    public EnrollmentClient create(Throwable cause) {
        return new EnrollmentClient() {
            @Override
            public SessionStatusResponse getCourseEnrollmentStatus(String courseId, String token) {
                log.error("Downstream Enrollment Service failure during getCourseEnrollmentStatus for courseId={}. Cause: {}", 
                        courseId, cause.getMessage(), cause);
                throw new EnrollmentServiceException("Enrollment service is currently unavailable. Access denied.", cause);
            }

            @Override
            public SessionStatusResponse getSessionEnrollmentStatus(String sessionId, String token) {
                log.error("Downstream Enrollment Service failure during getSessionEnrollmentStatus for sessionId={}. Cause: {}", 
                        sessionId, cause.getMessage(), cause);
                throw new EnrollmentServiceException("Enrollment service is currently unavailable. Access denied.", cause);
            }

            @Override
            public SessionStatusResponse getConferenceEnrollmentStatus(String conferenceId, String token) {
                log.error("Downstream Enrollment Service failure during getConferenceEnrollmentStatus for conferenceId={}. Cause: {}", 
                        conferenceId, cause.getMessage(), cause);
                throw new EnrollmentServiceException("Enrollment service is currently unavailable. Access denied.", cause);
            }

            @Override
            public java.util.List<String> getEnrolledLearners(String courseId) {
                log.error("Downstream Enrollment Service failure during getEnrolledLearners for courseId={}. Cause: {}",
                        courseId, cause.getMessage(), cause);
                return java.util.Collections.emptyList();
            }
        };
    }
}
