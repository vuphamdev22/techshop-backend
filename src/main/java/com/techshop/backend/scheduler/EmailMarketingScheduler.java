package com.techshop.backend.scheduler;

import com.techshop.backend.service.EmailMarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailMarketingScheduler {

    private final EmailMarketingService emailMarketingService;

    // Quét mỗi phút để tìm các chiến dịch được lập lịch
    @Scheduled(cron = "0 * * * * *")
    public void runScheduledCampaigns() {
        log.info("CronJob: Scanning for scheduled email campaigns...");
        emailMarketingService.sendScheduledCampaigns();
    }

    // Quét mỗi 30 phút để tìm giỏ hàng bị bỏ quên
    @Scheduled(cron = "0 */30 * * * *")
    public void runAbandonedCartScanner() {
        log.info("CronJob: Scanning for abandoned carts...");
        emailMarketingService.scanAndProcessAbandonedCarts();
    }

    // Quét hàng ngày lúc 01:00 sáng để gửi email Comeback cho inactive users
    @Scheduled(cron = "0 0 1 * * *")
    public void runInactiveUserScanner() {
        log.info("CronJob: Scanning for inactive users...");
        emailMarketingService.scanAndProcessInactiveUsers();
    }

    // Quét hàng ngày lúc 08:00 sáng để chúc mừng sinh nhật
    @Scheduled(cron = "0 0 8 * * *")
    public void runBirthdayScanner() {
        log.info("CronJob: Scanning for birthday users...");
        emailMarketingService.scanAndProcessBirthdayEmails();
    }
}
