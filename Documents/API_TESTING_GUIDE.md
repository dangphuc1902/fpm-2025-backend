# FPM-2025: Hướng dẫn Kiểm thử API (Postman Guide)

Tài liệu này hướng dẫn bạn cách sử dụng file Postman Collection để kiểm tra toàn bộ hệ thống Microservices thông qua API Gateway.

---

## 1. Chuẩn bị (Prerequisites)
1. **Khởi động Backend:** Đảm bảo toàn bộ Docker containers đang chạy (`docker-compose up -d`).
2. **Import Collection:** Mở Postman, chọn **Import** và chọn file: `Backend/Documents/FPM_2025_Postman_Collection.json`.
3. **Cấu hình Biến (Variables):** 
   - Click vào tên Collection `FPM-2025 Microservices API`.
   - Chuyển sang tab **Variables**.
   - Đảm bảo `base_url` đang là `http://localhost:8080/api/v1`.

---

## 2. Quy trình Kiểm thử (Test Workflow)

### Bước 1: Đăng ký & Đăng nhập (Auth Service)
1. Chạy request **Register**: Tạo một tài khoản mới.
2. Chạy request **Login**: 
   - Sau khi có kết quả trả về, copy giá trị `accessToken`.
   - Quay lại tab **Variables** của Collection, dán token vào biến `token`.
   - **Lưu ý:** Tất cả các request sau này đều sử dụng biến `{{token}}` này để xác thực.

### Bước 2: Quản lý Gia đình (Family) - *Tùy chọn*
1. Chạy request **Create Family**: Tạo một nhóm gia đình.
2. Copy `id` của family trả về, dán vào biến `family_id` trong Collection Variables.

### Bước 3: Quản lý Ví (Wallet Service)
1. Chạy request **Create Wallet**: 
   - Bạn có thể tạo ví cá nhân (không truyền `familyId`).
   - Hoặc tạo ví gia đình bằng cách thêm `"familyId": {{family_id}}` vào Body.
2. Chạy request **Get All Wallets**: Kiểm tra danh sách ví.
3. Copy `id` của một ví vừa tạo, dán vào biến `wallet_id` trong Collection Variables.

### Bước 4: Giao dịch (Transaction Service)
1. Chạy request **Create Transaction**: 
   - Ghi một khoản chi tiêu 50,000đ vào ví (`wallet_id`).
   - Kiểm tra xem số dư ví có bị trừ đi không bằng cách gọi lại `Get All Wallets`.

### Bước 5: Báo cáo (Reporting Service)
1. Chạy request **Get Dashboard**: Xem tổng số dư và thống kê nhanh.
2. Chạy request **Get Monthly Report**: Xem báo cáo chi tiết tháng `2026-03`.

---

## 3. Các lỗi thường gặp (Troubleshooting)
- **404 Not Found:** Gateway chưa nhận diện được Route. Hãy kiểm tra lại file `api-gateway.yml` và đảm bảo đã khởi động lại Gateway.
- **401 Unauthorized:** Token hết hạn hoặc chưa dán Token vào Variables. Hãy thực hiện lại bước Login.
- **503 Service Unavailable:** Service mục tiêu (ví dụ Wallet-service) chưa khởi động xong hoặc bị Crash. Hãy kiểm tra logs bằng lệnh `docker logs fpm-wallet-service`.

---

*Chúc bạn có một buổi kiểm thử thành công! Nếu gặp lỗi lạ, hãy gửi Log cho Antigravity nhé.*
