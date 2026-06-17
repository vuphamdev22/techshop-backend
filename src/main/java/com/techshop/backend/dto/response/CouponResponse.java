package com.techshop.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CouponResponse {
    private Long id;
    private String code;
    private String discountType; // "percentage" or "fixed"
    private Double discountValue;
    private Double minOrderValue;
    private Double maxDiscount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
}
