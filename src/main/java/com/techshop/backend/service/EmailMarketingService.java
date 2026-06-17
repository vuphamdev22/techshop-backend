package com.techshop.backend.service;

import com.techshop.backend.dto.request.CampaignFilterRequest;
import com.techshop.backend.dto.request.EmailCampaignRequest;
import com.techshop.backend.dto.response.CampaignReportResponse;
import com.techshop.backend.entity.EmailCampaign;

public interface EmailMarketingService {
    EmailCampaign createCampaign(EmailCampaignRequest request);
    long estimateMatchedUsers(CampaignFilterRequest filter);
    CampaignReportResponse getCampaignReport(Long campaignId);
    void sendScheduledCampaigns();
    void scanAndProcessAbandonedCarts();
    void scanAndProcessInactiveUsers();
    void scanAndProcessBirthdayEmails();
    void triggerWelcomeEmail(Long userId);
    void trackOpen(String token);
    void trackClick(String token);
    java.util.List<EmailCampaign> getAllCampaigns();
}
