# Hướng dẫn Điều chỉnh Thời gian Kiểm thử Marketing

Tài liệu này ghi nhớ các vị trí trong mã nguồn (Backend) giúp bạn dễ dàng chỉnh sửa lại thời gian chạy kiểm thử khi cần thiết trong tương lai.

---

## 1. Điều chỉnh chu kỳ quét của Scheduler

Mở tệp: `backend/src/main/java/com/techshop/backend/scheduler/EmailMarketingScheduler.java`

*   **Giỏ hàng bỏ quên (Abandoned Cart)**:
    *   *Sản xuất (Mặc định 30 phút)*: `@Scheduled(cron = "0 */30 * * * *")`
    *   *Kiểm thử (Mỗi phút)*: `@Scheduled(cron = "0 * * * * *")`
*   **Kích cầu khách cũ (Comeback)**:
    *   *Sản xuất (1:00 AM hàng ngày)*: `@Scheduled(cron = "0 0 1 * * *")`
    *   *Kiểm thử (Mỗi phút)*: `@Scheduled(cron = "0 * * * * *")`
*   **Chúc mừng sinh nhật (Birthday)**:
    *   *Sản xuất (8:00 AM hàng ngày)*: `@Scheduled(cron = "0 0 8 * * *")`
    *   *Kiểm thử (Mỗi phút)*: `@Scheduled(cron = "0 * * * * *")`

---

## 2. Điều chỉnh điều kiện lọc và giãn cách gửi (Rate Limits)

Mở tệp: `backend/src/main/java/com/techshop/backend/service/Impl/EmailMarketingServiceImpl.java`

### A. Chiến dịch giỏ hàng bỏ quên
Tìm kiếm hàm `scanAndProcessAbandonedCarts()`:
*   *Sản xuất (Từ 1 đến 2 giờ trước)*:
    ```java
    LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
    LocalDateTime threshold = LocalDateTime.now().minusHours(2);
    ```
*   *Kiểm thử (Từ 10 giây đến 5 phút trước)*:
    ```java
    LocalDateTime oneHourAgo = LocalDateTime.now().minusSeconds(10);
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
    ```

### B. Chiến dịch kích cầu khách cũ (Comeback / Inactive User)
Tìm kiếm hàm `scanAndProcessInactiveUsers()`:
*   *Sản xuất (Không đăng nhập trong 30 ngày, giãn cách gửi email marketing là 15 ngày)*:
    ```java
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    ...
    if (user.getLastMarketingEmailSentAt() != null && 
        user.getLastMarketingEmailSentAt().isAfter(LocalDateTime.now().minusDays(15))) {
    ```
*   *Kiểm thử (Không đăng nhập trong 10 giây, giãn cách gửi email marketing là 10 giây)*:
    ```java
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusSeconds(10);
    ...
    if (user.getLastMarketingEmailSentAt() != null && 
        user.getLastMarketingEmailSentAt().isAfter(LocalDateTime.now().minusSeconds(10))) {
    ```

### C. Giãn cách gửi email khuyến mãi hàng loạt (Campaign Rate Limit)
Tìm kiếm trong hàm `executeCampaignSending()`:
*   *Sản xuất (Tối thiểu 3 ngày mới gửi lại email marketing)*:
    ```java
    if (user.getLastMarketingEmailSentAt() != null && 
        user.getLastMarketingEmailSentAt().isAfter(LocalDateTime.now().minusDays(3))) {
    ```
*   *Kiểm thử (Tối thiểu 5 giây)*:
    ```java
    if (user.getLastMarketingEmailSentAt() != null && 
        user.getLastMarketingEmailSentAt().isAfter(LocalDateTime.now().minusSeconds(5))) {
    ```
