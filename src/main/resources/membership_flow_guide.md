# BẢN PHÂN TÍCH LUỒNG KỸ THUẬT & NGHIỆP VỤ HỆ THỐNG THÀNH VIÊN (MEMBERSHIP)

Dưới đây là tài liệu chi tiết mô tả cách hoạt động của hệ thống Tích điểm, Kinh nghiệm (EXP), và Nâng cấp thành viên tự động phục vụ việc bảo vệ đồ án tốt nghiệp.

---

## 1. ĐIỂM KÍCH HOẠT (TRIGGER POINT)
Hệ thống thành viên được kích hoạt tự động khi **Đơn hàng được giao thành công** (`OrderStatus.DELIVERED`).
1. Khi Quản trị viên cập nhật trạng thái đơn hàng:
   - **Class**: `com.techshop.backend.service.Impl.OrderServiceImpl`
   - **Phương thức**: `updateOrderStatus(orderId, status)`
2. Nếu trạng thái mới là `DELIVERED`, hệ thống gọi bất đồng bộ/đồng bộ dịch vụ thành viên:
   ```java
   membershipService.processOrderCompletion(order.getUser().getId(), order.getTotalPrice());
   ```

---

## 2. LUỒNG XỬ LÝ CHI TIẾT TẠI `MembershipServiceImpl.processOrderCompletion`

### Bước 1: Khôi phục thông tin Người dùng (User)
- Tìm kiếm thực thể `User` từ database thông qua `userRepository.findById(userId)`.
- Nếu không tìm thấy, hệ thống quăng ra một ngoại lệ `AppException(ErrorCode.USER_NOT_FOUND)`.

### Bước 2: Cập nhật chỉ số Tích lũy (Total Spent & Total Orders)
Hệ thống cập nhật cộng dồn hai thông số dùng để đánh giá cấp bậc thành viên:
- **`totalSpent`** (Tổng tiền chi tiêu): `user.setTotalSpent(currentSpent + orderAmount)`
- **`totalOrders`** (Tổng số đơn hàng): `user.setTotalOrders(currentOrders + 1)`

### Bước 3: Tính toán Điểm thưởng tích lũy (Reward Points)
Điểm thưởng được sử dụng cho việc đổi quà/chiết khấu sau này:
- Cơ chế quy đổi cơ bản: **10% giá trị đơn hàng** (Hệ số `POINTS_PER_DOLLAR = 0.1` tức chi tiêu $10 được 1 điểm).
- **Đặc quyền Vàng (Gold)**: Nếu cấp độ hiện tại của khách hàng là `GOLD`, họ sẽ được **nhân đôi điểm tích lũy** (`amount * POINTS_PER_DOLLAR * 2`).
- Điểm mới được cộng dồn vào thuộc tính `rewardPoints` của User.

### Bước 4: Tích lũy Điểm kinh nghiệm (EXP)
Điểm kinh nghiệm đại diện cho sự gắn kết lâu dài:
- Tỷ lệ quy đổi: **$1 chi tiêu tích luỹ được 10 EXP** (`EXP_PER_DOLLAR = 10.0`).
- Điểm EXP mới được cộng dồn vào thuộc tính `exp` của User.

### Bước 5: Kiểm tra nâng hạng Thành viên (Upgrade Check)
Hệ thống tính toán cấp bậc mới của User bằng hàm nội bộ `calculateLevel(user)`:
- **Ngưỡng quy định**:
  - **Hạng ĐỒNG (Bronze)**: Mặc định khi đăng ký tài khoản.
  - **Hạng BẠC (Silver)**: Tổng đơn hàng $\ge 3$ **HOẶC** tổng chi tiêu $\ge \$300$.
  - **Hạng VÀNG (Gold)**: Tổng đơn hàng $\ge 10$ **HOẶC** tổng chi tiêu $\ge \$1000$.

### Bước 6: Xử lý khi thăng hạng (Level Up)
Nếu cấp bậc mới (`newLevel`) khác với cấp bậc cũ (`oldLevel`):
1. **Cập nhật trạng thái**: Đặt lại `user.setMembershipLevel(newLevel)`.
2. **Ghi nhật ký lịch sử**: Khởi tạo và lưu một thực thể `MembershipHistory` lưu thông tin: hạng cũ, hạng mới, thời gian nâng cấp và ghi chú lý do nâng cấp.
3. **Tự động cấp Voucher thăng hạng**:
   - Gọi phương thức `generateUpgradeVoucher(user, newLevel)`.
   - Tạo mã coupon ngẫu nhiên theo cấu trúc dạng `SILVER-XXXXXX` hoặc `GOLD-XXXXXX`.
   - Lưu coupon mới này vào bảng `Coupon` (với thời hạn sử dụng 60 ngày).
   - Liên kết Coupon này với User thông qua thực thể `UserVoucher` và lưu vào DB.

---

## 3. MÁP CƠ SỞ DỮ LIỆU LIÊN QUAN (DATABASE SHEMA)

1. **Bảng `users`**:
   - `membership_level`: Enum lưu cấp bậc (`BRONZE`, `SILVER`, `GOLD`).
   - `reward_points`: Tổng điểm tích lũy có thể tiêu dùng.
   - `total_spent`: Tổng số tiền đã thanh toán từ trước tới nay.
   - `total_orders`: Tổng số đơn hàng hoàn thành.
   - `exp`: Tổng điểm kinh nghiệm.
2. **Bảng `membership_history`**:
   - `user_id`: Khóa ngoại liên kết bảng User.
   - `from_level` & `to_level`: Trạng thái chuyển đổi cấp hạng.
   - `upgraded_at`: Ngày nâng cấp.
3. **Bảng `user_vouchers`**:
   - Khóa ngoại liên kết `user_id` và `coupon_id` để cấp phát voucher thăng hạng riêng tư cho tài khoản đó, tránh việc mã thăng hạng bị lộ và sử dụng bởi tài khoản khác.
