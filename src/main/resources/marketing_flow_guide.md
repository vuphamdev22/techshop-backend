# BẢN PHÂN TÍCH LUỒNG KỸ THUẬT & NGHIỆP VỤ PHÂN HỆ MARKETING & CAMPAIGN

Phân hệ Email Marketing trong hệ thống VoltTech Store là một giải pháp tự động hóa tiếp thị (Marketing Automation) hoàn chỉnh, giúp doanh nghiệp tiếp cận đúng đối tượng khách hàng dựa trên hành vi và đặc điểm cá nhân hóa.

---

## 1. CƠ CHẾ LẬP LỊCH TỰ ĐỘNG (AUTOMATED SCHEDULER & CRON JOBS)

Hệ thống sử dụng các bộ lập lịch tự động của Spring Boot (`@Scheduled`) để tự động chạy các tác vụ ngầm quét dữ liệu theo chu kỳ thời gian thực.
- **Class**: `com.techshop.backend.scheduler.EmailMarketingScheduler`

| Tác vụ quét | Biểu thức Cron | Thời điểm chạy | Phương thức dịch vụ kích hoạt |
| :--- | :--- | :--- | :--- |
| **Chiến dịch lập lịch** | `0 * * * * *` | Mỗi phút một lần | `emailMarketingService.sendScheduledCampaigns()` |
| **Giỏ hàng bỏ quên** | `0 */30 * * * *` | Mỗi 30 phút một lần | `emailMarketingService.scanAndProcessAbandonedCarts()` |
| **Tái tương tác** | `0 0 1 * * *` | Hàng ngày lúc 01:00 AM | `emailMarketingService.scanAndProcessInactiveUsers()` |
| **Chúc mừng sinh nhật** | `0 0 8 * * *` | Hàng ngày lúc 08:00 AM | `emailMarketingService.scanAndProcessBirthdayEmails()` |

---

## 2. QUY TRÌNH GỬI CHIẾN DỊCH EMAIL HÀNG LOẠT (EMAIL CAMPAIGN)

Khi một chiến dịch được kích hoạt gửi, hệ thống thực hiện qua các bước kỹ thuật sau:

### Bước 2.1: Xây dựng truy vấn động lọc tệp khách hàng (Target Filtering)
- **Hàm**: `buildUserQuery(CampaignFilterRequest filter, boolean countOnly)`
- **Xử lý**: Hệ thống tự động ghép chuỗi **JPQL (Java Persistence Query Language)** động dựa trên các bộ lọc mà người quản trị cấu hình:
  - Lọc theo Cấp hạng thành viên (`membershipLevel`).
  - Lọc theo Mức chi tiêu tối thiểu (`minTotalSpent`).
  - Lọc theo Số ngày không đăng nhập (`inactiveDays`).
  - Lọc theo Giới tính (`gender`).
  - Lọc chỉ gửi cho người đăng ký nhận tin (`emailSubscribed = true`).

### Bước 2.2: Xử lý bất đồng bộ tránh nghẽn luồng HTTP Thread (Asynchronous Processing)
- **Hàm**: `executeCampaignSending(Long campaignId)` được đánh dấu annotation **`@Async`**.
- **Ý nghĩa kỹ thuật**: Giúp tác vụ gửi hàng ngàn email chạy ngầm trên một Thread Pool riêng biệt của JVM, giải phóng luồng xử lý HTTP chính ngay lập tức để giao diện Admin không bị đơ/treo.

### Bước 2.3: Bộ điều tiết chống bị nhận diện thư rác (Anti-Spam Rate Limit)
1. **Tần suất nhận mail marketing**: Hệ thống kiểm tra điều kiện `user.getLastMarketingEmailSentAt()`. Nếu khách hàng đã nhận email marketing trong vòng **3 ngày gần nhất**, hệ thống sẽ tự động bỏ qua để tránh gây phiền hà cho khách hàng.
2. **Độ trễ khi gửi SMTP**: Thực hiện gọi `Thread.sleep(200)` (trễ 0.2 giây) sau mỗi email được gửi đi. Việc này giúp giảm tải cho Mail Server và ngăn chặn việc bị các máy chủ lớn như Gmail đánh dấu IP của hệ thống là Spam.

---

## 3. CƠ CHẾ ĐO LƯỜNG HIỆU QUẢ (TRACKING ANALYTICS)

Hệ thống sử dụng kỹ thuật chèn mã theo dõi độc quyền để đo lường chính xác các chỉ số **Open Rate** (Tỷ lệ mở) và **Click-Through Rate** (Tỷ lệ click link):
- **Phương thức xử lý link**: `appendTracking(htmlContent, token)`

### 3.1. Kỹ thuật theo dõi Mở email (Open Rate Tracking)
- Hệ thống tạo ra một chuỗi Token ngẫu nhiên (UUID) duy nhất cho mỗi email gửi tới mỗi user.
- Chèn một thẻ ảnh ẩn có kích thước 1x1 pixel vào cuối nội dung email:
  ```html
  <img src="http://localhost:8080/api/public/track/open?token=TRACKING_TOKEN_VI_DU" width="1" height="1" style="display:none;" />
  ```
- Khi người dùng mở mail, trình đọc email tự động tải ảnh này $\to$ Endpoint `/api/public/track/open` được gọi $\to$ Hệ thống cập nhật bản ghi trong bảng `email_logs`: `opened = true`, `opened_at = LocalDateTime.now()`.

### 3.2. Kỹ thuật theo dõi Click link (Click Rate Tracking)
- Hệ thống quét qua toàn bộ mã HTML của email và thay thế tất cả các thẻ liên kết `href="..."` thành địa chỉ redirect trung gian:
  ```html
  href="http://localhost:8080/api/public/track/click?token=TRACKING_TOKEN&redirect=LINK_GOC"
  ```
- Khi người dùng click vào link $\to$ Trình duyệt gọi tới endpoint `/api/public/track/click` $\to$ Hệ thống ghi nhận `clicked = true`, `clicked_at = LocalDateTime.now()` vào nhật ký $\to$ Thực hiện redirect (chuyển hướng) người dùng về trang đích ban đầu.

---

## 4. KỊCH BẢN CHĂM SÓC KHÁCH HÀNG TỰ ĐỘNG (LIFECYCLE EMAILS)

1. **Welcome Email (Chào mừng)**: Được gọi ngay khi tài khoản đăng ký thành công qua hàm `triggerWelcomeEmail(userId)`. Tự động đính kèm mã Voucher Bronze 5% để chào đón thành viên mới.
2. **Abandoned Cart Email (Giỏ hàng bỏ quên)**: Hàm `scanAndProcessAbandonedCarts()` tự động quét các giỏ hàng đã có sản phẩm nhưng không có cập nhật/thanh toán trong vòng 1-2 giờ qua để gửi email nhắc nhở kèm danh sách sản phẩm họ đã bỏ quên.
3. **Comeback Email (Tái tương tác)**: Hàm `scanAndProcessInactiveUsers()` quét những khách hàng không đăng nhập hệ thống trong vòng 30 ngày để gửi email nhớ nhung kèm voucher kéo họ quay lại mua sắm.
