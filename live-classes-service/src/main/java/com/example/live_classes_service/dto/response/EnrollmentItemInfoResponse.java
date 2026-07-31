package com.example.live_classes_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentItemInfoResponse {
    private String targetId;
    private String creatorId;
    private String targetType;
    private String title;
    private String pricingType;
    private BigDecimal price;
    private Boolean isFree;
    private Boolean isVerified;
    private String status;
}
