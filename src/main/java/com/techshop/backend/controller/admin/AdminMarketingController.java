package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.request.CampaignFilterRequest;
import com.techshop.backend.dto.request.EmailCampaignRequest;
import com.techshop.backend.dto.response.CampaignReportResponse;
import com.techshop.backend.entity.EmailCampaign;
import com.techshop.backend.service.EmailMarketingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/marketing")
@RequiredArgsConstructor
public class AdminMarketingController {

    private final EmailMarketingService emailMarketingService;

    // 1. Tạo chiến dịch marketing
    @PostMapping("/campaigns")
    public ResponseEntity<EmailCampaign> createCampaign(@RequestBody EmailCampaignRequest request) {
        EmailCampaign campaign = emailMarketingService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(campaign);
    }

    // 2. Ước lượng số lượng user khớp bộ lọc
    @PostMapping("/users/estimate")
    public ResponseEntity<Long> estimateMatchedUsers(@RequestBody CampaignFilterRequest filter) {
        long count = emailMarketingService.estimateMatchedUsers(filter);
        return ResponseEntity.ok(count);
    }

    // 3. Lấy báo cáo hiệu suất chiến dịch
    @GetMapping("/campaigns/{id}/report")
    public ResponseEntity<CampaignReportResponse> getCampaignReport(@PathVariable("id") Long id) {
        CampaignReportResponse report = emailMarketingService.getCampaignReport(id);
        return ResponseEntity.ok(report);
    }

    // 4. Lấy danh sách toàn bộ chiến dịch
    @GetMapping("/campaigns")
    public ResponseEntity<java.util.List<EmailCampaign>> getAllCampaigns() {
        java.util.List<EmailCampaign> campaigns = emailMarketingService.getAllCampaigns();
        return ResponseEntity.ok(campaigns);
    }
}
