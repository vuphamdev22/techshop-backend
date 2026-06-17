package com.techshop.backend.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techshop.backend.service.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateContent(String systemInstruction, String prompt, List<String> history) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to local intelligence.");
            return generateLocalFallback(prompt);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            // Build request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Build the JSON payload using standard Maps
            Map<String, Object> requestBody = new HashMap<>();

            // 1. Add System Instruction
            Map<String, Object> sysInstructionMap = new HashMap<>();
            sysInstructionMap.put("parts", Collections.singletonList(Collections.singletonMap("text", systemInstruction)));
            requestBody.put("systemInstruction", sysInstructionMap);

            // 2. Add Contents (History + current prompt)
            List<Map<String, Object>> contents = new ArrayList<>();

            // Convert and add conversational history
            if (history != null) {
                for (String turn : history) {
                    if (turn == null || !turn.contains(":")) continue;
                    int splitIdx = turn.indexOf(":");
                    String role = turn.substring(0, splitIdx).trim().toLowerCase();
                    String text = turn.substring(splitIdx + 1).trim();

                    // Gemini roles: "user" or "model"
                    if ("bot".equals(role)) role = "model";

                    Map<String, Object> contentTurn = new HashMap<>();
                    contentTurn.put("role", role);
                    contentTurn.put("parts", Collections.singletonList(Collections.singletonMap("text", text)));
                    contents.add(contentTurn);
                }
            }

            // Add the final user prompt
            Map<String, Object> finalTurn = new HashMap<>();
            finalTurn.put("role", "user");
            finalTurn.put("parts", Collections.singletonList(Collections.singletonMap("text", prompt)));
            contents.add(finalTurn);

            requestBody.put("contents", contents);

            // 3. Add generation config
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 1000);
            requestBody.put("generationConfig", genConfig);

            // Execute POST request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse generated content out of Gemini Response JSON
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate content via Gemini API. Falling back to local intelligence.", e);
        }

        return generateLocalFallback(prompt);
    }

    private String generateLocalFallback(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("laptop")) {
            return "💻 **Tư vấn Laptop tại VoltTech:**\n\nChúng tôi đang phân phối các dòng máy nổi bật:\n- **MacBook Pro 16\" M3 Max** (Cực mạnh cho đồ họa & lập trình)\n- **Dell XPS 15 9530** (Màn hình OLED, thanh lịch)\n- **ASUS ROG Zephyrus G14** (Gaming mỏng nhẹ)\n\nBạn cần sử dụng laptop chuyên cho tác vụ học tập, văn phòng hay chơi game cấu hình cao để mình tư vấn cấu hình phù hợp nhất nhé? 👌";
        }
        if (lower.contains("điện thoại") || lower.contains("phone")) {
            return "📱 **Tư vấn Điện thoại thông minh:**\n\nCác dòng flagship cực hot tại VoltTech:\n- **iPhone 15 Pro Max** (Thiết kế Titanium bền bỉ, sang trọng)\n- **Samsung Galaxy S24 Ultra** (Kèm bút S Pen chuyên nghiệp)\n- **Google Pixel 8 Pro** (Camera AI siêu đỉnh)\n\nBạn có ưu tiên thương hiệu nào hoặc mức giá mong muốn tầm bao nhiêu không ạ?";
        }
        if (lower.contains("ship") || lower.contains("giao hàng") || lower.contains("vận chuyển")) {
            return "🚚 **Chính sách giao hàng của VoltTech:**\n\n- **Miễn phí giao hàng (Free Shipping)** cho mọi đơn hàng từ **$99 trở lên**.\n- Với đơn hàng dưới $99, phí giao hàng toàn quốc cố định là **$9.99**.\n- **Thời gian giao nhận**: Từ 1-2 ngày làm việc (các tỉnh thành lớn thường nhận hàng sau 24h).";
        }
        if (lower.contains("giảm giá") || lower.contains("voucher") || lower.contains("khuyến mãi")) {
            return "🎁 **Ưu đãi ngập tràn tại VoltTech:**\n\n- Nhập mã **VOLT10** tại màn hình giỏ hàng để được **giảm ngay 10%** cho đơn hàng đầu tiên của bạn!\n- Ngoài ra, chúng tôi thường xuyên tổ chức Flash Sale vào thứ 6 hàng tuần với mức giảm giá lên đến 30% cho các sản phẩm hot.";
        }
        if (lower.contains("đổi trả") || lower.contains("hoàn tiền")) {
            return "🛡️ **Chính sách đổi trả & bảo hành của VoltTech:**\n\n- **Đổi mới 1-đổi-1 trong vòng 30 ngày đầu tiên** nếu sản phẩm phát sinh lỗi kỹ thuật từ nhà sản xuất (miễn phí phí vận chuyển thu hồi).\n- **Điều kiện**: Sản phẩm còn nguyên hộp, đầy đủ phụ kiện và không trầy xước.";
        }
        return "Chào bạn! Tôi là VoltBot 🤖. Tôi có thể tư vấn các dòng sản phẩm công nghệ (Laptop, Điện thoại, Tai nghe, Phụ kiện), hướng dẫn săn voucher giảm giá, phí giao hàng hoặc đổi trả sản phẩm. Bạn hãy thử đặt câu hỏi cụ thể hơn nhé!";
    }
}
