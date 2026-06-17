package com.techshop.backend.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmailCampaignRequest {
    private String name;
    private String subject;
    private String templateName;
    private String voucherCode;
    private CampaignFilterRequest targetFilter;
    private LocalDateTime scheduledAt;
}
