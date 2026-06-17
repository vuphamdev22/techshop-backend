package com.techshop.backend.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CouponRequest {
    private String code;
    private String discountType; // "percentage" or "fixed"
    private Double discountValue;
    private Double minOrderValue;
    private Double maxDiscount;
    private Integer usageLimit;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
}
