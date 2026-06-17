# BẢN PHÂN TÍCH LUỒNG KỸ THUẬT & NGHIỆP VỤ AI CHATBOT VOLTBOT

Dưới đây là chi tiết luồng hoạt động từ Client (Frontend) đến Server (Backend), đi qua các Class, Controller, Service và Database cụ thể để phục vụ việc giải thích/bảo vệ đồ án tốt nghiệp.

---

## BƠ ĐỒ LUỒNG DỮ LIỆU TỔNG QUAN

1. **Client** (User gửi tin nhắn) $\to$ 
2. **`ChatController`** (API Endpoint `/api/chat/send`) $\to$ 
3. **`ChatServiceImpl`** (Hàm `processChatMessage`) $\to$ 
4. **`ChatSessionMemory`** (Phục hồi/Khởi tạo session) $\to$ 
5. **AI Parameter Extractor** (Bóc tách bộ lọc tìm kiếm) $\to$ 
6. **Database Query** (Lọc sản phẩm khớp bộ lọc + FAQs) $\to$ 
7. **Gemini API Call** (Tạo Prompt hệ thống + Sinh câu trả lời tư vấn) $\to$ 
8. **Client** (Hiển thị câu trả lời + Thẻ sản phẩm gợi ý).

---

## CHI TIẾT CÁC BƯỚC VÀ PHƯƠNG THỨC XỬ LÝ (CODE WALKTHROUGH)

### BƯỚC 1: Tiếp nhận Request tại Controller
- **Class**: `com.techshop.backend.controller.ChatController`
- **Phương thức**: `sendMessage(@RequestBody ChatRequest request)`
- **Xử lý**: 
  - Tiếp nhận DTO `ChatRequest` (chứa `sessionId`, `message` của user và `email` nếu đã đăng nhập).
  - Gọi phương thức nghiệp vụ: `chatService.processChatMessage(request)`.

### BƯỚC 2: Phục hồi Context Hội thoại (Session Context)
- **Class**: `com.techshop.backend.service.ChatSessionMemory`
- **Phương thức**: `getOrCreateSession(sessionId)`
- **Xử lý**:
  - Lấy ra đối tượng `SessionContext` quản lý bộ nhớ đệm hội thoại. Đối tượng này chứa:
    - `history`: Lịch sử các câu thoại trước đó giữa User và Bot.
    - `attributes`: Map các bộ lọc tìm kiếm đang hoạt động (`category`, `brand`, `minPrice`, `maxPrice`, `searchQuery`).

### BƯỚC 3: Thiết lập lại ngữ cảnh (Nếu có lệnh Reset)
- **Class**: `com.techshop.backend.service.Impl.ChatServiceImpl`
- **Xử lý**:
  - Kiểm tra nếu tin nhắn người dùng chứa các cụm từ: `"quay lại"`, `"menu chính"`, `"reset"`, hoặc click nút `"🔙 Menu chính"`.
  - Thực hiện gọi `session.clear()` để xoá toàn bộ lịch sử trò chuyện và các bộ lọc cũ, đưa hội thoại về trạng thái ban đầu.

### BƯỚC 4: Bóc tách tham số bộ lọc (AI Parameter Extraction)
Mục tiêu là xác định xem người dùng đang muốn tìm sản phẩm nào (Laptop, Điện thoại, hãng gì, tầm giá bao nhiêu).
- **Trường hợp dùng Gemini AI**:
  - Gọi hàm `buildExtractionPrompt(message, session.getHistory(), attributes)`. Prompt này hướng dẫn AI đọc tin nhắn của user và trả về định dạng **JSON** chuẩn.
  - Sử dụng `geminiService.generateContent` để gửi prompt sang Gemini API.
  - Nhận chuỗi JSON phản hồi và phân tích (Parse) bằng `ObjectMapper`.
  - Cờ **`contextSwitch`**: Nếu AI phát hiện người dùng chuyển nhóm chủ đề (Ví dụ: Đang hỏi Laptop Dell lại chuyển sang Điện thoại iPhone), hệ thống sẽ tự động gọi `session.clear()` để dọn sạch bộ lọc rác cũ.
- **Trường hợp Fallback Cục bộ (Local Fallback Parser)**:
  - Nếu không có API Key, hệ thống tự động chạy bộ phân tích từ khóa Regex cục bộ:
    1. Gọi `detectAndHandleCategorySwitch(lowerMsg, attributes, session)`: Nếu phát hiện danh mục mới qua từ khóa (ví dụ: có chữ "điện thoại" $\to$ chuyển danh mục sang `Smartphones`), nếu danh mục này khác danh mục cũ $\implies$ Gọi `session.clear()`.
    2. Gọi `extractBrandFilter(lowerMsg, attributes)`: So khớp các từ khóa hãng như `apple`, `dell`, `asus`,... để lưu vào Map bộ lọc.
    3. Gọi `extractPriceFilter(lowerMsg, attributes)`: So khớp khoảng giá (ví dụ: từ khóa "dưới 10 triệu" hoặc "15 đến 20 triệu" $\to$ Quy đổi theo tỷ giá USD thành `minPrice` và `maxPrice`).

### BƯỚC 5: Truy vấn Database và So khớp sản phẩm (Filtering)
- **Class**: `com.techshop.backend.service.Impl.ChatServiceImpl`
- **Hành động**:
  - Gọi `productRepository.findAll()` để tải danh sách sản phẩm hiện hữu.
  - Gọi hàm `filterProducts(allProducts, attributes, lowerMsg)` để lọc:
    - Nếu bộ lọc `category` tồn tại $\implies$ Lọc sản phẩm có danh mục tương ứng.
    - Nếu bộ lọc `brand` tồn tại $\implies$ So khớp tên sản phẩm, mô tả hoặc tên thương hiệu chứa từ khóa hãng.
    - So khớp khoảng giá `minPrice` $\le$ `price` $\le$ `maxPrice`.
    - Trả về danh sách `matchedProducts` chứa các sản phẩm khớp tiêu chí.

### BƯỚC 6: Thu thập thông tin hỗ trợ khác
- **FAQ (Chính sách cửa hàng)**: Gọi `chatFaqRepository.findAll()` và so khớp từ khóa qua hàm `getMatchedFaqContext` để lấy ra tài liệu chính sách (phí giao hàng, bảo hành...).
- **Voucher khuyến mãi**: Lấy danh sách mã giảm giá đang kích hoạt từ `couponRepository.findAll()`.
- **Cá nhân hóa tài khoản**: Nếu khách hàng đã đăng nhập, truy vấn lịch sử đơn hàng từ `orderRepository.findByUserId(userId)` để đưa vào ngữ cảnh.

### BƯỚC 7: Biên soạn System Prompt & Sinh câu trả lời tư vấn
- **Hàm**: `buildSystemInstruction(...)`
- **Xử lý**:
  - Gộp tất cả dữ liệu: Toàn bộ danh mục cửa hàng (`getFullCatalogContext`), danh sách sản phẩm khớp bộ lọc (`matchedProducts`), các voucher khả dụng, lịch sử mua hàng của khách, và các chính sách FAQ phù hợp.
  - Gửi toàn bộ dữ liệu này làm **System Instruction** (Chỉ dẫn hệ thống) cùng với tin nhắn hiện tại và lịch sử chat sang Gemini API.
  - Gemini AI đóng vai trò là nhân viên tư vấn mua sắm thông minh (VoltBot), đọc hiểu toàn bộ kho hàng và chính sách này để sinh ra câu trả lời tư vấn cá nhân hóa bằng Tiếng Việt.
  - Nếu không có API Key, hàm `generateDynamicLocalResponse(...)` sẽ tự động ghép dữ liệu thô từ cơ sở dữ liệu thành danh sách văn bản hiển thị cho người dùng.

### BƯỚC 8: Đóng gói Phản hồi gửi về Frontend
- **Hàm**: `generateQuickReplies(attributes)` tự động sinh gợi ý nút bấm phản hồi nhanh kế tiếp phù hợp với danh mục đang xem để định hướng trải nghiệm người dùng.
- Trả về DTO `ChatResponse` bao gồm:
  - `text`: Nội dung tư vấn của bot.
  - `products`: Danh sách tối đa 3 sản phẩm gợi ý (được ánh xạ thành `ProductResponse`).
  - `quickReplies`: Các chuỗi nút gợi ý bấm nhanh.
- Frontend nhận gói tin qua WebSocket hoặc REST API và kết xuất (render) giao diện trực quan cho người dùng.
