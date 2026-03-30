# Đánh Giá Hiện Trạng và Lộ Trình Hoàn Thiện Dự Án (Roadmap)

> **Cập nhật:** 2026-03-27 – Review thực tế source code  
> Chi tiết đầy đủ xem tại: `Backend_Implementation_Status.md`

---

## 1. Trạng Thái Các Microservices Hiện Tại

### Hạ Tầng (Infrastructure Services)
- [x] **`eureka-server` (Port 8761)**: ✅ Hoàn chỉnh.
- [x] **`api-gateway` (Port 8080)**: ✅ JWT filter, routing, CORS, logging đã hoạt động. Còn thiếu rate limiting và circuit breaker.
- [x] **`config-service` (Port 8888)**: ✅ Đã tồn tại tại thư mục `config` ở root. Spring Cloud Config Server đã sẵn sàng phục vụ configurations.

### Core Business Services

1. **`user-auth-service` (Port: 8081)** — 🟢 ~95% Hoàn thiện
   - **Đã làm**: Đủ REST APIs auth, Family CRUD, JWT + BlackList Redis. Đã thêm **gRPC Server** (`ValidateToken`, v.v.) và **Kafka publisher** `user.created`.
   - **Chưa hoàn thiện**: Cần kiểm tra lại OAuth2 Google flow.

2. **`wallet-service` (Port: 8082)** — 🟢 ~90% Hoàn thiện
   - **Đã làm**: CRUD ví hoàn chỉnh + bonus endpoints (active, type, total-balance, count); Shared wallet (share/unshare/list); gRPC Server impl đầy đủ; Kafka publisher; RabbitMQ config.
   - **Chưa hoàn thiện**: Redis caching chi tiết cần verify; API `GET /wallets/family/{familyId}` theo spec chưa có.

3. **`transaction-service` (Port: 8083)** — 🟢 ~90% Hoàn thiện
   - **Đã làm**: Đầy đủ CRUD REST APIs (`GET`, `POST`, `PUT`, `DELETE`). Đã implement hoàn chỉnh **gRPC Server** (`getTransactionById`, `getTransactionsByWallet`, v.v.).
   - **Chưa hoàn thiện**: OCR/Voice/Notification mapper endpoints; chưa consume Kafka từ các service khác (ví dụ: `notification.parsed`).

4. **`reporting-service` (Port: 8088)** — 🟢 ~85% Hoàn thiện
   - **Đã làm**: Dashboard, Monthly report, Export PDF/Excel, Biểu đồ `spending-by-category`, `trends`, `budget-comparison`. Đã có Kafka consumer.
   - **Chưa hoàn thiện**: Async export job hoàn chỉnh (xuất qua background task), AI insights mới stubs.

5. **`notification-service` (Port: 8085)** — 🟢 ~90% Hoàn thiện
   - **Đã làm**: RabbitMQ listener, REST APIs (nhận từ bank, đăng ký FCM, lịch sử, unread count). Database + Entities đủ. **Firebase Admin SDK** tích hợp thật (production/simulation dual-mode). **Parser ngân hàng sâu** (MB Bank 4 format, VCB 4 format, MoMo 6 format). **Kafka publish `notification.parsed`** hoàn chỉnh.
   - **Chưa hoàn thiện**: Unit tests chi tiết cho parser, Kafka consumer từ reporting-service.

6. **`category-service` (Port: 8084)** — 🔴 0% (Module chưa tồn tại)
   - **Lưu ý**: `wallet-service` có `CategoryEntity` + `CategoryController` nội bộ. Cần quyết định tách hay gộp.

7. **`ocr-service` (Port: 8086)** — 🔴 0% (Module chưa tồn tại)

8. **`ai-service` (Port: 8087)** — 🔴 0% (Module chưa tồn tại)

---

## 2. Kế Hoạch Triển Khai Tiếp Theo (Roadmap)

### 🚨 Bước 1: Hoàn Thiện Core (Ưu Tiên Cao)

~~**1a. transaction-service — Bổ sung CRUD còn thiếu**~~ (Đã hoàn thành)
- [x] `PUT /api/v1/transactions/{id}` — update giao dịch
- [x] `DELETE /api/v1/transactions/{id}` — xóa giao dịch + publish Kafka
- [x] `GET /api/v1/transactions` — list by user (filter date/category/type, paginate)
- [x] Hoàn thiện gRPC impl (`getTransactionById`, `getTransactionsByDateRange`, v.v.)

~~**1b. user-auth-service — gRPC Server**~~ (Đã hoàn thành)
- [x] Tạo `UserGrpcServiceImpl.java` với `ValidateToken`, `GetUserById`, `GetFamilyMembers`
- [x] Thêm Kafka publish `user.created` (để wallet tạo ví mặc định)

### ⚡ Bước 2: Hoàn Thiện Reporting (Ưu Tiên Trung)

- [x] `GET /api/v1/reports/spending-by-category` (aggregate từ TransactionSummary)
- [x] `GET /api/v1/reports/trends` (so sánh theo tháng)
- [x] `GET /api/v1/reports/budget-comparison`
- [x] Verify `TransactionGrpcClient` hoạt động thực sự

### ⚙️ Bước 3: Nâng Cấp Notification Service

- [x] REST: `POST /receive`, `POST /fcm/register`, `GET /history`
- [x] Entity + Repository (notification_db)
- [x] FCM Firebase integration (Firebase Admin SDK 9.3.0, production/simulation dual-mode)
- [x] Parser thông báo ngân hàng (regex pattern sâu cho MB Bank, VCB, MoMo + generic fallback)
- [x] Kafka publish `notification.parsed` (với đầy đủ fields: amount, type, account, ref, balance)

### 🔮 Bước 4: Services Nâng Cao (Future)

- [x] `category-service` — Quyết định: Giữ CategoryEntity trong `wallet-service` để giảm over-engineering, các service khác gọi qua gRPC hoặc lưu reference ID.
- [x] `ocr-service` — Đã tạo cấu trúc Spring Boot service, tích hợp thư viện Tesseract.
- [x] `ai-service` — Đã tạo cấu trúc, cung cấp API NLP bóc tách giao dịch và phát hiện bất thường (Anomaly Detection).
- [x] `config-service` — Spring Cloud Config Server (đã khởi tạo tại thư mục `config` ở root)

### 🔧 Bước 5: Kỹ Thuật & Chất Lượng

- [x] Thống nhất API prefix: `/api/v1/` cho tất cả endpoints (hiện DashboardController dùng `/api/dashboard`)
- [x] Xóa `TransactionController` trong wallet-service (trùng chức năng với transaction-service)
- [x] Rate limiting tại API Gateway (Redis)
- [x] Circuit breaker Resilience4j
- [x] Swagger/OpenAPI export
