package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.ChatRequest;
import com.techshop.backend.dto.ChatResponse;
import com.techshop.backend.dto.response.ProductResponse;
import com.techshop.backend.entity.ChatFaq;
import com.techshop.backend.entity.Coupon;
import com.techshop.backend.entity.Order;
import com.techshop.backend.entity.Product;
import com.techshop.backend.entity.User;
import com.techshop.backend.mapper.ProductMapper;
import com.techshop.backend.repository.ChatFaqRepository;
import com.techshop.backend.repository.CouponRepository;
import com.techshop.backend.repository.OrderRepository;
import com.techshop.backend.repository.ProductRepository;
import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.service.ChatService;
import com.techshop.backend.service.ChatSessionMemory;
import com.techshop.backend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final ChatFaqRepository chatFaqRepository;
    private final ChatSessionMemory chatSessionMemory;
    private final GeminiService geminiService;
    private final ProductMapper productMapper;

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String apiKey;

    @Override
    @Transactional(readOnly = true)
    public ChatResponse processChatMessage(ChatRequest request) {
        String sessionId = request.getSessionId();
        String message = request.getMessage();
        String email = request.getEmail();

        // 1. Get or Create Session Memory
        ChatSessionMemory.SessionContext session = chatSessionMemory.getOrCreateSession(sessionId);
        Map<String, String> attributes = session.getAttributes();
        String lowerMsg = message.toLowerCase();

        // 2. Clear Session context if requested
        if (lowerMsg.contains("quay lại") || lowerMsg.contains("menu chính") || lowerMsg.contains("menu") ||
                lowerMsg.contains("bắt đầu") || lowerMsg.contains("reset") || lowerMsg.equals("🔙 menu chính")) {
            session.clear();
            attributes = session.getAttributes();
        }

        // 3. Extract Context Filters using AI if available, otherwise local fallback
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String extractionPrompt = buildExtractionPrompt(message, session.getHistory(), attributes);
                String jsonResult = geminiService.generateContent(
                    "You are a search parameter extractor. Output ONLY valid JSON matching the schema.",
                    extractionPrompt,
                    null
                );

                if (jsonResult.contains("```")) {
                    jsonResult = jsonResult.replaceAll("```json|```", "").trim();
                }

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(jsonResult);

                boolean contextSwitch = jsonNode.path("contextSwitch").asBoolean();
                if (contextSwitch) {
                    log.info("[ChatSession] Context switch detected by AI. Clearing session attributes.");
                    session.clear();
                    attributes = session.getAttributes();
                }

                if (jsonNode.has("category") && !jsonNode.get("category").isNull()) {
                    String cat = jsonNode.get("category").asText();
                    if (!cat.equalsIgnoreCase("null") && !cat.isEmpty()) {
                        // If category changes, clear price/brand/search filters
                        String oldCat = attributes.get("category");
                        if (oldCat != null && !oldCat.equalsIgnoreCase(cat)) {
                            log.info("[ChatSession] Category changed from '{}' to '{}'. Resetting filters.", oldCat, cat);
                            attributes.remove("brand");
                            attributes.remove("minPrice");
                            attributes.remove("maxPrice");
                            attributes.remove("searchQuery");
                        }
                        attributes.put("category", cat);
                    }
                }
                if (jsonNode.has("brand") && !jsonNode.get("brand").isNull()) {
                    String br = jsonNode.get("brand").asText();
                    if (!br.equalsIgnoreCase("null") && !br.isEmpty()) {
                        attributes.put("brand", br);
                    }
                }
                if (jsonNode.has("minPrice") && !jsonNode.get("minPrice").isNull()) {
                    double minP = jsonNode.get("minPrice").asDouble();
                    if (minP > 0) {
                        attributes.put("minPrice", String.valueOf(minP));
                    }
                }
                if (jsonNode.has("maxPrice") && !jsonNode.get("maxPrice").isNull()) {
                    double maxP = jsonNode.get("maxPrice").asDouble();
                    if (maxP > 0) {
                        attributes.put("maxPrice", String.valueOf(maxP));
                    }
                }
                if (jsonNode.has("searchQuery") && !jsonNode.get("searchQuery").isNull()) {
                    String query = jsonNode.get("searchQuery").asText();
                    if (!query.equalsIgnoreCase("null") && !query.isEmpty()) {
                        attributes.put("searchQuery", query);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to extract search filters using AI. Falling back to keyword parser.", e);
                detectAndHandleCategorySwitch(lowerMsg, attributes, session);
                attributes = session.getAttributes();
                extractBrandFilter(lowerMsg, attributes);
                extractPriceFilter(lowerMsg, attributes);
            }
        } else {
            detectAndHandleCategorySwitch(lowerMsg, attributes, session);
            attributes = session.getAttributes();
            extractBrandFilter(lowerMsg, attributes);
            extractPriceFilter(lowerMsg, attributes);
        }

        // 4. Query live products based on context
        List<Product> allProducts = productRepository.findAll();
        List<Product> matchedProducts = filterProducts(allProducts, attributes, lowerMsg);

        // 5. Query active coupons (vouchers)
        List<Coupon> activeCoupons = couponRepository.findAll().stream()
                .filter(c -> c != null && Boolean.TRUE.equals(c.getIsActive()))
                .collect(Collectors.toList());

        // 6. Query user order context
        String orderContext = "";
        String userContext = "";
        if (email != null && !email.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userContext = "Khách hàng đăng nhập: " + user.getFirstName() + " " + user.getLastName() + " (Email: " + user.getEmail() + ", SĐT: " + (user.getPhone() != null ? user.getPhone() : "Chưa cập nhật") + ").";
                List<Order> orders = orderRepository.findByUserId(user.getId());
                if (!orders.isEmpty()) {
                    StringBuilder sb = new StringBuilder("Lịch sử đơn hàng:\n");
                    orders.forEach(o -> sb.append("- Đơn hàng #").append(o.getId())
                            .append(" đặt ngày ").append(o.getCreatedAt())
                            .append(", Trạng thái giao hàng: ").append(o.getStatus())
                            .append(", Trạng thái thanh toán: ").append(o.getIsPaid() ? "Đã thanh toán" : "Chưa thanh toán")
                            .append(", Tổng tiền: $").append(o.getTotalPrice()).append("\n"));
                    orderContext = sb.toString();
                } else {
                    orderContext = "Khách hàng chưa đặt đơn hàng nào.";
                }
            }
        }

        // 7. Smart FAQ Match for Policy Context
        List<ChatFaq> allFaqs = chatFaqRepository.findAll();
        String matchedFaqContext = getMatchedFaqContext(lowerMsg, allFaqs);

        // 8. Build System Prompt & Gemini API Call
        String systemInstruction = buildSystemInstruction(matchedProducts, allProducts, activeCoupons, userContext, orderContext, matchedFaqContext, attributes);
        
        // Call Gemini or Dynamic Local Intelligence Fallback
        String botReplyText;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("Gemini API key is not configured. Falling back to local database-backed intelligence.");
            botReplyText = generateDynamicLocalResponse(message, matchedProducts, allProducts, activeCoupons, matchedFaqContext, attributes, orderContext);
        } else {
            botReplyText = geminiService.generateContent(systemInstruction, message, session.getHistory());
        }

        // 9. Update Session History (limit context memory)
        session.addMessage("user", message);
        session.addMessage("bot", botReplyText);

        // 10. Generate Quick Action Replies based on flow
        List<String> quickReplies = generateQuickReplies(attributes);

        List<ProductResponse> productResponses = matchedProducts.stream()
                .limit(3)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .text(botReplyText)
                .products(productResponses)
                .quickReplies(quickReplies)
                .build();
    }

    @Override
    public List<ChatFaq> getAllFaqs() {
        return chatFaqRepository.findAll();
    }

    /**
     * Detects if the new message causes a category switch.
     * If the detected category differs from the current one, clears all filters before setting the new category.
     * If no category keyword is found in the message, the old category remains (context is preserved).
     */
    private void detectAndHandleCategorySwitch(String text, Map<String, String> attributes, ChatSessionMemory.SessionContext session) {
        String detectedCategory = detectCategoryFromText(text);
        if (detectedCategory != null) {
            String oldCategory = attributes.get("category");
            if (oldCategory != null && !oldCategory.equalsIgnoreCase(detectedCategory)) {
                // Category has switched - clear all old filters
                log.info("[ChatSession] Local parser: category switch detected '{}' -> '{}'. Clearing session.", oldCategory, detectedCategory);
                session.clear();
            }
            session.getAttributes().put("category", detectedCategory);
        }
    }

    private String detectCategoryFromText(String text) {
        // Điện thoại keywords first (higher priority over gaming phones)
        if (text.contains("điện thoại") || text.contains("smartphone") || text.contains("iphone") ||
                text.contains("galaxy") || text.contains("pixel") || text.contains("android") ||
                text.contains("oneplus")) {
            return "Smartphones";
        } else if (text.contains("laptop") || text.contains("máy tính xách tay") || text.contains("macbook") ||
                text.contains("notebook")) {
            return "Laptops";
        } else if (text.contains("máy tính để bàn") || text.contains("desktop") || text.contains("workstation") ||
                (text.contains("pc") && !text.contains("spec"))) {
            return "Desktop PCs";
        } else if (text.contains("linh kiện") || text.contains("cpu") || text.contains("gpu") ||
                text.contains("card màn hình") || text.contains("ram") || text.contains("ssd")) {
            return "Components";
        } else if (text.contains("màn hình") || text.contains("monitor")) {
            return "Monitors";
        } else if (text.contains("gaming") || text.contains("chơi game") || text.contains("tai nghe") ||
                text.contains("headset") || text.contains("ps5") || text.contains("xbox") ||
                text.contains("steam deck") || text.contains("nintendo") || text.contains("playstation")) {
            return "Gaming";
        } else if (text.contains("bàn phím") || text.contains("chuột") || text.contains("keyboard") ||
                text.contains("mouse") || text.contains("ngoại vi") || text.contains("micro") ||
                text.contains("stream deck")) {
            return "Peripherals";
        }
        return null; // No category keyword detected - don't change existing filter
    }

    private void extractCategoryFilter(String text, Map<String, String> attributes) {
        String detected = detectCategoryFromText(text);
        if (detected != null) {
            attributes.put("category", detected);
        }
    }

    private void extractBrandFilter(String text, Map<String, String> attributes) {
        String[] brands = {"apple", "dell", "asus", "samsung", "sony", "bose", "garmin", "nintendo", "logitech", "keychron", "google", "oneplus"};
        for (String brand : brands) {
            if (text.contains(brand)) {
                // Capitalize brand name
                String capitalized = brand.substring(0, 1).toUpperCase() + brand.substring(1);
                attributes.put("brand", capitalized);
                break;
            }
        }
    }

    private void extractPriceFilter(String text, Map<String, String> attributes) {
        if (text.contains("15 đến 20") || text.contains("15-20") || (text.contains("15") && text.contains("20") && (text.contains("triệu") || text.contains("tr")))) {
            attributes.put("minPrice", "600");
            attributes.put("maxPrice", "800");
        } else if (text.contains("dưới 500") || text.contains("dưới 500$")) {
            attributes.put("maxPrice", "500");
        } else if (text.contains("dưới 1000") || text.contains("dưới 1000$")) {
            attributes.put("maxPrice", "1000");
        } else if (text.contains("dưới 1500") || text.contains("dưới 1500$")) {
            attributes.put("maxPrice", "1500");
        } else if (text.contains("dưới 2000") || text.contains("dưới 2000$")) {
            attributes.put("maxPrice", "2000");
        } else if (text.contains("dưới 10 triệu") || text.contains("dưới 10tr")) {
            attributes.put("maxPrice", "420");
        } else if (text.contains("dưới 15 triệu") || text.contains("dưới 15tr")) {
            attributes.put("maxPrice", "650");
        } else if (text.contains("dưới 20 triệu") || text.contains("dưới 20tr")) {
            attributes.put("maxPrice", "900");
        } else if (text.contains("dưới 30 triệu") || text.contains("dưới 30tr")) {
            attributes.put("maxPrice", "1300");
        }
    }

    private List<Product> filterProducts(List<Product> all, Map<String, String> attrs, String rawMsg) {
        String category = attrs.get("category");
        String brand = attrs.get("brand");
        String minPriceStr = attrs.get("minPrice");
        String maxPriceStr = attrs.get("maxPrice");
        String searchQuery = attrs.get("searchQuery");

        return all.stream()
                .filter(p -> {
                    // Match category if set
                    if (category != null && !category.isEmpty()) {
                        String pCat = p.getCategory() != null ? p.getCategory().getName() : "";
                        if (!pCat.equalsIgnoreCase(category)) return false;
                    }
                    // Match brand if set
                    if (brand != null && !brand.isEmpty()) {
                        String pName = p.getName() != null ? p.getName().toLowerCase() : "";
                        String pDesc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                        String pBrand = p.getBrand() != null && p.getBrand().getName() != null ? p.getBrand().getName().toLowerCase() : "";
                        if (!pName.contains(brand.toLowerCase()) && 
                            !pDesc.contains(brand.toLowerCase()) &&
                            !pBrand.contains(brand.toLowerCase())) {
                            return false;
                        }
                    }
                    // Match minPrice if set
                    if (minPriceStr != null && !minPriceStr.isEmpty()) {
                        double minPrice = Double.parseDouble(minPriceStr);
                        if (p.getPrice() == null || p.getPrice() < minPrice) return false;
                    }
                    // Match maxPrice if set
                    if (maxPriceStr != null && !maxPriceStr.isEmpty()) {
                        double maxPrice = Double.parseDouble(maxPriceStr);
                        if (p.getPrice() == null || p.getPrice() > maxPrice) return false;
                    }
                    // Match searchQuery if set
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        String pName = p.getName() != null ? p.getName().toLowerCase() : "";
                        String pDesc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                        if (!pName.contains(searchQuery.toLowerCase()) && !pDesc.contains(searchQuery.toLowerCase())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private String getMatchedFaqContext(String text, List<ChatFaq> faqs) {
        StringBuilder sb = new StringBuilder();
        for (ChatFaq faq : faqs) {
            if (faq.getKeyword() == null || faq.getKeyword().isEmpty()) continue;
            String[] keywords = faq.getKeyword().split(",");
            for (String kw : keywords) {
                if (text.contains(kw.trim().toLowerCase())) {
                    sb.append("TÀI LIỆU FAQ CHUẨN (").append(faq.getQuestion()).append("):\n")
                      .append(faq.getAnswer()).append("\n\n");
                    break; // match once for this FAQ
                }
            }
        }
        return sb.toString();
    }

    private String getFullCatalogContext(List<Product> allProducts) {
        StringBuilder sb = new StringBuilder();
        for (Product p : allProducts) {
            String brandName = p.getBrand() != null ? p.getBrand().getName() : "Chính hãng";
            String catName = p.getCategory() != null ? p.getCategory().getName() : "Chưa phân loại";
            sb.append("- **").append(p.getName()).append("** (Hãng: ").append(brandName)
              .append(", Danh mục: ").append(catName)
              .append(", Giá: $").append(p.getPrice())
              .append(" (~").append(Math.round(p.getPrice() * 25 / 1000.0)).append(" triệu VNĐ)")
              .append(", Kho: ").append(p.getStock()).append(" máy")
              .append(", Mô tả: ").append(p.getDescription());
            if (p.getSpecs() != null && !p.getSpecs().isEmpty()) {
                sb.append(", Thông số: ");
                p.getSpecs().forEach(s -> sb.append(s.getSpecKey()).append(": ").append(s.getSpecValue()).append("; "));
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    private String buildSystemInstruction(List<Product> matched, List<Product> allProducts, List<Coupon> vouchers, String userCtx, String orderCtx, String faqCtx, Map<String, String> attrs) {
        StringBuilder instruction = new StringBuilder();
        instruction.append("Bạn là VoltBot - nhân viên tư vấn mua sắm công nghệ thông minh, thân thiện của website bán lẻ công nghệ VoltTech.\n");
        instruction.append("QUY TẮC PHẢN HỒI:\n")
                  .append("- Hãy trả lời tự nhiên, lịch sự bằng Tiếng Việt và chèn nhẹ nhàng một số biểu cảm icon (emoji) phù hợp 👌.\n")
                  .append("- Giữ câu trả lời ngắn gọn, rõ ràng, chia các ý bằng dấu gạch đầu dòng và định dạng các từ khóa quan trọng bằng chữ đậm (**từ khóa**).\n")
                  .append("- Không trả lời máy móc. Tư vấn thực tế theo nhu cầu khách hàng.\n\n");

        instruction.append("HƯỚNG DẪN XỬ LÝ CÁC NHÓM CÂU HỎI:\n")
                  .append("1. **Tư vấn sản phẩm & tìm kiếm theo nhu cầu**:\n")
                  .append("   - Sinh viên CNTT/Lập trình/Học AI: Đề xuất các dòng laptop cấu hình mạnh như ASUS ROG Zephyrus G14 (RTX 4070), Dell XPS 15 9530 (RTX 4060, i9), hoặc MacBook Pro 16\" M3 Max.\n")
                  .append("   - Mua laptop học tập văn phòng tầm trung (15 - 20 triệu VNĐ, tương đương $600 - $800): Recommend ASUS Vivobook 15 ($649.99) hoặc HP Pavilion 15 ($749.99).\n")
                  .append("   - Thiết kế đồ họa / AutoCAD: Đề xuất Dell XPS 15 hoặc ASUS ROG Zephyrus G14 vì có card đồ họa rời NVIDIA RTX và màn hình chuẩn màu sắc nét.\n")
                  .append("   - Chơi game: ASUS ROG Zephyrus G14 hoặc các thiết bị gaming chuyên dụng như Steam Deck, PlayStation 5, Nintendo Switch.\n")
                  .append("   - Điện thoại chụp ảnh đẹp: iPhone 15 Pro Max (5x zoom), Samsung S24 Ultra (200MP camera), Pixel 8 Pro (Tensor G3, Magic Eraser).\n")
                  .append("   - Điện thoại pin trâu / sạc nhanh: OnePlus 12 (5400mAh, 100W SuperVOOC) hoặc Samsung S24 Ultra (5000mAh, 45W).\n")
                  .append("   - Tai nghe: Sony WH-1000XM5 (chụp tai, chống ồn tốt nhất), AirPods Pro 2 (nhét tai, nhỏ gọn, tương thích Apple).\n")
                  .append("   - Phụ kiện: Chuột Logitech MX Master 3S (văn phòng yên tĩnh), bàn phím cơ Keychron Q1 Pro (gõ êm, double-gasket).\n")
                  .append("2. **So sánh sản phẩm**:\n")
                  .append("   - Hãy so sánh chi tiết các cấu hình, ưu nhược điểm dựa trên thông số thực tế.\n")
                  .append("   - Nếu khách hỏi so sánh sản phẩm bên ngoài hệ thống (như MacBook Air M3, Dell XPS 13, iPhone 16), hãy dùng kiến thức chung của bạn để so sánh và giới thiệu các dòng máy tương đương đang có sẵn trong catalog (như MacBook Pro M3 Max, Dell XPS 15, iPhone 15 Pro Max).\n")
                  .append("3. **Hỏi thông số chi tiết (Specs)**:\n")
                  .append("   - Khả năng nâng cấp RAM: Dell XPS 15 hỗ trợ nâng cấp đến 64GB; MacBook và ROG Zephyrus G14 không thể nâng cấp RAM sau khi mua (RAM hàn chết).\n")
                  .append("   - Chống nước: iPhone 15 Pro Max, Samsung S24 Ultra, Pixel 8 Pro đạt chuẩn IP68; OnePlus 12 đạt chuẩn IP65.\n")
                  .append("   - Bảo hành: Tất cả sản phẩm được bảo hành chính hãng 12-24 tháng, đổi mới 1-đổi-1 trong 30 ngày đầu nếu có lỗi phần cứng.\n")
                  .append("4. **Giá và khuyến mãi**:\n")
                  .append("   - Báo giá theo USD lưu trong catalog. Trả góp 0% lãi suất hỗ trợ qua thẻ tín dụng liên kết hơn 25 ngân hàng hoặc công ty tài chính, duyệt trong 15 phút.\n")
                  .append("5. **Tồn kho & Chi nhánh**:\n")
                  .append("   - Hệ thống chi nhánh VoltTech:\n")
                  .append("     + Hà Nội: 136 Xuân Thủy, Cầu Giấy, Hà Nội.\n")
                  .append("     + TP. Hồ Chí Minh: 227 Nguyễn Văn Cừ, Quận 5, TP. HCM.\n")
                  .append("   - Màu sắc sẵn có của các sản phẩm hot: iPhone 15 Pro Max (Titan Tự nhiên, Titan Đen), Samsung S24 Ultra (Xám Titan, Đen Titan), Sony WH-1000XM5 (Đen, Bạc).\n")
                  .append("6. **Đơn hàng & tài khoản**:\n")
                  .append("   - Đọc thông tin lịch sử đơn hàng của khách hàng đăng nhập ở phần dưới.\n")
                  .append("   - Chỉ cho phép khách hàng tự hủy đơn trực tiếp trên web nếu đơn hàng ở trạng thái **PENDING**. Nếu đã là **CONFIRMED** hoặc **SHIPPED**, hướng dẫn khách liên hệ hotline CSKH **1900 8198**.\n")
                  .append("   - Đổi địa chỉ giao hàng: Hướng dẫn cập nhật trong trang cá nhân cho các đơn sau, hoặc liên hệ ngay hotline hỗ trợ nếu đơn hàng vừa đặt đang ở trạng thái PENDING.\n\n");

        if (userCtx != null && !userCtx.isEmpty()) {
            instruction.append("THÔNG TIN KHÁCH HÀNG ĐĂNG NHẬP:\n").append(userCtx).append("\n").append(orderCtx).append("\n\n");
        }

        if (faqCtx != null && !faqCtx.isEmpty()) {
            instruction.append("ĐƯỜNG LỐI CHÍNH SÁCH BÁN HÀNG CỦA CỬA HÀNG:\n").append(faqCtx).append("\n\n");
        }

        instruction.append("CÁC MÃ GIẢM GIÁ (VOUCHER) ĐANG CÓ SẴN CỦA HỆ THỐNG:\n");
        if (!vouchers.isEmpty()) {
            vouchers.forEach(v -> instruction.append("- Mã: **").append(v.getCode()).append("** (Loại giảm: ").append(v.getDiscountType())
                    .append(", Giá trị giảm: ").append(v.getDiscountValue()).append(", Đơn tối thiểu: $").append(v.getMinOrderValue() != null ? v.getMinOrderValue() : "0")
                    .append(")\n"));
        } else {
            instruction.append("- Mã: **VOLT10** (Giảm ngay 10% cho đơn hàng đầu tiên của bạn tại cửa hàng!)\n");
        }
        instruction.append("\n");

        // Add Active Context attributes
        instruction.append("BỘ LỌC TÌM KIẾM ĐANG HOẠT ĐỘNG:\n");
        attrs.forEach((k, v) -> instruction.append("- ").append(k).append(": ").append(v).append("\n"));
        instruction.append("\n");

        // Include the entire product catalog in system instruction
        instruction.append("BẢN ĐỒ TOÀN BỘ DANH MỤC SẢN PHẨM CỬA HÀNG ĐANG KINH DOANH (Hãy dùng danh mục này để tư vấn, so sánh hoặc gợi ý sản phẩm thay thế phù hợp khi sản phẩm người dùng tìm kiếm hết hàng hoặc nằm ngoài khoảng giá):\n");
        instruction.append(getFullCatalogContext(allProducts)).append("\n");

        // Add inventory search context
        instruction.append("DANH SÁCH SẢN PHẨM KHỚP BỘ LỌC TÌM KIẾM HIỆN TẠI (Đề xuất trực tiếp các sản phẩm này trước tiên):\n");
        if (!matched.isEmpty()) {
            matched.stream().limit(5).forEach(p -> {
                String pName = p.getName() != null ? p.getName() : "Sản phẩm";
                String pBrand = p.getBrand() != null && p.getBrand().getName() != null ? p.getBrand().getName() : "Chính hãng";
                
                String specsStr = "";
                if (p.getSpecs() != null && !p.getSpecs().isEmpty()) {
                    specsStr = " · Thông số kỹ thuật: [" + p.getSpecs().stream()
                        .map(s -> s.getSpecKey() + ": " + s.getSpecValue())
                        .collect(Collectors.joining(", ")) + "]";
                }

                instruction.append("- [")
                    .append(pName).append("] - Giá: $").append(p.getPrice() != null ? p.getPrice() : "0.0")
                    .append(" · Hãng: ").append(pBrand)
                    .append(" · Kho còn: ").append(p.getStock() != null ? p.getStock() : "0")
                    .append(" máy · Mô tả: ").append(p.getDescription() != null ? p.getDescription() : "Chưa có mô tả")
                    .append(specsStr)
                    .append("\n");
            });
            instruction.append("\nHướng dẫn khách hàng rằng họ có thể xem thẻ sản phẩm ở phía dưới khung chat để click xem chi tiết hoặc bấm thêm vào giỏ hàng trực tiếp!");
        } else {
            instruction.append("- Không tìm thấy sản phẩm nào khớp chính xác bộ lọc hiện tại của bạn.\n");
            instruction.append("Hãy lịch sự thông báo cho khách hàng biết, sau đó tự động sử dụng Bản đồ toàn bộ danh mục sản phẩm ở trên để đề xuất các sản phẩm tương tự hoặc cấu hình gần nhất có sẵn tại VoltTech!");
        }

        return instruction.toString();
    }

    private List<String> generateQuickReplies(Map<String, String> attributes) {
        List<String> replies = new ArrayList<>();
        String category = attributes.get("category");

        if (category == null) {
            replies.add("💻 Tư vấn Laptop");
            replies.add("📱 Tư vấn Điện thoại");
            replies.add("🎁 Xem mã giảm giá");
            replies.add("🚚 Phí giao hàng");
            replies.add("📦 Theo dõi đơn hàng");
        } else if ("Laptops".equals(category)) {
            replies.add("💻 Laptop Dell");
            replies.add("💻 Macbook Apple");
            replies.add("💻 Laptop gaming rẻ");
            replies.add("🎁 Xem mã giảm giá");
            replies.add("🔙 Menu chính");
        } else if ("Smartphones".equals(category)) {
            replies.add("📱 Điện thoại iPhone");
            replies.add("📱 Điện thoại Samsung");
            replies.add("📱 Điện thoại dưới 1000$");
            replies.add("🎁 Xem mã giảm giá");
            replies.add("🔙 Menu chính");
        } else {
            replies.add("💻 Tư vấn Laptop");
            replies.add("📱 Tư vấn Điện thoại");
            replies.add("🎁 Xem mã giảm giá");
            replies.add("🔙 Menu chính");
        }
        return replies;
    }

    private String generateDynamicLocalResponse(String message, List<Product> matchedProducts, List<Product> allProducts, List<Coupon> activeCoupons, String faqContext, Map<String, String> attributes, String orderContext) {
        String lower = message.toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 **VoltBot Local Intelligence:**\n\n");

        // 1. Order Status & Management Queries
        if (lower.contains("đơn hàng") || lower.contains("hủy đơn") || lower.contains("đổi địa chỉ") || lower.contains("địa chỉ nhận") || lower.contains("bao giờ nhận")) {
            if (lower.contains("hủy")) {
                return sb.append("📦 **Chính sách hủy đơn hàng của VoltTech:**\n\n")
                  .append("- Bạn có thể tự hủy đơn hàng trực tiếp trên website nếu trạng thái đơn hàng là **PENDING** (Chờ xử lý).\n")
                  .append("- Nếu đơn hàng đã chuyển sang **CONFIRMED** hoặc **SHIPPED**, bạn không thể tự hủy trên web. Xin vui lòng liên hệ hotline **1900 8198** sớm nhất có thể để được hỗ trợ.\n👌").toString();
            }
            if (lower.contains("đổi") || lower.contains("địa chỉ")) {
                return sb.append("🚚 **Thay đổi địa chỉ giao hàng:**\n\n")
                  .append("- Bạn có thể cập nhật danh sách địa chỉ nhận hàng của mình trong trang **Hồ sơ cá nhân ➡️ Địa chỉ**.\n")
                  .append("- Đối với đơn hàng đã đặt và đang ở trạng thái **PENDING**, vui lòng liên hệ ngay hotline **1900 8198** để nhân viên cập nhật thông tin vận đơn trước khi bàn giao cho đơn vị vận chuyển.\n👌").toString();
            }
            if (orderContext != null && !orderContext.trim().isEmpty()) {
                return sb.append("📋 **Thông tin đơn hàng của bạn:**\n\n")
                  .append(orderContext)
                  .append("\n- **Giao hàng**: Đơn hàng sau khi được xác nhận (CONFIRMED) sẽ giao đến tay bạn trong 1-2 ngày làm việc. Bạn có thể bấm vào mã đơn hàng để theo dõi chi tiết nhé! 👌").toString();
            } else {
                return sb.append("🔍 **Thông tin đơn hàng:**\n\n")
                  .append("Hiện tại bạn chưa đăng nhập hoặc chưa có đơn hàng nào trong tài khoản này. Vui lòng đăng nhập bằng tài khoản mua hàng để mình tra cứu trạng thái đơn hàng chính xác nhất cho bạn nhé! 👌").toString();
            }
        }

        // 2. Budget Queries
        if (lower.contains("triệu") || lower.contains("tr") || lower.contains("ngân sách") || lower.contains("tầm giá")) {
            boolean is15to20 = (lower.contains("15") && lower.contains("20")) || lower.contains("15-20") || lower.contains("15 đến 20") || lower.contains("15tr đến 20tr");
            boolean isUnder10 = lower.contains("dưới 10") || lower.contains("dưới 10tr") || lower.contains("10 triệu");
            boolean isUnder15 = lower.contains("dưới 15") || lower.contains("dưới 15tr") || lower.contains("15 triệu");
            boolean isUnder20 = lower.contains("dưới 20") || lower.contains("dưới 20tr") || lower.contains("20 triệu");

            if (is15to20 || isUnder20) {
                sb.append("💻 **Tư vấn Laptop / Thiết bị trong tầm giá 15 - 20 triệu VNĐ (~$600 - $800):**\n\n")
                  .append("Tại VoltTech, chúng tôi đề xuất các sản phẩm tối ưu cho học tập và làm việc sau:\n")
                  .append("- **ASUS Vivobook 15** ($649.99 ~ 16.2 triệu VNĐ): Core i5-1335U, 16GB RAM, 512GB SSD. Cực kỳ mỏng nhẹ, pin tốt, gõ phím êm.\n")
                  .append("- **HP Pavilion 15** ($749.99 ~ 18.7 triệu VNĐ): Ryzen 7 7730U, 16GB RAM, 512GB SSD. Hiệu năng đa nhân mạnh mẽ, màn hình IPS sắc nét.\n\n")
                  .append("👉 Cả hai máy đều đang sẵn hàng tại kho. Bạn có thể sử dụng mã giảm giá **VOLT10** để được giảm ngay 10% khi đặt mua! 👌");
                return sb.toString();
            }
            if (isUnder10) {
                sb.append("📱 **Tư vấn thiết bị dưới 10 triệu VNĐ (~$400):**\n\n")
                  .append("Với ngân sách dưới 10 triệu, bạn có thể tham khảo các dòng sản phẩm chất lượng cao sau:\n")
                  .append("- **Sony WH-1000XM5** ($399.99 ~ 10 triệu VNĐ): Tai nghe chống ồn đỉnh cao, thời lượng pin 30h, âm thanh tuyệt vời.\n")
                  .append("- **Apple AirPods Pro (2nd Gen)** ($249.99 ~ 6.2 triệu VNĐ): Nhỏ gọn, chống ồn tốt, tương thích hoàn hảo với iPhone.\n")
                  .append("- **Nintendo Switch OLED** ($349.99 ~ 8.7 triệu VNĐ): Máy chơi game cầm tay đa năng, màn hình OLED rực rỡ.\n\n")
                  .append("Nếu bạn cần tìm Laptop dưới 10 triệu, hiện tại VoltTech chưa có sẵn dòng laptop phân khúc giá rẻ này (các dòng laptop của chúng tôi bắt đầu từ $649). Bạn có thể tham khảo trả góp 0% lãi suất nhé! 👌");
                return sb.toString();
            }
        }

        // 3. Specific Use Case Consultations
        if (lower.contains("sinh viên cntt") || lower.contains("học tập") || lower.contains("lập trình") || lower.contains("học ai") || lower.contains("coder") || lower.contains("developer")) {
            sb.append("💻 **Tư vấn Laptop cho Sinh viên CNTT, Lập trình & học AI:**\n\n")
              .append("Để học lập trình và AI tốt, bạn cần laptop có cấu hình RAM tối thiểu 16GB, CPU đời mới mạnh mẽ và tốt nhất là có card đồ họa rời (NVIDIA) để chạy các thuật toán Deep Learning:\n\n")
              .append("1. **ASUS ROG Zephyrus G14** ($1599.99): AMD Ryzen 9, 16GB RAM, card rời **NVIDIA RTX 4070**. Đây là mẫu máy lý tưởng nhất cho học AI và lập trình di động nhờ card đồ họa cực mạnh và thiết kế gọn nhẹ.\n")
              .append("2. **Dell XPS 15 9530** ($1899.99): Intel Core i9, 32GB RAM, card rời **NVIDIA RTX 4060**. Cấu hình khủng, màn hình OLED đẹp, RAM nâng cấp được lên 64GB.\n")
              .append("3. **ASUS Vivobook 15** ($649.99) hoặc **HP Pavilion 15** ($749.99): Lựa chọn giá rẻ (15-20 triệu) rất tốt cho học lập trình web, mobile thông thường (không yêu cầu học sâu AI cấu hình cao).\n\n")
              .append("Nếu bạn chuyên tâm học AI nặng, hãy ưu tiên chọn mẫu có **card đồ họa rời RTX** của Dell hoặc ASUS ROG nhé! 👌");
            return sb.toString();
        }

        if (lower.contains("thiết kế đồ họa") || lower.contains("đồ họa") || lower.contains("design") || lower.contains("autocad") || lower.contains("photoshop") || lower.contains("premiere")) {
            sb.append("🎨 **Tư vấn Laptop thiết kế đồ họa & AutoCAD:**\n\n")
              .append("Tác vụ đồ họa cần màn hình chuẩn màu (sRGB, DCI-P3) và CPU/GPU hiệu năng cao:\n")
              .append("- **Dell XPS 15 9530** ($1899.99): Có màn hình **3.5K OLED Touch** siêu sắc nét, chuẩn màu tuyệt đối, CPU Intel Core i9 và GPU **NVIDIA RTX 4060** gánh mượt AutoCAD, Revit, Adobe Premiere.\n")
              .append("- **MacBook Pro 16\" M3 Max** ($3499.99): CPU 16 nhân, GPU 40 nhân, 48GB Unified Memory. Màn hình Liquid Retina XDR cực đỉnh, lý tưởng cho dựng phim chuyên nghiệp và thiết kế đồ họa 2D/3D.\n")
              .append("- **LG UltraFine 27\" 5K** ($1299.99): Màn hình rời lý tưởng bổ trợ cho thiết kế đồ họa với độ sáng 500 nits, dải màu P3 rộng.\n👌");
            return sb.toString();
        }

        if (lower.contains("chơi game") || lower.contains("gaming") || lower.contains("liên minh") || lower.contains("lol") || lower.contains("pubg") || lower.contains("fifa")) {
            sb.append("🎮 **Tư vấn thiết bị chơi game tại VoltTech:**\n\n")
              .append("- **ASUS ROG Zephyrus G14** ($1599.99): Laptop Gaming đỉnh cao với Ryzen 9, card rời **RTX 4070**, màn hình 165Hz chơi mượt mọi game AAA và Liên Minh Huyền Thoại.\n")
              .append("- **PlayStation 5 Slim** ($499.99): Máy console tối ưu cho trải nghiệm game trên TV 4K.\n")
              .append("- **Steam Deck OLED 512GB** ($549.99): Máy chơi game PC cầm tay tuyệt vời, màn hình OLED 90Hz mượt mà.\n")
              .append("- **Nintendo Switch OLED** ($349.99): Tiện lợi cho game gia đình, co-op di động.\n👌");
            return sb.toString();
        }

        // 4. Product Comparisons
        if (lower.contains("so sánh") || lower.contains("khác nhau thế nào") || lower.contains("khác biệt") || lower.contains("nên chọn")) {
            if (lower.contains("macbook") || lower.contains("xps") || lower.contains("dell")) {
                sb.append("⚖️ **So sánh MacBook Pro 16 M3 Max và Dell XPS 15 9530:**\n\n")
                  .append("- **MacBook Pro 16 M3 Max** ($3499.99): Phù hợp với người dùng hệ sinh thái Apple, thời lượng pin khủng (lên tới 22 tiếng), hiệu năng đồ họa cực mạnh, chạy mát mẻ không ồn.\n")
                  .append("- **Dell XPS 15 9530** ($1899.99): Phù hợp với lập trình viên Windows/Linux, màn hình cảm ứng OLED sắc nét, khả năng nâng cấp RAM linh hoạt lên tới 64GB, chơi game tốt hơn.\n\n")
                  .append("*Lưu ý: Nếu bạn nghe nói về MacBook Air M3 hay Dell XPS 13, chúng là dòng mỏng nhẹ cơ động (pin tốt, cấu hình vừa phải). Còn dòng Pro 16 và XPS 15 tại VoltTech là dòng hiệu năng cao chuyên nghiệp.* 👌");
                return sb.toString();
            }
            if (lower.contains("iphone") || lower.contains("samsung") || lower.contains("galaxy") || lower.contains("s24")) {
                sb.append("⚖️ **So sánh iPhone 15 Pro Max và Samsung Galaxy S24 Ultra:**\n\n")
                  .append("- **iPhone 15 Pro Max** ($1199.99): Khung Titan sang trọng, chip A17 Pro mạnh mẽ, camera zoom quang học 5x cực kỳ ổn định cho quay phim. Hệ điều hành iOS mượt mà, giữ giá tốt.\n")
                  .append("- **Samsung Galaxy S24 Ultra** ($1299.99): Màn hình Dynamic AMOLED 2X sáng hơn, bút S Pen tích hợp tiện lợi, camera 200MP zoom ấn tượng, hỗ trợ nhiều tính năng Galaxy AI tiên tiến.\n👌");
                return sb.toString();
            }
            if (lower.contains("airpods") || lower.contains("sony") || lower.contains("1000xm5")) {
                sb.append("⚖️ **So sánh Apple AirPods Pro và Sony WH-1000XM5:**\n\n")
                  .append("- **Sony WH-1000XM5** ($399.99): Dạng tai nghe chụp tai (Over-ear), chống ồn chủ động tốt nhất thế giới, âm bass sâu, pin 30-38 tiếng liên tục, đeo êm tai.\n")
                  .append("- **Apple AirPods Pro (2nd Gen)** ($249.99): Tai nghe nhét tai (In-ear), cực kỳ nhỏ gọn, chống ồn tốt, âm thanh không gian cá nhân hóa, mang đi lại thuận tiện.\n👌");
                return sb.toString();
            }
            if (lower.contains("16gb") && lower.contains("32gb")) {
                return sb.append("🧠 **Sự khác biệt giữa RAM 16GB và 32GB:**\n\n")
                  .append("- **RAM 16GB**: Đủ cho các tác vụ văn phòng, chơi game thông thường, lập trình web/di động cơ bản và thiết kế đồ họa 2D.\n")
                  .append("- **RAM 32GB**: Cần thiết khi bạn chạy máy ảo (Docker, VM), xử lý dữ liệu lớn (Big Data), lập trình/huấn luyện AI local, hoặc dựng phim 4K chuyên nghiệp.\n👌").toString();
            }
            if (lower.contains("nvme") && lower.contains("sata")) {
                return sb.append("💾 **So sánh SSD NVMe và SSD SATA:**\n\n")
                  .append("- **SSD NVMe (PCIe)**: Tốc độ đọc ghi siêu nhanh (từ 3500MB/s đến 7000MB/s), gắn trực tiếp lên mainboard qua khe M.2. Thích hợp cho hệ điều hành, game nặng và xử lý file lớn.\n")
                  .append("- **SSD SATA**: Tốc độ chậm hơn (giới hạn ở ~550MB/s), dùng cáp SATA hoặc dạng M.2 SATA. Giá rẻ hơn, thích hợp làm ổ lưu trữ phụ.\n👌").toString();
            }
        }

        // 5. Product Specs / Details Queries
        if (lower.contains("nâng cấp ram") || lower.contains("card đồ họa rời") || lower.contains("card rời") || lower.contains("chống nước") || lower.contains("bảo hành")) {
            if (lower.contains("ram") || lower.contains("nâng cấp")) {
                return sb.append("⚙️ **Khả năng nâng cấp RAM của máy tính:**\n\n")
                  .append("- **Dell XPS 15 9530**: Hỗ trợ nâng cấp RAM tối đa lên **64GB DDR5** (có 2 khe cắm SO-DIMM tháo rời).\n")
                  .append("- **MacBook Pro 16\" M3 Max** và **ASUS ROG Zephyrus G14**: RAM được hàn chết trên bo mạch (Unified Memory / On-board), không thể nâng cấp sau khi mua. Bạn nên chọn đúng phiên bản dung lượng khi đặt mua nhé!\n👌").toString();
            }
            if (lower.contains("card") || lower.contains("đồ họa") || lower.contains("gpu")) {
                return sb.append("🎮 **Thông tin Card đồ họa rời (GPU):**\n\n")
                  .append("- **ASUS ROG Zephyrus G14**: Tích hợp card rời cực mạnh **NVIDIA GeForce RTX 4070 (8GB)**.\n")
                  .append("- **Dell XPS 15 9530**: Tích hợp card rời **NVIDIA GeForce RTX 4060 (8GB)**.\n")
                  .append("- **MacBook Pro 16\" M3 Max**: Sử dụng **GPU tích hợp 40 nhân** hiệu năng siêu khủng tương đương card rời cao cấp.\n")
                  .append("- Các sản phẩm văn phòng như ASUS Vivobook 15 và HP Pavilion 15 sử dụng card đồ họa tích hợp Intel Iris Xe / AMD Radeon.\n👌").toString();
            }
            if (lower.contains("chống nước")) {
                return sb.append("💦 **Khả năng chống nước của điện thoại:**\n\n")
                  .append("- **iPhone 15 Pro Max**, **Samsung Galaxy S24 Ultra** và **Google Pixel 8 Pro** đều đạt chuẩn chống nước, bụi **IP68** (có thể chịu nước ở độ sâu 1.5 mét trong tối đa 30 phút).\n")
                  .append("- **OnePlus 12** đạt chuẩn **IP65** (chỉ chống tia nước phun và bụi, không nên ngâm nước).\n👌").toString();
            }
            if (lower.contains("bảo hành")) {
                return sb.append("🛡️ **Chính sách bảo hành sản phẩm tại VoltTech:**\n\n")
                  .append("- Tất cả các sản phẩm chính hãng mua tại VoltTech đều được bảo hành từ **12 đến 24 tháng** theo đúng chính sách của nhà sản xuất.\n")
                  .append("- Đổi mới 1-đổi-1 trong vòng **30 ngày đầu tiên** nếu phát sinh lỗi kỹ thuật từ nhà sản xuất.\n👌").toString();
            }
        }

        // 6. Inventory, Colors, Branches & Locations
        if (lower.contains("chi nhánh") || lower.contains("địa chỉ cửa hàng") || lower.contains("hà nội") || lower.contains("hcm") || lower.contains("sài gòn") || lower.contains("còn hàng") || lower.contains("màu")) {
            if (lower.contains("chi nhánh") || lower.contains("địa chỉ") || lower.contains("hà nội") || lower.contains("hcm") || lower.contains("sài gòn")) {
                return sb.append("🏢 **Hệ thống chi nhánh cửa hàng VoltTech:**\n\n")
                  .append("Chúng tôi có 2 chi nhánh lớn luôn sẵn sàng phục vụ quý khách:\n")
                  .append("1. **Chi nhánh Hà Nội**: 136 Xuân Thủy, Cầu Giấy, Hà Nội.\n")
                  .append("2. **Chi nhánh TP. Hồ Chí Minh**: 227 Nguyễn Văn Cừ, Quận 5, TP. HCM.\n\n")
                  .append("- **Thời gian mở cửa**: 8:00 - 21:30 hàng ngày (kể cả chủ nhật).\n")
                  .append("- Cả hai chi nhánh đều có sẵn hàng trải nghiệm cho các dòng máy hot! 👌").toString();
            }
            if (lower.contains("màu")) {
                return sb.append("🎨 **Các phiên bản màu sắc sẵn có:**\n\n")
                  .append("- **iPhone 15 Pro Max**: Titan Tự nhiên (Natural Titanium), Titan Đen (Black).\n")
                  .append("- **Samsung Galaxy S24 Ultra**: Xám Titan (Titanium Gray), Đen Titan.\n")
                  .append("- **Sony WH-1000XM5**: Đen (Black), Bạc (Silver).\n")
                  .append("- **Bose QuietComfort Ultra**: Trắng sang trọng (Luxe White).\n👌").toString();
            }
            if (lower.contains("khi nào nhập") || lower.contains("nhập hàng")) {
                return sb.append("🔄 **Kế hoạch nhập hàng:**\n\n")
                  .append("Các sản phẩm tạm cháy hàng sẽ được VoltTech bổ sung liên tục sau **1 - 2 tuần**. Bạn có thể để lại số điện thoại hoặc email nhận tin để hệ thống gửi thông báo tự động ngay khi hàng về nhé! 👌").toString();
            }
        }

        // 7. General FAQ match or fallback
        if (faqContext != null && !faqContext.isEmpty()) {
            String cleanedFaq = faqContext.replace("TÀI LIỆU FAQ CHUẨN", "📌")
                                          .replace("):", ")")
                                          .trim();
            sb.append("Dưới đây là thông tin chính sách chính thức của cửa hàng VoltTech mà bạn quan tâm:\n\n")
              .append(cleanedFaq).append("\n\n")
              .append("Bạn cần mình hỗ trợ thêm thông tin nào khác không ạ? 👌");
            return sb.toString();
        }

        // Fallback Product search catalog list
        String category = attributes.get("category");
        String brand = attributes.get("brand");
        if (category != null) {
            sb.append("Tôi đã lọc danh mục **").append(category).append("**");
            if (brand != null) {
                sb.append(" thương hiệu **").append(brand).append("**");
            }
            sb.append(" theo yêu cầu của bạn.\n\n");
        } else if (brand != null) {
            sb.append("Tôi đã lọc các sản phẩm thương hiệu **").append(brand).append("** từ kho hàng.\n\n");
        } else {
            sb.append("Chào bạn! Dưới đây là danh sách các mẫu sản phẩm công nghệ bán chạy tại VoltTech:\n\n");
        }

        if (!matchedProducts.isEmpty()) {
            sb.append("Các sản phẩm phù hợp:\n");
            matchedProducts.stream().limit(5).forEach(p -> {
                String pName = p.getName() != null ? p.getName() : "Sản phẩm";
                String pBrand = p.getBrand() != null && p.getBrand().getName() != null ? p.getBrand().getName() : "Chính hãng";
                sb.append("- **").append(pName).append("** · Hãng: **").append(pBrand)
                  .append("** · Giá: **$").append(p.getPrice()).append("**")
                  .append(" (Còn lại: ").append(p.getStock()).append(" máy trong kho)\n");
            });
            sb.append("\n👉 Bạn có thể click trực tiếp vào các thẻ sản phẩm bên dưới khung chat để xem thông tin chi tiết hoặc đặt mua nhanh nhé! 👌");
        } else {
            sb.append("Chúng tôi có đầy đủ Laptop, Điện thoại, Tai nghe chính hãng Apple, Samsung, Dell, Asus, Sony... Bạn có thể cho mình biết nhu cầu chi tiết hoặc ngân sách để mình tư vấn chính xác mẫu máy phù hợp nhé! 👌");
        }

        return sb.toString();
    }

    private String buildExtractionPrompt(String message, List<String> history, Map<String, String> currentAttributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là AI bóc tách thông tin bộ lọc tìm kiếm sản phẩm công nghệ từ tin nhắn người dùng tại cửa hàng VoltTech.\n");
        sb.append("Hãy phân tích tin nhắn hiện tại và lịch sử trò chuyện (nếu có), sau đó trả về chuỗi JSON chính xác theo định dạng sau (KHÔNG chèn thêm bất kỳ chữ nào khác ngoài JSON, không bọc trong markdown code blocks, chỉ trả về JSON nguyên bản):\n");
        sb.append("{\n");
        sb.append("  \"searchQuery\": \"tên sản phẩm cụ thể, model cụ thể hoặc null (ví dụ: 'iphone 15 pro', 'ipad air m2')\",\n");
        sb.append("  \"category\": \"Laptops\" | \"Desktop PCs\" | \"Components\" | \"Gaming\" | \"Monitors\" | \"Peripherals\" | null (Trong đó: Laptops là máy tính xách tay, Macbook; Desktop PCs là máy tính để bàn, PC, Workstation; Components là linh kiện phần cứng như CPU, GPU, RAM, SSD; Gaming là thiết bị chơi game, máy console, tay cầm, tai nghe chơi game/tai nghe; Monitors là màn hình máy tính; Peripherals là thiết bị ngoại vi như chuột, bàn phím, loa, micro, stream deck...),\n");
        sb.append("  \"brand\": \"Tên thương hiệu chuẩn hóa viết hoa chữ cái đầu (ví dụ: Apple, Dell, Asus, Samsung, Sony, Bose, Garmin...) hoặc null\",\n");
        sb.append("  \"minPrice\": số_tiền_usd_hoặc_null,\n");
        sb.append("  \"maxPrice\": số_tiền_usd_hoặc_null,\n");
        sb.append("  \"contextSwitch\": true nếu tin nhắn của người dùng chuyển đổi chủ đề sang sản phẩm khác hoàn toàn hoặc hãng khác hoàn toàn (ví dụ: đang hỏi mua Laptop Asus lại hỏi sang Điện thoại Samsung, hoặc từ tìm kiếm giá rẻ chuyển sang tìm kiếm giá cao hẳn), ngược lại là false\n");
        sb.append("}\n\n");
        sb.append("QUY TẮC QUY ĐỔI TIỀN TỆ:\n");
        sb.append("- Cơ sở dữ liệu của chúng ta lưu giá bằng tiền USD ($). Do đó, nếu người dùng sử dụng đơn vị tiền Việt (đồng, triệu, củ, tr, k), bạn hãy quy đổi sang USD bằng cách chia cho 25.000 (Ví dụ: '10 triệu' hoặc '10tr' -> 400, '20 triệu' hoặc '20 củ' -> 800, '15tr' -> 600, '500k' -> 20, '25 triệu' -> 1000).\n");
        sb.append("- Nếu người dùng nói 'dưới 20 triệu', hãy đặt maxPrice = 800 và minPrice = null.\n");
        sb.append("- Nếu người dùng nói 'tầm 15 đến 20 triệu', đặt minPrice = 600 và maxPrice = 800.\n");
        sb.append("- Nếu người dùng nói 'trên 15 triệu', đặt minPrice = 600 và maxPrice = null.\n\n");
        
        // Provide current session context so AI can detect category switch
        if (currentAttributes != null && !currentAttributes.isEmpty()) {
            sb.append("Ngữ cảnh session hiện tại (BỘ LỌC ĐANG HOẠT ĐỘNG):\n");
            currentAttributes.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
            sb.append("=> Nếu tin nhắn mới thể hiện rõ ràng việc chuyển sang danh mục/hãng sản phẩm KHÁC với ngữ cảnh hiện tại, hãy đặt contextSwitch = true.\n\n");
        }

        if (history != null && !history.isEmpty()) {
            sb.append("Lịch sử trò chuyện gần đây để tham khảo ngữ cảnh:\n");
            history.forEach(h -> sb.append("- ").append(h).append("\n"));
            sb.append("\n");
        }
        
        sb.append("Tin nhắn người dùng hiện tại: \"").append(message).append("\"\n");
        sb.append("Trả về JSON:");
        return sb.toString();
    }
}
