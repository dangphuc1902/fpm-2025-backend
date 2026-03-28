# 📊 FPM-2025 Backend – Trạng Thái Triển Khai Thực Tế

> **Cập nhật:** 2026-03-27  
> **Nguồn:** Review trực tiếp source code trong thư mục `Backend/`

---

## 🔍 Tóm Tắt Nhanh

| Service | Tồn tại? | REST API | gRPC Server | gRPC Client | Kafka | RabbitMQ | Mức độ hoàn thiện |
|---------|---------|----------|-------------|-------------|-------|----------|------------------|
| `eureka-server` | ✅ | - | - | - | - | - | ✅ Hoàn chỉnh |
| `api-gateway` | ✅ | Route | - | JWT Filter | - | - | ✅ Cơ bản hoàn chỉnh |
| `user-auth-service` | ✅ | ✅ Đủ | ❌ Chưa có impl | - | ❌ | - | 🟡 ~75% |
| `wallet-service` | ✅ | ✅ Đầy đủ+ | ✅ Có impl | ✅ | ✅ Publisher | ✅ Config | 🟢 ~90% |
| `transaction-service` | ✅ | ⚠️ Thiếu 5 API | ✅ Stub only | ✅ gRPC→Wallet | ✅ Publisher | ✅ Gửi | 🟡 ~60% |
| `reporting-service` | ✅ | ✅ Cơ bản | - | ✅ gRPC Client | ✅ Consumer | ⚠️ | 🟡 ~65% |
| `notification-service` | ✅ | ❌ Chỉ Listener | - | - | ❌ | ✅ Consumer | 🔴 ~15% |
| `category-service` | ❌ Không tồn tại | - | - | - | - | - | 🔴 0% |
| `ocr-service` | ❌ Không tồn tại | - | - | - | - | - | 🔴 0% |
| `ai-service` | ❌ Không tồn tại | - | - | - | - | - | 🔴 0% |

---

## 1. Infrastructure Services

### ✅ `eureka-server` (Port: 8761)
- **Trạng thái:** Hoàn chỉnh.
- Đã khởi tạo, đăng ký service discovery cho toàn bộ hệ thống.

### ✅ `api-gateway` (Port: 8080)
- **Trạng thái:** Hoàn chỉnh cơ bản.
- **Có:** `RouteConfig.java`, `JwtAuthenticationFilter.java`, `LoggingFilter.java`, `CorsConfig.java`, `SecurityConfig.java`
- **Thiếu:** Rate limiting (Redis), Circuit breaker, config route `/api/families/**`, `/api/categories/**`, `/api/reports/**`

### ⚠️ `config-service` (Port: 8888)
- **Trạng thái:** **Không thấy module** trong thư mục Backend. Config đang dùng trực tiếp trong từng service.
- **Cần làm:** Tạo module `config-service` hoặc xác nhận dùng embedded config.

---

## 2. Core Business Services

### 🟡 `user-auth-service` (Port: 8081) — ~75% Hoàn thiện

**REST APIs đã implement:**
| Endpoint | Trạng thái |
|----------|-----------|
| `POST /api/v1/auth/register` | ✅ Done |
| `POST /api/v1/auth/login` + Rate limiting | ✅ Done |
| `POST /api/v1/auth/google` | ✅ Done |
| `POST /api/v1/auth/validate` | ✅ Done |
| `POST /api/v1/auth/logout` | ✅ Done (JWT Blacklist Redis) |
| `POST /api/v1/auth/refresh` | ✅ Done (RefreshTokenService) |
| `GET /api/v1/users/me` | ✅ Done (UserController) |
| `PUT /api/v1/users/me` | ✅ Done |
| `POST /api/v1/families` | ✅ Done |
| `GET /api/v1/families` | ✅ Done |
| `GET /api/v1/families/{id}/members` | ✅ Done |
| `POST /api/v1/families/{id}/invite` | ✅ Done |

**Entities có:** `UserEntity`, `FamilyEntity`, `FamilyMemberEntity`, `RefreshToken`, `UserPreferencesEntity`, `FamilyRole`

**Thiếu / Chưa hoàn thiện:**
- ❌ **gRPC Server** (`ValidateToken`, `GetUserById`, `GetFamilyMembers`) — file impl chưa tìm thấy trong source
- ❌ **Kafka publish** `user.created` event (để wallet-service tạo ví mặc định)
- ⚠️ OAuth2 Google flow cần kiểm tra lại `CustomOAuth2UserService.java`

---

### 🟢 `wallet-service` (Port: 8082) — ~90% Hoàn thiện

**REST APIs đã implement (đầy đủ và mở rộng):**
| Endpoint | Trạng thái |
|----------|-----------|
| `POST /api/v1/wallets` | ✅ Done |
| `GET /api/v1/wallets` | ✅ Done |
| `GET /api/v1/wallets/active` | ✅ Done (bonus) |
| `GET /api/v1/wallets/type/{type}` | ✅ Done (bonus) |
| `GET /api/v1/wallets/shared` | ✅ Done |
| `GET /api/v1/wallets/{id}` | ✅ Done |
| `PATCH /api/v1/wallets/{id}/toggle` | ✅ Done (bonus) |
| `PUT /api/v1/wallets/{id}` | ✅ Done |
| `DELETE /api/v1/wallets/{id}` | ✅ Done |
| `GET /api/v1/wallets/total-balance` | ✅ Done (bonus) |
| `GET /api/v1/wallets/count` | ✅ Done (bonus) |
| `POST /api/v1/wallets/{id}/share` | ✅ Done |
| `GET /api/v1/wallets/{id}/shares` | ✅ Done |
| `DELETE /api/v1/wallets/{id}/share/{uid}` | ✅ Done |

**Entities có:** `WalletEntity`, `WalletPermissionEntity`, `CategoryEntity`, `TransactionEntity` (local)

**gRPC Server:** ✅ `WalletServiceGrpcImpl` implement đủ các RPC: `GetWalletById`, `GetWalletsByUserId`, `UpdateBalance`, `ValidateWalletAccess`, `CheckSufficientBalance`

**Kafka:** ✅ `WalletEventPublisher` — publish `wallet.created`, `wallet.updated`, `balance.changed`

**Thiếu:**
- ⚠️ Kafka consumer `user.created` chưa rõ (xem `TransactionEventListener.java`)
- ⚠️ Redis caching chi tiết (RedisConfig có nhưng cần verify)
- ❌ API `GET /api/v1/wallets/family/{familyId}` (theo spec)

---

### 🟡 `transaction-service` (Port: 8083) — ~60% Hoàn thiện

**REST APIs đã implement:**
| Endpoint | Trạng thái |
|----------|-----------|
| `POST /api/v1/transactions` | ✅ Done — có gRPC→Wallet, Kafka, RabbitMQ |
| `GET /api/v1/transactions/wallet/{walletId}` | ✅ Done (paged) |
| `GET /api/v1/transactions/{id}` | ✅ Done |
| `PUT /api/v1/transactions/{id}` | ❌ Chưa implement |
| `DELETE /api/v1/transactions/{id}` | ❌ Chưa implement |
| `GET /api/v1/transactions` (list by user) | ❌ Chưa implement |
| `GET /api/v1/transactions/search` | ❌ Chưa implement |
| `POST /api/v1/transactions/voice` | ❌ Chưa implement |
| `POST /api/v1/transactions/notification` | ❌ Chưa implement |
| `POST /api/v1/transactions/ocr` | ❌ Chưa implement |

**Entities có:** `TransactionEntity`, `RecurringTransactionEntity`, `TransactionAttachmentEntity`

**gRPC Server:** ⚠️ `TransactionServiceGrpcImpl` đã khai báo đủ method nhưng **tất cả đều là TODO stub** — chưa có logic thực sự

**Kafka:** ✅ Publish `transaction.created` trong `createTransaction()`

**RabbitMQ:** ✅ Gửi message tới `notification.exchange`

**Thiếu:**
- ❌ PUT/DELETE Transaction endpoints
- ❌ GET list transactions by user (filter, search, paginate)
- ❌ gRPC Server impl thực sự (chưa gọi `transactionService` trong các method)
- ❌ Kafka consume `notification.parsed`, `ocr.completed`, `category.assigned`
- ❌ Voice, Bank notification, OCR endpoints

---

### 🟡 `reporting-service` (Port: 8088) — ~65% Hoàn thiện

**REST APIs đã implement:**
| Endpoint | Trạng thái |
|----------|-----------|
| `GET /api/dashboard` | ✅ Done (DashboardController + Redis cache) |
| `GET /api/v1/reports/monthly` | ✅ Done |
| `GET /api/v1/reports/export/pdf` | ✅ Done |
| `GET /api/v1/reports/export/excel` | ✅ Done |
| `GET /api/v1/reports/insights` | ⚠️ Stub — trả về "AI Insights upcoming!" |
| `GET /api/v1/reports/spending-by-category` | ❌ Chưa implement |
| `GET /api/v1/reports/trends` | ❌ Chưa implement |
| `GET /api/v1/reports/budget-comparison` | ❌ Chưa implement |
| `POST /api/v1/reports/export` | ❌ Chưa có async export job |
| `GET /api/v1/reports/export/{jobId}/download` | ❌ Chưa implement |

**Kafka Consumer:** ✅ `TransactionEventConsumer` — consume `transaction.created`, cập nhật `TransactionSummaryEntity`

**gRPC Client:** ✅ `TransactionGrpcClient` có khai báo (cần verify logic thực sự)

**Entities/Domain có:** `Budget`, `BudgetAlert`, `CategorySummary`, `ExportJob`, `MonthlySummary`, `TransactionSummaryEntity`

**Services có:** `BudgetService`, `DashboardService`, `ReportingService`, `StatisticsService`, `ReportGeneratorService`, `KafkaTransactionConsumer`

**Thiếu:**
- ❌ Spending by category chart endpoint
- ❌ Trends endpoint
- ❌ Budget comparison endpoint
- ❌ Async export job system hoàn chỉnh
- ⚠️ RabbitMQ send `report.generate` chưa rõ

---

### 🔴 `notification-service` (Port: 8085) — ~15% Hoàn thiện

**Có:**
- `NotificationListener.java` — lắng nghe `notification.queue` từ RabbitMQ → **chỉ log ra console** (giả lập gửi email)

**Thiếu hoàn toàn:**
- ❌ REST APIs: `/api/notifications/receive`, `/api/notifications/fcm/register`, `/api/notifications/history`
- ❌ Database (`notification_db`) – không có entity/repository
- ❌ FCM Push Notification (firebase-admin)
- ❌ Parser thông báo ngân hàng (MB Bank, Momo, VCB, TCB)
- ❌ Kafka publish `notification.parsed`
- ❌ Deduplication logic

---

### 🔴 `category-service` (Port: 8084) — 0% (Không tồn tại)

Theo thiết kế trong `microservice_details_fucon.md`, service này cần:
- REST APIs: CRUD category, budget management
- gRPC Server: `GetCategoryById`, `AutoCategorize`, `GetCategoriesByFamily`, `CheckBudgetStatus`
- Kafka: consume `transaction.created`, publish `category.assigned`
- RabbitMQ: consume `ai.classification.result`

**→ Module chưa được tạo.**

> ⚠️ **Lưu ý:** `wallet-service` đang có `CategoryEntity` và `CategoryController` nội bộ. Cần quyết định: tách ra `category-service` riêng hay giữ luôn trong `wallet-service`.

---

### 🔴 `ocr-service` (Port: 8086) — 0% (Không tồn tại)

**→ Module chưa được tạo.**

---

### 🔴 `ai-service` (Port: 8087) — 0% (Không tồn tại)

**→ Module chưa được tạo.**

---

## 3. Nhận Xét Về Tài Liệu Hiện Có

| File | Tình trạng | Vấn đề |
|------|-----------|--------|
| `microservice_details_fucon.md` | ⚠️ Cần cập nhật | Port reporting-service sai (doc ghi 8088, task_role ghi 8084), category-service và ocr-service chưa tồn tại trong thực tế |
| `document_api.md` | ⚠️ Cần bổ sung | API prefix sai (`/api/v1/` vs `/api/`), nhiều API đánh dấu "Done" nhưng thiếu endpoints thực tế |
| `Project_Status_Roadmap.md` | 🔴 Lỗi thời | Trạng thái cũ, đã thay đổi nhiều. transaction-service và notification-service đã được tạo |
| `task_role.md` | ⚠️ Phần kết luận sai | "Đã hoàn thiện 100% MVP" — KHÔNG ĐÚNG với thực tế hiện tại |
| `Communication_Architecture.md` | ✅ Tốt | Mô tả kiến trúc còn hợp lệ |
| `Grpc_Usage_Review.md` | ✅ Tốt | Mô tả contract gRPC đúng |
| `Client_Backend_API_Contract.md` | ✅ Tốt | Contract family API đúng |
| `Client_Backend_API_Contract_Shared_Wallet.md` | ✅ Tốt | Contract shared wallet đúng, đã implement |

---

## 4. Roadmap Ưu Tiên Tiếp Theo

### 🚨 Ưu Tiên Cao (Core hoạt động được)

1. **Hoàn thiện `transaction-service`**
   - [ ] Implement `PUT /api/v1/transactions/{id}` (update)
   - [ ] Implement `DELETE /api/v1/transactions/{id}` (delete + publish event)
   - [ ] Implement `GET /api/v1/transactions` (list by user, filter, paginate)
   - [ ] Hoàn thiện gRPC `TransactionServiceGrpcImpl` (thay TODO bằng logic thực)

2. **Hoàn thiện user-auth gRPC Server**
   - [ ] Tạo `UserGrpcServiceImpl.java` — implement `ValidateToken`, `GetUserById`
   - [ ] Kết nối API Gateway dùng gRPC thay vì REST validate

3. **Hoàn thiện `reporting-service`**
   - [ ] `GET /api/v1/reports/spending-by-category`
   - [ ] `GET /api/v1/reports/trends`
   - [ ] Verify `TransactionGrpcClient` hoạt động thực sự

### ⚡ Ưu Tiên Trung Bình

4. **Nâng cấp `notification-service`**
   - [ ] Thêm REST controller (receive, fcm/register, history)
   - [ ] Thêm Database (entity + repository)
   - [ ] Tích hợp FCM Firebase (hoặc giử simulation)

5. **Tạo `category-service`** (hoặc quyết định giữ category trong wallet-service)
   - [ ] Nếu tách: tạo module, migrate category logic từ wallet-service
   - [ ] gRPC Server cho `AutoCategorize`

### 🔮 Ưu Tiên Thấp (Future)

6. **`ocr-service`** — Tích hợp Tesseract/Google Vision
7. **`ai-service`** — NLP, Speech-to-Text, Anomaly detection
8. **`config-service`** module riêng

---

## 5. Vấn Đề Kỹ Thuật Cần Chú Ý

1. **API prefix không nhất quán**: Một số endpoint dùng `/api/v1/`, số khác dùng `/api/` (DashboardController dùng `/api/dashboard`)
2. **wallet-service có TransactionController nội bộ** — có thể conflict với `transaction-service`
3. **gRPC stubs trong transaction-service** chưa connect với business logic
4. **Kafka consumer trong notification-service** chưa implement (chỉ có RabbitMQ)
5. **category-service** chưa tồn tại nhưng được cần bởi reporting-service và transaction-service

