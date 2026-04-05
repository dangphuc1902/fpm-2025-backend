# 🗺️ Lộ Trình Hoàn Thiện Dự Án (Project Roadmap) – FPM-2025

> **Cập nhật:** 2026-04-04 (Review thực trạng)  
> Chi tiết đầy đủ xem tại: `Backend_Implementation_Status.md`

---

## 1. Trạng Thái Hiện Tại (Audit)

### 🏗️ Hạ Tầng (Infrastructure)
- [x] **`eureka-server` (8761)**: ✅ Hoàn chỉnh.
- [x] **`api-gateway` (8080)**: ✅ JWT filter, routing, logging đã chạy.
- [x] **`config-service` (8888)**: ✅ Đã tồn tại tại thư mục `config` ở root. Spring Cloud Config Server sẵn sàng.

### 💼 Core Business Services
- [x] **`user-auth-service` (8081)**: ✅ ~98% - Login (Google/Local), Register, gRPC Auth, Family CRUD.
- [x] **`wallet-service` (8082)**: ✅ ~95% - Wallet CRUD, Shared Wallet, Category Hub (CRUD Category). Gỡ bỏ TransactionController.
- [x] **`transaction-service` (8083)**: ✅ ~95% - Full CRUD `/api/v1/transactions`, gRPC Server/Client, Kafka.
- [x] **`reporting_service` (8084)**: ✅ ~90% - Dashboard, Monthly report, Export (PDF/Excel), Chart data ( spending-by-category, trends).
- [x] **`notification-service` (8085)**: ✅ ~98% - FCM thật, Bank SMS Parser (MB, VCB, MoMo). Kafka publish `notification.parsed`.

### 🧪 High-Tech Services
- [x] **`ocr-service` (8086)**: 🟢 ~60% - Đã có cấu trúc, Tesseract wrapper hoạt động (cần native config).
- [x] **`ai-service` (8087)**: 🟢 ~50% - Đã có cấu trúc, NLP stub hoạt động.

---

## 2. Kế Hoạch Tiếp Theo (Roadmap)

### 🚨 Bước 1: Ổn Định Core (Ưu Tiên Cao) - ✅ HOÀN THÀNH
- [x] Tích hợp gRPC giữa `transaction-service` và `wallet-service` để cập nhật số dư.
- [x] Triển khai Family management API chuẩn trong `user-auth-service`.
- [x] Xây dựng `notification-service` xử lý tin nhắn ngân hàng (Auto-matching).

### 🚨 Bước 2: Nâng Cấp Trải Nghiệm & Bảo Mật - ✅ HOÀN THÀNH
- [x] Bổ sung **Rate Limiting** (Redis) tại API Gateway để tránh Spam.
- [x] Implement **Circuit Breaker** (Resilience4j) tại Gateway cho các service.
- [x] Hoàn thiện **Budgeting Logic** trong `reporting_service` (Kafka Alerts).
- [x] Implement async export job system hoàn chỉnh (Job monitoring + download link).

### 🔮 Bước 3: Services Nâng Cao (Tương Lai Gần)
- [ ] **OCR Production**: Cấu hình Docker image với Tesseract native libraries và Vietnamese data training.
- [ ] **AI Integration**: Thay thế các NLP Stub bằng API thật (Google Gemini 1.5 Flash) để trích xuất giao dịch từ tin nhắn chat phức tạp.
- [ ] **Anomaly Detection Realtime**: Thu thập dữ liệu giao dịch mẫu để phát hiện các giao dịch không bình thường (ví dụ: bỗng dưng tiêu x3 mức bình thường).

### 🔧 Bước 4: Kỹ Thuật & QA
- [ ] Thống nhất API prefix: Đảm bảo 100% controller dùng `/api/v1/`.
- [ ] Viết Unit Test cho Bank SMS Parser (đây là phần cực kỳ quan trọng).
- [ ] Swagger/OpenAPI: Export đầy đủ API spec để client Mobile generate code tự động.

---

## 3. Dọn Dẹp Tài Liệu (Documents Cleaning)
Dựa trên review 04/04, các file sau sẽ bị thay thế hoặc gỡ bỏ:
- [x] `task_role.md` → 🔴 Lỗi thời.
- [x] `doc_feature.md` → 🔴 Trùng lặp với `Backend_Implementation_Status.md`.
- [ ] `document_api.md` → Cần sửa prefix `/api/` thành `/api/v1/`.

