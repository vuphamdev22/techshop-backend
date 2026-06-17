package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.AdminChatRequest;
import com.techshop.backend.dto.response.AdminChatResponse;
import com.techshop.backend.entity.*;
import com.techshop.backend.enums.PaymentMethod;
import com.techshop.backend.enums.PaymentStatus;
import com.techshop.backend.repository.*;
import com.techshop.backend.service.AdminChatService;
import com.techshop.backend.service.ChatSessionMemory;
import com.techshop.backend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminChatServiceImpl implements AdminChatService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final ChatSessionMemory chatSessionMemory;
    private final GeminiService geminiService;

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String apiKey;

    @Override
    @Transactional(readOnly = true)
    public AdminChatResponse processChatMessage(AdminChatRequest request) {
        String sessionId = request.getSessionId();
        String message = request.getMessage();

        // 1. Get or Create Session Memory
        ChatSessionMemory.SessionContext session = chatSessionMemory.getOrCreateSession(sessionId);
        
        // 2. Fetch live data for context injection
        List<Payment> allPayments = paymentRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();
        List<Product> allProducts = productRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        List<Coupon> allCoupons = couponRepository.findAll();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime startOfThisWeek = now.minusDays(7);
        LocalDateTime startOfThisMonth = now.minusDays(30);

        // Calculate Revenue and Orders for Today
        double todayRevenue = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfToday))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum() +
                allOrders.stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.COD && Boolean.TRUE.equals(o.getIsPaid()) && o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfToday))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0)
                .sum();

        long todayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfToday))
                .count();

        // Calculate Revenue and Orders for Yesterday
        double yesterdayRevenue = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfYesterday) && p.getCreatedAt().isBefore(startOfToday))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum() +
                allOrders.stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.COD && Boolean.TRUE.equals(o.getIsPaid()) && o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfYesterday) && o.getCreatedAt().isBefore(startOfToday))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0)
                .sum();

        long yesterdayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfYesterday) && o.getCreatedAt().isBefore(startOfToday))
                .count();

        // Calculate Revenue and Orders for Weekly (Last 7 days)
        double weeklyRevenue = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfThisWeek))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum() +
                allOrders.stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.COD && Boolean.TRUE.equals(o.getIsPaid()) && o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfThisWeek))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0)
                .sum();

        long weeklyOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfThisWeek))
                .count();

        // Calculate Revenue and Orders for Monthly (Last 30 days)
        double monthlyRevenue = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfThisMonth))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum() +
                allOrders.stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.COD && Boolean.TRUE.equals(o.getIsPaid()) && o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfThisMonth))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0)
                .sum();

        long monthlyOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfThisMonth))
                .count();

        // Low stock products
        List<Product> lowStockProducts = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() < 10)
                .collect(Collectors.toList());

        // Popular products
        Map<Product, Integer> salesMap = new HashMap<>();
        for (Order order : allOrders) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                if (p != null) {
                    salesMap.put(p, salesMap.getOrDefault(p, 0) + item.getQuantity());
                }
            }
        }
        List<Map.Entry<Product, Integer>> topProducts = salesMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        // Active Vouchers
        List<Coupon> activeVouchers = allCoupons.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .collect(Collectors.toList());

        // 3. Build detailed system instruction injection
        StringBuilder sys = new StringBuilder();
        sys.append("Bạn là VoltBot Admin AI Assistant - Trợ lý trí tuệ nhân tạo chuyên sâu dành cho người quản trị cửa hàng công nghệ VoltTech.\n");
        sys.append("Nhiệm vụ của bạn là hỗ trợ admin phân tích kinh doanh, thống kê doanh thu, đề xuất chiến lược voucher, kiểm tra kho hàng và chuẩn bị hành động tạo mã giảm giá.\n\n");

        sys.append("THÔNG TIN HỆ THỐNG HIỆN TẠI (Dữ liệu thời gian thực cập nhật lúc: ").append(now).append("):\n");
        sys.append("1. THỐNG KÊ DOANH THU & ĐƠN HÀNG (USD):\n")
                .append("- Hôm nay: Doanh thu $").append(String.format("%.2f", todayRevenue)).append(" · ").append(todayOrders).append(" đơn hàng.\n")
                .append("- Hôm qua: Doanh thu $").append(String.format("%.2f", yesterdayRevenue)).append(" · ").append(yesterdayOrders).append(" đơn hàng.\n")
                .append("- Tuần này (7 ngày qua): Doanh thu $").append(String.format("%.2f", weeklyRevenue)).append(" · ").append(weeklyOrders).append(" đơn hàng.\n")
                .append("- Tháng này (30 ngày qua): Doanh thu $").append(String.format("%.2f", monthlyRevenue)).append(" · ").append(monthlyOrders).append(" đơn hàng.\n\n");

        sys.append("2. DANH SÁCH SẢN PHẨM BÁN CHẠY NHẤT:\n");
        if (!topProducts.isEmpty()) {
            topProducts.forEach(e -> sys.append("- ID: ").append(e.getKey().getId()).append(" · ").append(e.getKey().getName()).append(" · Đã bán: ").append(e.getValue()).append(" sản phẩm · Tồn kho: ").append(e.getKey().getStock()).append("\n"));
        } else {
            sys.append("- Chưa có sản phẩm nào bán ra.\n");
        }
        sys.append("\n");

        sys.append("3. SẢN PHẨM SẮP HẾT HÀNG (Tồn kho < 10):\n");
        if (!lowStockProducts.isEmpty()) {
            lowStockProducts.stream().limit(10).forEach(p -> sys.append("- ID: ").append(p.getId()).append(" · ").append(p.getName()).append(" · Tồn kho: ").append(p.getStock()).append(" máy\n"));
        } else {
            sys.append("- Không có sản phẩm nào có số lượng tồn kho dưới 10.\n");
        }
        sys.append("\n");

        sys.append("4. CÁC MÃ GIẢM GIÁ (VOUCHER) ĐANG KÍCH HOẠT:\n");
        if (!activeVouchers.isEmpty()) {
            activeVouchers.forEach(v -> sys.append("- Mã: ").append(v.getCode()).append(" · Loại: ").append(v.getDiscountType()).append(" · Giá trị: ").append(v.getDiscountValue()).append(" · Đơn tối thiểu: $").append(v.getMinOrderValue() != null ? v.getMinOrderValue() : 0.0).append(" · Đã dùng: ").append(v.getUsedCount()).append("/").append(v.getUsageLimit() == null ? "Không giới hạn" : v.getUsageLimit()).append("\n"));
        } else {
            sys.append("- Hiện không có mã giảm giá nào đang kích hoạt.\n");
        }
        sys.append("\n");

        sys.append("5. TỔNG QUAN TÀI KHOẢN KHÁCH HÀNG:\n")
                .append("- Tổng số khách hàng đã đăng ký: ").append(allUsers.size()).append(" thành viên.\n\n");

        sys.append("HƯỚNG DẪN TRẢ LỜI & CÚ PHÁP HÀNH ĐỘNG:\n")
                .append("- Trả lời lịch sự bằng Tiếng Việt, sử dụng định dạng markdown rõ ràng, phân tích súc tích.\n")
                .append("- Nếu được hỏi về voucher hoặc đề xuất chiến lược voucher, hãy đề xuất các voucher cụ thể (ví dụ: giảm giá x% sản phẩm bán chậm hoặc tri ân khách hàng khi doanh số giảm).\n")
                .append("- ĐẶC BIỆT: Khi quản trị viên yêu cầu tạo voucher (hoặc bạn muốn đề xuất một voucher và muốn người dùng tạo ngay bằng 1 click), bạn PHẢI chèn cú pháp hành động này vào cuối cùng phản hồi của bạn:\n")
                .append("  `[ACTION:CREATE_VOUCHER:code=MÃ_GIẢM_GIÁ,type=percentage|fixed,value=SỐ_TIỀN_HOẶC_PHẦN_TRĂM,minSpend=ĐƠN_TỐI_THIỂU]`\n")
                .append("  Ví dụ: `[ACTION:CREATE_VOUCHER:code=VOLT30,type=percentage,value=30.0,minSpend=100.0]`\n")
                .append("  Lưu ý: Chỉ chèn đúng cú pháp này khi admin yêu cầu/đồng ý tạo voucher. Backend sẽ tự bóc tách để gửi thông tin hành động riêng biệt về cho frontend hiển thị giao diện xác nhận trực quan.\n");

        // 4. Call Gemini or fallback if API key is not configured
        String botReplyText;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("Gemini API key is not configured. Falling back to local admin database intelligence.");
            botReplyText = generateLocalAdminResponse(
                    message, todayRevenue, todayOrders, yesterdayRevenue, yesterdayOrders,
                    weeklyRevenue, weeklyOrders, monthlyRevenue, monthlyOrders,
                    topProducts, lowStockProducts, activeVouchers, allUsers.size()
            );
        } else {
            botReplyText = geminiService.generateContent(sys.toString(), message, session.getHistory());
        }

        // 5. Parse action tag from reply text if exists
        AdminChatResponse.ChatAction action = null;
        if (botReplyText != null && botReplyText.contains("[ACTION:CREATE_VOUCHER:")) {
            try {
                int startIdx = botReplyText.indexOf("[ACTION:CREATE_VOUCHER:");
                int endIdx = botReplyText.indexOf("]", startIdx);
                if (endIdx > startIdx) {
                    String actionTag = botReplyText.substring(startIdx, endIdx + 1);
                    String paramsContent = actionTag.substring("[ACTION:CREATE_VOUCHER:".length(), actionTag.length() - 1);
                    String[] params = paramsContent.split(",");
                    Map<String, Object> actionParams = new HashMap<>();
                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim();
                            String val = keyValue[1].trim();
                            if ("value".equals(key) || "minSpend".equals(key)) {
                                actionParams.put(key, Double.parseDouble(val));
                            } else {
                                actionParams.put(key, val);
                            }
                        }
                    }

                    action = AdminChatResponse.ChatAction.builder()
                            .type("CREATE_VOUCHER")
                            .parameters(actionParams)
                            .build();

                    // Remove action tag from text to keep chat clean
                    botReplyText = botReplyText.replace(actionTag, "").trim();
                }
            } catch (Exception e) {
                log.error("Failed to parse admin chat action tag", e);
            }
        }

        // 6. Update Session History
        session.addMessage("user", message);
        session.addMessage("bot", botReplyText);

        // 7. Setup Quick Replies
        List<String> quickReplies = Arrays.asList(
                "📊 Báo cáo doanh thu",
                "📦 Phân tích kho hàng",
                "🎁 Đề xuất chiến dịch voucher",
                "📈 Phân tích sản phẩm bán chạy"
        );

        return AdminChatResponse.builder()
                .text(botReplyText)
                .quickReplies(quickReplies)
                .action(action)
                .build();
    }

    private String generateLocalAdminResponse(
            String message,
            double todayRevenue, long todayOrders,
            double yesterdayRevenue, long yesterdayOrders,
            double weeklyRevenue, long weeklyOrders,
            double monthlyRevenue, long monthlyOrders,
            List<Map.Entry<Product, Integer>> topProducts,
            List<Product> lowStockProducts,
            List<Coupon> activeVouchers,
            int totalUsers
    ) {
        String lower = message.toLowerCase();
        StringBuilder sb = new StringBuilder();

        if (lower.contains("tạo mã") || lower.contains("tạo voucher")) {
            // Default params
            String code = "VOLT20";
            double val = 20.0;
            double spend = 100.0;

            // Try to find coupon code: all uppercase alphanumeric, length >= 4
            String[] words = message.split("\\s+");
            for (String w : words) {
                String cleanWord = w.replaceAll("[^a-zA-Z0-9]", "");
                if (cleanWord.length() >= 4 && cleanWord.toUpperCase().equals(cleanWord) && cleanWord.matches("^[A-Z0-9]+$")) {
                    code = cleanWord;
                    break;
                }
            }

            // Parse percentage value
            if (lower.contains("giảm")) {
                int idx = lower.indexOf("giảm");
                String sub = lower.substring(idx + 4).trim();
                StringBuilder num = new StringBuilder();
                for (char c : sub.toCharArray()) {
                    if (Character.isDigit(c) || c == '.') {
                        num.append(c);
                    } else if (num.length() > 0) {
                        break;
                    }
                }
                if (num.length() > 0) {
                    try {
                        val = Double.parseDouble(num.toString());
                    } catch (Exception ignored) {}
                }
            }

            // Parse min spend value
            if (lower.contains("đơn từ") || lower.contains("tối thiểu")) {
                int idx = lower.contains("đơn từ") ? lower.indexOf("đơn từ") + 6 : lower.indexOf("tối thiểu") + 9;
                String sub = lower.substring(idx).trim();
                StringBuilder num = new StringBuilder();
                for (char c : sub.toCharArray()) {
                    if (Character.isDigit(c) || c == '.') {
                        num.append(c);
                    } else if (num.length() > 0) {
                        break;
                    }
                }
                if (num.length() > 0) {
                    try {
                        spend = Double.parseDouble(num.toString());
                    } catch (Exception ignored) {}
                }
            }

            return "Tôi đã chuẩn bị sẵn hành động tạo mã giảm giá **" + code + "** giảm **" + (val % 1 == 0 ? String.format("%.0f", val) : val) + "%** cho các đơn hàng tối thiểu **$" + String.format("%.0f", spend) + "**. Quản trị viên vui lòng xác nhận bằng cách bấm nút bên dưới nhé! 👌\n" +
                    "[ACTION:CREATE_VOUCHER:code=" + code + ",type=percentage,value=" + val + ",minSpend=" + spend + "]";
        }

        if (lower.contains("doanh thu") || lower.contains("doanh số") || lower.contains("báo cáo") || lower.contains("tiền")) {
            sb.append("📊 **Báo cáo Doanh thu & Đơn hàng (USD):**\n\n");
            sb.append("- **Hôm nay:** Doanh thu **$").append(String.format("%.2f", todayRevenue)).append("** · **").append(todayOrders).append("** đơn hàng.\n");
            sb.append("- **Hôm qua:** Doanh thu **$").append(String.format("%.2f", yesterdayRevenue)).append("** · **").append(yesterdayOrders).append("** đơn hàng.\n");
            sb.append("- **Tuần này (7 ngày qua):** Doanh thu **$").append(String.format("%.2f", weeklyRevenue)).append("** · **").append(weeklyOrders).append("** đơn hàng.\n");
            sb.append("- **Tháng này (30 ngày qua):** Doanh thu **$").append(String.format("%.2f", monthlyRevenue)).append("** · **").append(monthlyOrders).append("** đơn hàng.\n\n");
            sb.append("Dữ liệu này được thống kê trực tiếp từ đơn hàng của hệ thống. 👌");
            return sb.toString();
        }

        if (lower.contains("sắp hết hàng") || lower.contains("tồn kho") || lower.contains("kho")) {
            sb.append("📦 **Sản phẩm sắp hết hàng (Tồn kho < 10):**\n\n");
            if (!lowStockProducts.isEmpty()) {
                lowStockProducts.stream().limit(10).forEach(p ->
                        sb.append("- **").append(p.getName()).append("** (ID: ").append(p.getId()).append(") · Số lượng còn lại: **").append(p.getStock()).append("** máy.\n")
                );
                sb.append("\nGợi ý: Bạn có thể tạo chương trình khuyến mãi xả kho cho các sản phẩm trên. 👌");
            } else {
                sb.append("Tuyệt vời! Hiện tại không có sản phẩm nào có số lượng tồn kho dưới 10 máy. 👌");
            }
            return sb.toString();
        }

        if (lower.contains("bán chạy") || lower.contains("hot") || lower.contains("chạy nhất")) {
            sb.append("📈 **Top 5 Sản phẩm bán chạy nhất hệ thống:**\n\n");
            if (!topProducts.isEmpty()) {
                topProducts.forEach(e ->
                        sb.append("- **").append(e.getKey().getName()).append("** · Đã bán: **").append(e.getValue()).append("** sản phẩm · Tồn kho hiện tại: ").append(e.getKey().getStock()).append("\n")
                );
            } else {
                sb.append("Chưa có đơn hàng nào hoàn tất để thống kê sản phẩm bán chạy. 👌");
            }
            return sb.toString();
        }

        if (lower.contains("voucher") || lower.contains("mã giảm giá") || lower.contains("khuyến mãi") || lower.contains("đề xuất")) {
            sb.append("🎁 **Đề xuất chiến lược Voucher & Khuyến mãi từ Trợ lý AI:**\n\n");
            if (!lowStockProducts.isEmpty()) {
                Product target = lowStockProducts.get(0);
                sb.append("1. **Chiến lược đẩy hàng tồn:** Sản phẩm **").append(target.getName()).append("** chỉ còn **").append(target.getStock()).append("** máy trong kho. Đề xuất tạo mã **DISCOUNT15** giảm **15%** để thúc đẩy bán nhanh.\n\n");
            }
            sb.append("2. **Kích cầu doanh số:** Đề xuất tạo mã giảm giá **SUMMER30** giảm **30%** cho các đơn hàng trị giá từ **$100.00** trở lên để thu hút thêm người dùng mới.\n\n");
            sb.append("Bạn có muốn tạo mã giảm giá **VOLT30** (Giảm 30%, đơn từ $100) để kích hoạt chiến dịch ngay lúc này không?\n");
            sb.append("Tôi đã chuẩn bị sẵn nút kích hoạt nhanh bên dưới. 👇\n");
            sb.append("[ACTION:CREATE_VOUCHER:code=VOLT30,type=percentage,value=30.0,minSpend=100.0]");
            return sb.toString();
        }

        // General default greeting
        sb.append("🤖 **VoltBot Local Assistant (Offline Mode):**\n\n");
        sb.append("Xin chào! Hiện tại máy chủ AI đang chạy ngoại tuyến. Tôi vẫn có thể giúp bạn kiểm tra hệ thống. Bạn hãy thử đặt câu hỏi như:\n");
        sb.append("- *Doanh thu hôm nay/tuần này/tháng này*\n");
        sb.append("- *Sản phẩm bán chạy nhất/sắp hết hàng*\n");
        sb.append("- *Đề xuất chiến dịch voucher* hoặc ra lệnh *'Tạo mã VOLT20 giảm 20% đơn từ 150$'*");
        return sb.toString();
    }

    @Override
    public void clearContext(String sessionId) {
        chatSessionMemory.clearSession(sessionId);
    }
}
