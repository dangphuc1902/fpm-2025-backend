# 🐳 Hướng Dẫn Đóng Gói Docker & Chạy Kịch Bản Test Tự Động (FPM-2025)

Tài liệu này hướng dẫn cách chạy Monolith trên môi trường Docker và sử dụng kịch bản test tích hợp tự động để kiểm thử toàn diện các tính năng của hệ thống.

---

## 1. Cơ Chế Đóng Gói Docker Tối Ưu (Host-Built JAR Copy)

Trong môi trường thực tế, dự án sử dụng một số thư viện dùng chung (`fpm-common`, `fpm-domain`, `fpm-security`) được lưu trữ trên GitHub Packages riêng tư. Khi biên dịch bên trong container (Multi-stage build), Maven sẽ bị chặn bởi lỗi **401 Unauthorized** do thiếu thông tin xác thực.

### Giải pháp tối ưu đã triển khai:
*   Chúng ta biên dịch dự án ngay trên máy host (nơi Maven đã được xác thực hoặc đã lưu cache các thư viện con trong thư mục `.m2/repository`).
*   Tệp `fpm-monolith/Dockerfile` được đơn giản hóa để chỉ sao chép tệp JAR đã biên dịch sẵn từ thư mục `target` của host vào container:

```dockerfile
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy tệp JAR đã được build sẵn từ host
COPY target/fpm-monolith-1.0.0-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

Cơ chế này giúp:
1.  **Tránh lỗi 401**: Biên dịch hoàn toàn độc lập với GitHub Packages mạng.
2.  **Tốc độ build cực nhanh**: Docker image chỉ mất dưới 2 giây để đóng gói sau khi tệp JAR đã được biên dịch xong.

---

## 2. Kịch Bản Chạy Tự Động (Orchestrator Script)

Tệp script [run_monolith_docker_tests.ps1](file:///d:/WorkSpace/App_Dev/FPM_Project/Backend/run_monolith_docker_tests.ps1) giúp tự động hóa toàn bộ quy trình:
1.  Biên dịch dự án Monolith bằng Maven: `mvn clean package -pl fpm-monolith -DskipTests`
2.  Tự động sao chép tệp cấu hình `.env.template` thành `.env` nếu chưa tồn tại.
3.  Dừng các container cũ và khởi động lại toàn bộ Docker Compose stack của Monolith nằm tại `fpm-monolith/docker-compose.yml` (Nginx, Postgres, Redis, backend-monolith) độc lập hoàn toàn với các microservices của máy chủ.
4.  Liên tục poll (tru vấn) endpoint `/actuator/health` của Spring Boot cho đến khi nhận được trạng thái `UP`.
5.  Kích hoạt chạy kịch bản Python kiểm thử tích hợp toàn bộ các API.

---

## 3. Kịch Bản Test Tự Động (API Integration Test Suite)

Tệp Python [scratch/test_api.py](file:///d:/WorkSpace/App_Dev/FPM_Project/Backend/scratch/test_api.py) sử dụng thư viện chuẩn của Python (`urllib.request`) để kiểm tra toàn bộ các tính năng từ đầu đến cuối (E2E) một cách tự động, không yêu cầu cài đặt thêm thư viện ngoài (`requests`):

### Danh sách 17 Test Cases được thực thi liên tục:
1.  **Register User**: Tạo một tài khoản ngẫu nhiên mới qua `POST /api/v1/auth/register`.
2.  **Login User**: Đăng nhập qua `POST /api/v1/auth/login` để lấy JWT access token.
3.  **Get Profile**: Gọi `GET /api/v1/users/me` với Token để kiểm tra bộ lọc JWT.
4.  **Get Preferences**: Lấy tùy chỉnh cá nhân qua `GET /api/v1/users/preferences`.
5.  **Create Family**: Tạo nhóm gia đình mới (`POST /api/v1/families`) và kiểm tra bộ lọc tự động tiêm header `X-User-Id`.
6.  **Get Families**: Lấy danh sách gia đình tham gia.
7.  **Create Wallet**: Tạo ví tiền mặt mới với số dư khởi tạo `2,000,000 VND`.
8.  **Get Categories**: Lấy danh sách danh mục để chọn ID cho giao dịch.
9.  **Create Transaction**: Ghi nhận một chi tiêu mới `150,000 VND` qua `POST /api/v1/transactions`.
10. **Verify Wallet Balance**: Lấy thông tin ví để xác thực số dư mới chính xác là `1,850,000 VND` (Đã trừ đi 150k giao dịch).
11. **Upload Attachment**: Tải ảnh hóa đơn đính kèm lên giao dịch qua endpoint `/attachments` mới.
12. **Delete Attachment**: Xóa ảnh đính kèm khỏi giao dịch.
13. **Get Dashboard**: Lấy dữ liệu tổng quan dashboard và xác thực hoạt động của Redis cache.
14. **Get Monthly Report**: Xuất báo cáo tổng kết tháng.
15. **AI NLP Extraction**: Gửi câu lệnh "Ăn sáng phở bò 45000" lên Gemini AI / Rule-based để trích xuất số tiền và danh mục chi tiêu tự động.
16. **AI Chat Advisor**: Gửi câu hỏi tư vấn tài chính lên trợ lý AI.
17. **OCR Extraction**: Gửi tệp text mô phỏng hóa đơn lên Tesseract OCR để trích xuất cửa hàng và số tiền.

---

## 4. Hướng Dẫn Thực Hiện

### Bước 1: Khởi động Docker Desktop
Hãy đảm bảo ứng dụng **Docker Desktop** đã được bật và đang chạy trên hệ thống của bạn.

### Bước 2: Chạy Script Tự Động
Mở terminal PowerShell tại thư mục `Backend` và chạy lệnh sau:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_monolith_docker_tests.ps1
```

### Bước 3: Xem Kết Quả
Script sẽ tự động build, start container, đợi hệ thống UP và chạy tests. Bạn sẽ nhận được báo cáo tổng kết dạng:
```
============================================================
                       TEST RUN SUMMARY
============================================================
 Register User                      : PASS
 Login User                         : PASS
 Get Profile                        : PASS
 Get Preferences                    : PASS
 Create Family                      : PASS
 Get Families                       : PASS
 Create Wallet                      : PASS
 Get Categories                     : PASS
 Create Transaction                 : PASS
 Verify Wallet Balance              : PASS
 Upload Attachment                  : PASS
 Delete Attachment                  : PASS
 Get Dashboard                      : PASS
 Get Monthly Report                 : PASS
 AI NLP Extraction                  : PASS
 AI Chat Advisor                    : PASS
 OCR Extraction                     : PASS
------------------------------------------------------------
Total Tests Run: 17
Passed         : 17
Failed         : 0
============================================================
```

---
*Tài liệu hướng dẫn Docker & Test được thực hiện bởi Trợ lý AI (Antigravity).*
