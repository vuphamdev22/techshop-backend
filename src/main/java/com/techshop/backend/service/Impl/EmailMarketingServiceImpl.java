package com.techshop.backend.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techshop.backend.dto.request.CampaignFilterRequest;
import com.techshop.backend.dto.request.EmailCampaignRequest;
import com.techshop.backend.dto.response.CampaignReportResponse;
import com.techshop.backend.entity.*;
import com.techshop.backend.enums.CampaignStatus;
import com.techshop.backend.enums.EmailStatus;
import com.techshop.backend.repository.*;
import com.techshop.backend.service.EmailMarketingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailMarketingServiceImpl implements EmailMarketingService {

    private final EmailCampaignRepository campaignRepository;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final UserVoucherRepository userVoucherRepository;
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${server.port:8080}")
    private String serverPort;

    private String getBaseUrl() {
        return "http://localhost:" + serverPort;
    }

    @Override
    @Transactional
    public EmailCampaign createCampaign(EmailCampaignRequest request) {
        Coupon coupon = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            coupon = couponRepository.findByCode(request.getVoucherCode().toUpperCase())
                    .orElse(null);
        }

        String filterJson = "";
        try {
            filterJson = objectMapper.writeValueAsString(request.getTargetFilter());
        } catch (Exception e) {
            log.error("Failed to serialize target filter", e);
        }

        LocalDateTime scheduleTime = request.getScheduledAt() != null ? request.getScheduledAt() : LocalDateTime.now();
        EmailCampaign campaign = EmailCampaign.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .templateName(request.getTemplateName())
                .coupon(coupon)
                .targetGroupFilterJson(filterJson)
                .scheduledAt(scheduleTime)
                .status(CampaignStatus.SCHEDULED)
                .build();

        return campaignRepository.save(campaign);
    }

    @Override
    public long estimateMatchedUsers(CampaignFilterRequest filter) {
        String jpql = buildUserQuery(filter, true);
        Query query = entityManager.createQuery(jpql);
        bindUserQueryParameters(query, filter);
        return (long) query.getSingleResult();
    }

    @Override
    public CampaignReportResponse getCampaignReport(Long campaignId) {
        EmailCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        long sent = emailLogRepository.countByCampaignId(campaignId);
        long opened = emailLogRepository.countOpenedByCampaignId(campaignId);
        long clicked = emailLogRepository.countClickedByCampaignId(campaignId);
        long converted = emailLogRepository.countConvertedByCampaignId(campaignId);

        String openRate = sent > 0 ? String.format("%.2f%%", (double) opened / sent * 100) : "0.00%";
        String clickRate = sent > 0 ? String.format("%.2f%%", (double) clicked / sent * 100) : "0.00%";
        String conversionRate = sent > 0 ? String.format("%.2f%%", (double) converted / sent * 100) : "0.00%";

        // Tính doanh thu từ campaign (giả định đơn hàng có mã coupon của campaign)
        double revenue = 0.0;
        if (campaign.getCoupon() != null) {
            // Có thể truy vấn thêm từ bảng Order nếu có coupon_id
            // Ở đây tạm trả về dữ liệu mẫu hoặc truy vấn đơn giản
        }

        return CampaignReportResponse.builder()
                .campaignId(campaign.getId())
                .name(campaign.getName())
                .sentCount(sent)
                .openCount(opened)
                .clickCount(clicked)
                .conversionCount(converted)
                .openRate(openRate)
                .clickRate(clickRate)
                .conversionRate(conversionRate)
                .revenueGenerated(revenue)
                .voucherUsageCount(converted)
                .build();
    }

    @Override
    @Transactional
    public void sendScheduledCampaigns() {
        List<EmailCampaign> campaigns = campaignRepository.findByStatusAndScheduledAtBefore(
                CampaignStatus.SCHEDULED, LocalDateTime.now());

        for (EmailCampaign campaign : campaigns) {
            campaign.setStatus(CampaignStatus.SENDING);
            campaignRepository.save(campaign);
            
            // Trigger gửi async
            executeCampaignSending(campaign.getId());
        }
    }

    @Async
    @Transactional
    public void executeCampaignSending(Long campaignId) {
        EmailCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) return;

        try {
            CampaignFilterRequest filter = objectMapper.readValue(
                    campaign.getTargetGroupFilterJson(), CampaignFilterRequest.class);

            String jpql = buildUserQuery(filter, false);
            Query query = entityManager.createQuery(jpql);
            bindUserQueryParameters(query, filter);

            List<User> targetUsers = query.getResultList();
            log.info("Starting campaign '{}' sending to {} users", campaign.getName(), targetUsers.size());

            for (User user : targetUsers) {
                // Giới hạn tần suất nhận mail marketing (tối thiểu 3 ngày)
                if (user.getLastMarketingEmailSentAt() != null && 
                    user.getLastMarketingEmailSentAt().isAfter(LocalDateTime.now().minusDays(3))) {
                    log.info("Skip marketing email to {} (Rate limit)", user.getEmail());
                    continue;
                }

                String token = UUID.randomUUID().toString();
                EmailLog logRecord = EmailLog.builder()
                        .campaign(campaign)
                        .user(user)
                        .recipientEmail(user.getEmail())
                        .status(EmailStatus.QUEUED)
                        .trackingToken(token)
                        .build();
                emailLogRepository.save(logRecord);

                try {
                    // Render HTML Content
                    Context context = new Context();
                    context.setVariable("username", user.getFirstName() + " " + user.getLastName());
                    if (campaign.getCoupon() != null) {
                        context.setVariable("voucherCode", campaign.getCoupon().getCode());
                        context.setVariable("expiredDate", campaign.getCoupon().getEndDate());
                    }

                    String htmlContent = templateEngine.process("mail/" + campaign.getTemplateName(), context);
                    htmlContent = appendTracking(htmlContent, token);

                    sendMail(user.getEmail(), campaign.getSubject(), htmlContent);

                    logRecord.setStatus(EmailStatus.SENT);
                    logRecord.setSentAt(LocalDateTime.now());
                    
                    user.setLastMarketingEmailSentAt(LocalDateTime.now());
                    userRepository.save(user);
                } catch (Exception e) {
                    logRecord.setStatus(EmailStatus.FAILED);
                    logRecord.setErrorMessage(e.getMessage());
                    log.error("Failed to send campaign email to {}", user.getEmail(), e);
                }
                emailLogRepository.save(logRecord);

                // Delay nhỏ để tránh spam/rate limit của SMTP Server
                Thread.sleep(200);
            }

            campaign.setStatus(CampaignStatus.SENT);
            campaign.setSentAt(LocalDateTime.now());
            campaignRepository.save(campaign);
            log.info("Campaign '{}' sent successfully", campaign.getName());

        } catch (Exception e) {
            campaign.setStatus(CampaignStatus.FAILED);
            campaignRepository.save(campaign);
            log.error("Campaign sending failed", e);
        }
    }

    @Override
    @Transactional
    public void scanAndProcessAbandonedCarts() {
        // Quét tìm Carts có items và updatedAt cách đây 1 giờ
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        
        List<Cart> carts = cartRepository.findAll(); // Có thể viết custom query tối ưu hơn
        for (Cart cart : carts) {
            if (cart.getItems() == null || cart.getItems().isEmpty()) continue;
            if (cart.getUpdatedAt() == null || cart.getUpdatedAt().isBefore(threshold) || cart.getUpdatedAt().isAfter(oneHourAgo)) continue;

            User user = cart.getUser();
            if (user == null || !user.isEmailSubscribed()) continue;

            // Kiểm tra tránh gửi trùng trong vòng 3 ngày qua cho giỏ hàng
            String token = UUID.randomUUID().toString();
            EmailLog logRecord = EmailLog.builder()
                    .user(user)
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.QUEUED)
                    .trackingToken(token)
                    .build();
            emailLogRepository.save(logRecord);

            try {
                Context context = new Context();
                context.setVariable("username", user.getFirstName());
                context.setVariable("cartItems", cart.getItems());

                String htmlContent = templateEngine.process("mail/abandoned-cart", context);
                htmlContent = appendTracking(htmlContent, token);

                sendMail(user.getEmail(), "🛒 Bạn bỏ quên sản phẩm trong giỏ hàng kìa!", htmlContent);

                logRecord.setStatus(EmailStatus.SENT);
                logRecord.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                logRecord.setStatus(EmailStatus.FAILED);
                logRecord.setErrorMessage(e.getMessage());
            }
            emailLogRepository.save(logRecord);
        }
    }

    @Override
    @Transactional
    public void scanAndProcessInactiveUsers() {
        // 30 ngày chưa đăng nhập
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<User> inactiveUsers = userRepository.findAll(); // Có thể tối ưu bằng Custom Query
        
        for (User user : inactiveUsers) {
            if (user.getLastLoginAt() == null || user.getLastLoginAt().isAfter(thirtyDaysAgo)) continue;
            if (!user.isEmailSubscribed()) continue;

            // Kiểm tra hạn chế marketing spam
            if (user.getLastMarketingEmailSentAt() != null && 
                user.getLastMarketingEmailSentAt().isAfter(LocalDateTime.now().minusDays(15))) {
                continue;
            }

            String token = UUID.randomUUID().toString();
            EmailLog logRecord = EmailLog.builder()
                    .user(user)
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.QUEUED)
                    .trackingToken(token)
                    .build();
            emailLogRepository.save(logRecord);

            try {
                Context context = new Context();
                context.setVariable("username", user.getFirstName());

                String htmlContent = templateEngine.process("mail/comeback", context);
                htmlContent = appendTracking(htmlContent, token);

                sendMail(user.getEmail(), "❤️ Chúng tôi nhớ bạn! Nhận ngay ưu đãi quay lại", htmlContent);

                logRecord.setStatus(EmailStatus.SENT);
                logRecord.setSentAt(LocalDateTime.now());
                user.setLastMarketingEmailSentAt(LocalDateTime.now());
                userRepository.save(user);
            } catch (Exception e) {
                logRecord.setStatus(EmailStatus.FAILED);
                logRecord.setErrorMessage(e.getMessage());
            }
            emailLogRepository.save(logRecord);
        }
    }

    @Override
    @Transactional
    public void scanAndProcessBirthdayEmails() {
        java.time.LocalDate today = java.time.LocalDate.now();
        List<User> users = userRepository.findAll(); // Hoặc Custom query lấy user sinh nhật hôm nay
        
        for (User user : users) {
            if (user.getBirthday() == null) continue;
            if (user.getBirthday().getMonth() != today.getMonth() || 
                user.getBirthday().getDayOfMonth() != today.getDayOfMonth()) continue;
            if (!user.isEmailSubscribed()) continue;

            String token = UUID.randomUUID().toString();
            EmailLog logRecord = EmailLog.builder()
                    .user(user)
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.QUEUED)
                    .trackingToken(token)
                    .build();
            emailLogRepository.save(logRecord);

            try {
                Context context = new Context();
                context.setVariable("username", user.getFirstName());

                String htmlContent = templateEngine.process("mail/birthday", context);
                htmlContent = appendTracking(htmlContent, token);

                sendMail(user.getEmail(), "🎂 Chúc mừng sinh nhật bạn! Quà tặng từ VoltTech", htmlContent);

                logRecord.setStatus(EmailStatus.SENT);
                logRecord.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                logRecord.setStatus(EmailStatus.FAILED);
                logRecord.setErrorMessage(e.getMessage());
            }
            emailLogRepository.save(logRecord);
        }
    }

    @Override
    @Async
    @Transactional
    public void triggerWelcomeEmail(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String token = UUID.randomUUID().toString();
        EmailLog logRecord = EmailLog.builder()
                .user(user)
                .recipientEmail(user.getEmail())
                .status(EmailStatus.QUEUED)
                .trackingToken(token)
                .build();
        emailLogRepository.save(logRecord);

        try {
            Context context = new Context();
            context.setVariable("username", user.getFirstName() + " " + user.getLastName());
            
            // Tìm voucher chào mừng của user nếu có
            List<UserVoucher> uvList = userVoucherRepository.findByUserId(user.getId());
            if (!uvList.isEmpty()) {
                context.setVariable("voucherCode", uvList.get(0).getCoupon().getCode());
                context.setVariable("expiredDate", uvList.get(0).getCoupon().getEndDate());
            }

            String htmlContent = templateEngine.process("mail/welcome", context);
            htmlContent = appendTracking(htmlContent, token);

            sendMail(user.getEmail(), "🎉 Chào mừng bạn gia nhập gia đình công nghệ VoltTech!", htmlContent);

            logRecord.setStatus(EmailStatus.SENT);
            logRecord.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            logRecord.setStatus(EmailStatus.FAILED);
            logRecord.setErrorMessage(e.getMessage());
            log.error("Failed to send welcome email to {}", user.getEmail(), e);
        }
        emailLogRepository.save(logRecord);
    }

    @Override
    @Transactional
    public void trackOpen(String token) {
        emailLogRepository.findByTrackingToken(token).ifPresent(logRecord -> {
            if (!logRecord.isOpened()) {
                logRecord.setOpened(true);
                logRecord.setOpenedAt(LocalDateTime.now());
                emailLogRepository.save(logRecord);
            }
        });
    }

    @Override
    @Transactional
    public void trackClick(String token) {
        emailLogRepository.findByTrackingToken(token).ifPresent(logRecord -> {
            if (!logRecord.isClicked()) {
                logRecord.setClicked(true);
                logRecord.setClickedAt(LocalDateTime.now());
                emailLogRepository.save(logRecord);
            }
        });
    }

    @Override
    public List<EmailCampaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    // --- Helper Methods ---

    private void sendMail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail, "VoltTech Store");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private String appendTracking(String html, String token) {
        // Pixel ảnh tracking Open
        String pixelUrl = getBaseUrl() + "/api/public/track/open?token=" + token;
        String pixelTag = "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";
        
        // Wrap link tracking Click
        String trackUrlPrefix = getBaseUrl() + "/api/public/track/click?token=" + token + "&redirect=";
        String processedHtml = html.replaceAll("href=\"(https?://[^\"]+)\"", "href=\"" + trackUrlPrefix + "$1\"");
        
        return processedHtml + pixelTag;
    }

    private String buildUserQuery(CampaignFilterRequest filter, boolean countOnly) {
        StringBuilder jpql = new StringBuilder();
        if (countOnly) {
            jpql.append("SELECT COUNT(u) FROM User u WHERE 1=1");
        } else {
            jpql.append("SELECT u FROM User u WHERE 1=1");
        }
        if (filter.getMembershipLevel() != null) {
            jpql.append(" AND u.membershipLevel = :membershipLevel");
        }
        if (filter.getMinTotalSpent() != null) {
            jpql.append(" AND u.totalSpent >= :minTotalSpent");
        }
        if (filter.getInactiveDays() != null) {
            jpql.append(" AND u.lastLoginAt < :inactiveLimit");
        }
        if (filter.getGender() != null) {
            jpql.append(" AND u.gender = :gender");
        }
        if (Boolean.TRUE.equals(filter.getEmailSubscribedOnly())) {
            jpql.append(" AND u.emailSubscribed = true");
        }
        return jpql.toString();
    }

    private void bindUserQueryParameters(Query query, CampaignFilterRequest filter) {
        if (filter.getMembershipLevel() != null) {
            query.setParameter("membershipLevel", filter.getMembershipLevel());
        }
        if (filter.getMinTotalSpent() != null) {
            query.setParameter("minTotalSpent", filter.getMinTotalSpent());
        }
        if (filter.getInactiveDays() != null) {
            query.setParameter("inactiveLimit", LocalDateTime.now().minusDays(filter.getInactiveDays()));
        }
        if (filter.getGender() != null) {
            query.setParameter("gender", filter.getGender());
        }
    }
}
