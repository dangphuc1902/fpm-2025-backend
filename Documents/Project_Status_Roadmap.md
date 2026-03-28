# Đánh Giá Hiện Trạng và Lộ Trình Hoàn Thiện Dự Án (Roadmap)

> **Cập nhật:** 2026-03-27 – Review thực tế source code  
> Chi tiết đầy đủ xem tại: `Backend_Implementation_Status.md`

---

## 1. Trạng Thái Các Microservices Hiện Tại

### Hạ Tầng (Infrastructure Services)
- [x] **`eureka-server` (Port 8761)**: ✅ Hoàn chỉnh.
- [x] **`api-gateway` (Port 8080)**: ✅ JWT filter, routing, CORS, logging đã hoạt động. Còn thiếu rate limiting và circuit breaker.
- [ ] **`config-service` (Port 8888)**: ❌ Module chưa tồn tại trong source. Mỗi service tự quản lý config riêng.

### Core Business Services

1. **`user-auth-service` (Port: 8081)** — 🟡 ~75% Hoàn thiện
   - **Đã làm**: Đủ REST APIs auth (register, login, google OAuth2, validate, logout, refresh), Family CRUD (tạo, danh sách, thành viên, invite), JWT + BlackList Redis, UserPreferences, RBAC entity.
   - **Chưa hoàn thiện**: gRPC Server impl (`ValidateToken`, `GetUserById`) — file impl chưa tìm thấy; Kafka publish `user.created`.

2. **`wallet-service` (Port: 8082)** — 🟢 ~90% Hoàn thiện
   - **Đã làm**: CRUD ví hoàn chỉnh + bonus endpoints (active, type, total-balance, count); Shared wallet (share/unshare/list); gRPC Server impl đầy đủ; Kafka publisher; RabbitMQ config.
   - **Chưa hoàn thiện**: Redis caching chi tiết cần verify; API `GET /wallets/family/{familyId}` theo spec chưa có.

3. **`transaction-service` (Port: 8083)** — 🟡 ~60% Hoàn thiện
   - **Đã làm**: `POST /api/v1/transactions` (tạo giao dịch, gọi gRPC wallet, publish Kafka + RabbitMQ); `GET /wallet/{walletId}` (paged); `GET /{id}`; gRPC Server đã khai báo (structs); Entity đủ.
   - **Chưa hoàn thiện**: PUT/DELETE transactions; GET list by user; gRPC stubs chưa implement logic; Kafka consumer; Voice/OCR/Notification endpoints.

4. **`reporting-service` (Port: 8088)** — 🟡 ~65% Hoàn thiện
   - **Đã làm**: Dashboard (với Redis cache); Monthly report; Export PDF/Excel; Kafka consumer `transaction.created`; Domain models đầy đủ (Budget, CategorySummary, ExportJob, MonthlySummary).
   - **Chưa hoàn thiện**: Spending-by-category, trends, budget-comparison endpoints; Async export job; gRPC client cần verify thực sự hoạt động.

5. **`notification-service` (Port: 8085)** — 🔴 ~15% Hoàn thiện
   - **Đã làm**: RabbitMQ listener nhận message từ `notification.queue` và log ra console.
   - **Chưa hoàn thiện**: Mọi thứ khác — REST API, Database, FCM Firebase, parser ngân hàng, Kafka publisher.

6. **`category-service` (Port: 8084)** — 🔴 0% (Module chưa tồn tại)
   - **Lưu ý**: `wallet-service` có `CategoryEntity` + `CategoryController` nội bộ. Cần quyết định tách hay gộp.

7. **`ocr-service` (Port: 8086)** — 🔴 0% (Module chưa tồn tại)

8. **`ai-service` (Port: 8087)** — 🔴 0% (Module chưa tồn tại)

---

## 2. Kế Hoạch Triển Khai Tiếp Theo (Roadmap)

### 🚨 Bước 1: Hoàn Thiện Core (Ưu Tiên Cao)

**1a. transaction-service — Bổ sung CRUD còn thiếu**
- [ ] `PUT /api/v1/transactions/{id}` — update giao dịch
- [ ] `DELETE /api/v1/transactions/{id}` — xóa giao dịch + publish Kafka
- [ ] `GET /api/v1/transactions` — list by user (filter date/category/type, paginate)
- [ ] Hoàn thiện gRPC impl (`getTransactionById`, `getTransactionsByDateRange`, v.v.)

**1b. user-auth-service — gRPC Server**
- [ ] Tạo `UserGrpcServiceImpl.java` với `ValidateToken`, `GetUserById`, `GetFamilyMembers`
- [ ] Thêm Kafka publish `user.created` (để wallet tạo ví mặc định)

### ⚡ Bước 2: Hoàn Thiện Reporting (Ưu Tiên Trung)

- [ ] `GET /api/v1/reports/spending-by-category` (aggregate từ TransactionSummary)
- [ ] `GET /api/v1/reports/trends` (so sánh theo tháng)
- [ ] `GET /api/v1/reports/budget-comparison`
- [ ] Verify `TransactionGrpcClient` hoạt động thực sự

### ⚙️ Bước 3: Nâng Cấp Notification Service

- [ ] REST: `POST /receive`, `POST /fcm/register`, `GET /history`
- [ ] Entity + Repository (notification_db)
- [ ] FCM Firebase integration (hoặc giả lập đầy đủ hơn)
- [ ] Parser thông báo ngân hàng (regex pattern cho MB Bank, Momo, VCB)
- [ ] Kafka publish `notification.parsed`

### 🔮 Bước 4: Services Nâng Cao (Future)

- [ ] `category-service` — quyết định tách từ wallet-service hay build mới
- [ ] `ocr-service` — Tesseract / Google Vision
- [ ] `ai-service` — Speech-to-text, NLP, anomaly detection
- [ ] `config-service` — Spring Cloud Config Server

### 🔧 Bước 5: Kỹ Thuật & Chất Lượng

- [ ] Thống nhất API prefix: `/api/v1/` cho tất cả endpoints (hiện DashboardController dùng `/api/dashboard`)
- [ ] Xóa `TransactionController` trong wallet-service (trùng chức năng với transaction-service)
- [ ] Rate limiting tại API Gateway (Redis)
- [ ] Circuit breaker Resilience4j
- [ ] Swagger/OpenAPI export
