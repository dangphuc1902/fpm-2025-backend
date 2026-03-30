# 📊 FPM-2025 Backend – Trạng Thái Triển Khai Thực Tế

> **Cập nhật:** 2026-03-27  
> **Nguồn:** Review trực tiếp source code trong thư mục `Backend/`

---

## 🔍 Tóm Tắt Nhanh

| Service | Tồn tại? | REST API | gRPC Server | gRPC Client | Kafka | RabbitMQ | Mức độ hoàn thiện |
|---------|---------|----------|-------------|-------------|-------|----------|------------------|
| `eureka-server` | ✅ | - | - | - | - | - | ✅ Hoàn chỉnh |
| `api-gateway` | ✅ | Route | - | JWT Filter | - | - | ✅ Cơ bản hoàn chỉnh |
| `user-auth-service` | ✅ | ✅ Đủ | ✅ Có impl | - | ✅ Publisher | - | 🟢 ~95% |
| `wallet-service` | ✅ | ✅ Đầy đủ+ | ✅ Có impl | ✅ | ✅ Publisher | ✅ Config | 🟢 ~90% |
| `transaction-service` | ✅ | ✅ Đủ CRUD | ✅ Có impl | ✅ Giá trị | ✅ Publisher | ✅ Gửi | 🟢 ~90% |
| `reporting-service` | ✅ | ✅ Cơ bản+ | - | ✅ gRPC Client | ✅ Consumer | ⚠️ | 🟢 ~85% |
| `notification-service` | ✅ | ✅ Có REST API | - | - | ✅ Publisher | ✅ Consumer | 🟢 ~90% |
| `category-service` | 🧩 Gộp vào wallet | - | - | - | - | - | ✅ Xong (Merged) |
| `ocr-service` | ✅ Tồn tại | ✅ Trích xuất | - | - | - | - | 🟢 ~50% (Stub) |
| `ai-service` | ✅ Tồn tại | ✅ NLP, Anomaly | - | - | - | - | 🟢 ~50% (Stub) |

---

## 1. Infrastructure Services

### ✅ `eureka-server` (Port: 8761)
- **Trạng thái:** Hoàn chỉnh.
- Đã khởi tạo, đăng ký service discovery cho toàn bộ hệ thống.

### ✅ `api-gateway` (Port: 8080)
- **Trạng thái:** Hoàn chỉnh cơ bản.
- **Có:** `RouteConfig.java`, `JwtAuthenticationFilter.java`, `LoggingFilter.java`, `CorsConfig.java`, `SecurityConfig.java`
- **Thiếu:** Rate limiting (Redis), Circuit breaker, config route `/api/families/**`, `/api/categories/**`, `/api/reports/**`

### ✅ `config-service` (Port: 8888)
- **Trạng thái:** Hoàn chỉnh (Mã nguồn nằm ở thư mục `config` riêng biệt tại root, thay vì trong `Backend/`).
- **Chi tiết:** Đã cấu hình và khởi tạo Spring Cloud Config Server.

---

## 2. Core Business Services

### 🟢 `user-auth-service` (Port: 8081) — ~95% Hoàn thiện

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
- ✅ **gRPC Server** (`ValidateToken`, `GetUserById`, `GetUsersByIds`, `GetFamilyMembers`) — đã được implement thành công trong `UserGrpcServiceImpl`.
- ✅ **Kafka publish** `user.created` event (khi register thành công) để wallet-service tạo ví mặc định.
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

### 🟢 `transaction-service` (Port: 8083) — ~90% Hoàn thiện

**REST APIs đã implement:**
| Endpoint | Trạng thái |
|----------|-----------|
| `POST /api/v1/transactions` | ✅ Done — có gRPC→Wallet, Kafka, RabbitMQ |
| `GET /api/v1/transactions/wallet/{walletId}` | ✅ Done (paged) |
| `GET /api/v1/transactions/{id}` | ✅ Done |
| `PUT /api/v1/transactions/{id}` | ✅ Done |
| `DELETE /api/v1/transactions/{id}` | ✅ Done |
| `GET /api/v1/transactions` (list by user) | ✅ Done |
| `GET /api/v1/transactions/search` | ❌ Chưa implement |
| `POST /api/v1/transactions/voice` | ❌ Chưa implement |
| `POST /api/v1/transactions/notification` | ❌ Chưa implement |
| `POST /api/v1/transactions/ocr` | ❌ Chưa implement |

**Entities có:** `TransactionEntity`, `RecurringTransactionEntity`, `TransactionAttachmentEntity`

**gRPC Server:** ✅ `TransactionServiceGrpcImpl` đã implement logic thực sự (`getTransactionById`, `getTransactionsByWallet`, `getTransactionsByDateRange`, `getTransactionsByUser`, `createTransaction`, `getTotalSpending`).

**Kafka:** ✅ Publish `transaction.created` trong `createTransaction()`

**RabbitMQ:** ✅ Gửi message tới `notification.exchange`

**Thiếu:**
- ❌ Kafka consume `notification.parsed`, `ocr.completed`, `category.assigned`
- ❌ Voice, Bank notification, OCR endpoints

---

### 🟢 `reporting-service` (Port: 8088) — ~85% Hoàn thiện

**REST APIs đã implement:**
| Endpoint             | Trạng Thái                                  |
| -------------------- | ------------------------------------------- |
| `GET /api/v1/dashboard` | ✅ Done (DashboardController + Redis cache) |
| `GET /api/v1/reports/monthly` | ✅ Done |
| `GET /api/v1/reports/export/pdf` | ✅ Done |
| `GET /api/v1/reports/export/excel` | ✅ Done |
| `GET /api/v1/reports/insights` | ⚠️ Stub — trả về "AI Insights upcoming!" |
| `GET /api/v1/reports/spending-by-category` | ✅ Done |
| `GET /api/v1/reports/trends` | ✅ Done |
| `GET /api/v1/reports/budget-comparison` | ✅ Done |
| `POST /api/v1/reports/export` | ❌ Chưa có async export job |
| `GET /api/v1/reports/export/{jobId}/download` | ❌ Chưa implement |

**Kafka Consumer:** ✅ `TransactionEventConsumer` — consume `transaction.created`, cập nhật `TransactionSummaryEntity`

**gRPC Client:** ✅ `TransactionGrpcClient` đã implement và hoạt động (gọi `getTransactionsByDateRange`, `getTransactionsByWallet`, `getTotalSpending`)

**Entities/Domain có:** `Budget`, `BudgetAlert`, `CategorySummary`, `ExportJob`, `MonthlySummary`, `TransactionSummaryEntity`

**Services có:** `BudgetService`, `DashboardService`, `ReportingService`, `StatisticsService`, `ReportGeneratorService`, `KafkaTransactionConsumer`

**Thiếu:**
- ❌ Khả năng query sâu vào chi tiết trend/AI analysis (đang stub).
- ❌ Async export job system hoàn chỉnh (đang xuất đồng bộ, export/{jobId}/download chưa có)
- ⚠️ RabbitMQ send `report.generate` chưa rõ

---

### 🟢 `notification-service` (Port: 8085) — ~90% Hoàn thiện

**Đã thực hiện:**
- ✅ **Listener:** Lắng nghe `notification.queue` từ RabbitMQ + Kafka consumers (`transaction.created`, `transaction.deleted`, `user.created`, `wallet.created`, `balance.changed`).
- ✅ **REST APIs:** `NotificationController` đầy đủ: `/receive`, `/fcm/register`, `/history`, `/unread-count`, `/{id}/read`, `/read-all`, `/fcm/status`.
- ✅ **Database & Entities:** `NotificationHistoryEntity`, `BankNotificationEntity`, `FcmTokenEntity` đầy đủ.
- ✅ **FCM Firebase Admin SDK 9.3.0:** `FcmPushService` với dual-mode (PRODUCTION khi có credentials, SIMULATION khi không). Hỗ trợ multicast, stale token cleanup, Android notification channel.
- ✅ **Bank Notification Parser sâu:** Regex patterns cho MB Bank (4 format SMS), VCB (4 format), MoMo (6 format: chuyển/nhận/thanh toán/rút/nạp/hoàn). Vietnamese amount parsing robust (1.500.000 vs 1,500.00). Parse balance, transactionRef, transactionTime.
- ✅ **Kafka Publisher:** Publish `notification.parsed` event với đầy đủ fields (notificationId, userId, bankName, amount, type, account, note, transactionRef, balance, transactionTime).

**Config classes:**
- `FirebaseConfig` — khởi tạo Firebase Admin SDK (classpath / filesystem credentials)
- `KafkaProducerConfig` — idempotent producer (acks=all, retries=3)

**Thiếu:**
- ⚠️ Unit tests chi tiết cho BankNotificationParser.
- ⚠️ Kafka consumer từ reporting-service chưa rõ.

---

### ✅ `category-service` (Đã gộp)

- **Trạng thái:** Không tạo service mới, sử dụng `wallet-service` (CategoryEntity, CategoryController) như một Category Hub để giảm độ trễ gọi mạng (network calls) và chi phí vận hành. Các service khác sẽ lưu `categoryId` tham chiếu hoặc dùng gRPC.

---

### 🟢 `ocr-service` (Port: 8086) — ~50% Hoàn thiện (Stub)

- **Trạng thái:** Đã khởi tạo cấu trúc Spring Boot, tích hợp thư viện `tess4j` (Tesseract) và cấu hình `pom.xml`, `application.yml`.
- **REST APIs:** `POST /api/v1/ocr/extract` để trích xuất hóa đơn. (Sử dụng cơ chế Stubbing/Fallback nếu máy chưa cài thư viện native).

---

### 🟢 `ai-service` (Port: 8087) — ~50% Hoàn thiện (Stub)

- **Trạng thái:** Đã khởi tạo cấu trúc, cấu hình.
- **REST APIs:**
  - `POST /api/v1/ai/nlp`: Phân tích câu văn (ví dụ "uống cafe 50k") để trích xuất số tiền, dự đoán danh mục.
  - `POST /api/v1/ai/anomaly`: Phát hiện giao dịch bất thường dựa trên từ khoá. (Sử dụng mock implementation thay thế cho model ML thực tế).

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
   - [x] Implement `PUT /api/v1/transactions/{id}` (update)
   - [x] Implement `DELETE /api/v1/transactions/{id}` (delete + publish event)
   - [x] Implement `GET /api/v1/transactions` (list by user, filter, paginate)
   - [x] Hoàn thiện gRPC `TransactionServiceGrpcImpl` (thay TODO bằng logic thực)

2. **Hoàn thiện user-auth gRPC Server**
   - [x] Tạo `UserGrpcServiceImpl.java` — implement `ValidateToken`, `GetUserById`
   - [ ] Kết nối API Gateway dùng gRPC thay vì REST validate

3. **Hoàn thiện `reporting-service`**
   - [x] `GET /api/v1/reports/spending-by-category`
   - [x] `GET /api/v1/reports/trends`
   - [x] Verify `TransactionGrpcClient` hoạt động thực sự

### ⚡ Ưu Tiên Trung Bình

4. **Nâng cấp `notification-service`** ✅ Hoàn thiện
   - [x] Thêm REST controller (receive, fcm/register, history, fcm/status)
   - [x] Thêm Database (entity + repository)
   - [x] Tích hợp FCM Firebase Admin SDK 9.3.0 (production/simulation dual-mode)
   - [x] Parser ngân hàng sâu (MB Bank, VCB, MoMo + generic)
   - [x] Kafka publish `notification.parsed`

5. **`category-service`** ✅ Đã quyết định giữ category logic trong `wallet-service`.

### 🔮 Ưu Tiên Thấp (Future)

6. **Hoàn thiện `ocr-service`** — Kết nối Tesseract/Google Vision thực tế với native library.
7. **Hoàn thiện `ai-service`** — Gọi Gemini/OpenAI API thật cho NLP và Anomaly detection.
8. ~~**`config-service`** module riêng~~ (Đã có sẵn tại folder `config` ở root)

---

## 5. Vấn Đề Kỹ Thuật Cần Chú Ý

1. ~~**API prefix không nhất quán**~~: Đã thống nhất dùng `/api/v1/` cho tất cả các controller.
2. ~~**wallet-service có TransactionController nội bộ**~~: Đã xóa, hiện chỉ sử dụng API từ `transaction-service`.
3. **gRPC stubs trong transaction-service** chưa connect với business logic
4. ~~**Kafka consumer trong notification-service** chưa implement~~ ✅ Đã có Kafka consumers + publisher
5. ~~**category-service** chưa tồn tại~~: Đã thống nhất dùng wallet-service làm Category Hub. Mọi API tham chiếu qua `categoryId`.

