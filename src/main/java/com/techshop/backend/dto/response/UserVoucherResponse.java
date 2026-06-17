package com.techshop.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserVoucherResponse {
    private Long id;
    private String code;
    private String discountType;
    private Double discountValue;
    private Double minOrderValue;
    private LocalDateTime endDate;
    private boolean used;
    private LocalDateTime assignedAt;
}
