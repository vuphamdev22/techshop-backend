package com.techshop.backend.service.Impl;

import com.techshop.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    @Async
    public void sendWelcomeEmail(String recipientEmail, String recipientName) {
        log.info("Sending asynchronous welcome email to {}", recipientEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "VoltTech Store");
            helper.setTo(recipientEmail);
            helper.setSubject("🎉 Chào mừng bạn gia nhập gia đình công nghệ VoltTech!");
            
            // Design a stunning, ultra-premium responsive HTML email layout
            String htmlContent = "<div style=\"font-family: 'Outfit', 'Inter', sans-serif; max-width: 600px; margin: 0 auto; background: #fafafa; border-radius: 24px; overflow: hidden; border: 1px solid #eaeaea; box-shadow: 0 10px 30px rgba(0,0,0,0.05);\">" +
                    // Header Banner
                    "  <div style=\"background: linear-gradient(135deg, #0F2027 0%, #203A43 50%, #2C5364 100%); padding: 40px 20px; text-align: center; color: white;\">" +
                    "    <h1 style=\"margin: 0; font-size: 32px; font-weight: 800; letter-spacing: -1px; text-shadow: 0 2px 4px rgba(0,0,0,0.3);\">⚡ VOLTTECH</h1>" +
                    "    <p style=\"margin: 5px 0 0 0; font-size: 14px; opacity: 0.8; text-transform: uppercase; letter-spacing: 2px;\">Trải nghiệm mua sắm công nghệ tương lai</p>" +
                    "  </div>" +
                    // Body Card
                    "  <div style=\"padding: 40px 30px; background: white;\">" +
                    "    <h2 style=\"margin-top: 0; font-size: 22px; color: #1e293b; font-weight: 700;\">Xin chào, " + recipientName + "! 👋</h2>" +
                    "    <p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">" +
                    "      Chúc mừng bạn đã đăng ký tài khoản thành công tại **VoltTech** - Hệ thống phân phối sản phẩm công nghệ cao cấp chính hãng hàng đầu." +
                    "    </p>" +
                    "    <p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">" +
                    "      Từ nay, bạn có thể dễ dàng trải nghiệm các dịch vụ mua sắm tiện lợi, theo dõi trạng thái đơn hàng thời gian thực, và đặc biệt là nhận được sự hỗ trợ tư vấn 24/7 từ **VoltBot AI** siêu thông minh của chúng tôi!" +
                    "    </p>" +
                    // Call to action button
                    "    <div style=\"text-align: center; margin: 35px 0;\">" +
                    "      <a href=\"http://localhost:5173\" style=\"background: #2563eb; color: white; padding: 14px 28px; border-radius: 12px; text-decoration: none; font-weight: 700; font-size: 15px; display: inline-block; box-shadow: 0 4px 15px rgba(37,99,235,0.3); transition: all 0.3s;\">Mua Sắm Ngay 🚀</a>" +
                    "    </div>" +
                    // Welcome Gift Card
                    "    <div style=\"background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); border: 1px dashed #bfdbfe; border-radius: 16px; padding: 20px; margin-bottom: 25px; text-align: center;\">" +
                    "      <span style=\"font-size: 12px; text-transform: uppercase; font-weight: 700; color: #2563eb; letter-spacing: 1px;\">Quà tặng đặc biệt của bạn</span>" +
                    "      <h3 style=\"margin: 10px 0; color: #1e3a8a; font-size: 20px; font-weight: 800;\">Giảm ngay 10% đơn hàng đầu</h3>" +
                    "      <p style=\"margin: 0; font-size: 13px; color: #1e40af;\">Nhập mã ưu đãi sau tại trang thanh toán:</p>" +
                    "      <div style=\"background: white; border: 1px solid #bfdbfe; display: inline-block; padding: 8px 16px; border-radius: 8px; font-family: monospace; font-size: 16px; font-weight: 700; color: #1e3a8a; margin-top: 10px;\">VOLT10</div>" +
                    "    </div>" +
                    "    <p style=\"font-size: 13px; color: #64748b; line-height: 1.5;\">" +
                    "      *Lưu ý: Bạn nhận được email này vì đã đăng ký tài khoản thành công bằng địa chỉ email này tại hệ thống VoltTech. Vui lòng không chia sẻ mã tài khoản của bạn cho người khác." +
                    "    </p>" +
                    "  </div>" +
                    // Footer
                    "  <div style=\"background: #f1f5f9; padding: 25px 20px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8;\">" +
                    "    <p style=\"margin: 0 0 5px 0;\">© 2026 VoltTech Inc. All rights reserved.</p>" +
                    "    <p style=\"margin: 0;\">Hotline hỗ trợ: 1900 xxxx · Email: cskh@volttech.com.vn</p>" +
                    "  </div>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Welcome email sent successfully to {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", recipientEmail, e);
        }
    }
}
