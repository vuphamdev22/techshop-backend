package com.techshop.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CampaignReportResponse {
    private Long campaignId;
    private String name;
    private long sentCount;
    private long openCount;
    private long clickCount;
    private long conversionCount;
    private String openRate;
    private String clickRate;
    private String conversionRate;
    private Double revenueGenerated;
    private long voucherUsageCount;
}
