# 03 — Services Catalog

> **Version:** 1.0 | **Updated:** 2026-05-15

---

## Table of Contents

1. [api-gateway](#1-api-gateway)
2. [user-auth-service](#2-user-auth-service)
3. [wallet-service](#3-wallet-service)
4. [transaction-service](#4-transaction-service)
5. [reporting-service](#5-reporting-service)
6. [notification-service](#6-notification-service)
7. [ocr-service & ai-service](#7-ocr-service--ai-service)

---

## 1. api-gateway

### Thông tin cơ bản

| Field | Value |
|-------|-------|
| **Container** | `fpm-api-gateway` |
| **Port** | `8080` (external) |
| **Base package** | `com.fpm_2025.api_gateway` |
| **Spring Boot** | Reactive WebFlux (KHÔNG dùng Servlet) |
| **DB** | Không có — stateless |

### Package Structure

```
api-gateway/
└── src/main/java/com/fpm_2025/api_gateway/
    ├── ApiGatewayApplication.java
    ├── config/
    │   ├── RouteConfig.java        ← Định nghĩa tất cả routes + CircuitBreaker
    │   └── SecurityConfig.java     ← JWT filter, permitAll paths
    ├── controller/
    │   └── FallbackController.java ← Trả lỗi khi CircuitBreaker OPEN
    └── filter/
        └── JwtAuthenticationFilter.java ← Validate JWT mọi request
```

### Nhiệm vụ

- **Route matching**: Điều hướng request đến đúng service theo path
- **JWT Validation**: Kiểm tra token trước khi forward
- **Rate Limiting**: Redis Token Bucket per user/IP
- **Circuit Breaker**: Resilience4j ngăn cascade failure
- **Load Balancing**: `lb://service-name` qua Eureka

### Không có REST API công khai

Gateway không expose endpoint nghiệp vụ — chỉ là proxy layer.

---

## 2. user-auth-service

### Thông tin cơ bản

| Field | Value |
|-------|-------|
| **Container** | `fpm-user-auth-service` |
| **REST Port** | `8081` |
| **gRPC Port** | `9090` |
| **DB** | `user_auth_db` (MySQL) |
| **Base package** | `com.fpm2025.user_auth_service` |

### Package Structure

```
user-auth-service/
└── src/main/java/com/fpm2025/user_auth_service/
    ├── controller/
    │   ├── AuthController.java           ← /api/v1/auth/**
    │   ├── UserController.java           ← /api/v1/users/**
    │   ├── FamilyController.java         ← /api/v1/families/**
    │   └── UserPreferencesController.java← /api/v1/users/preferences
    ├── service/
    │   └── AuthService.java              ← Business logic đăng ký/đăng nhập
    ├── entity/
    │   ├── UserEntity.java
    │   ├── FamilyEntity.java
    │   └── FamilyMemberEntity.java
    ├── repository/
    │   ├── UserRepository.java
    │   ├── FamilyRepository.java
    │   └── FamilyMemberRepository.java
    ├── grpc/
    │   └── UserGrpcServiceImpl.java      ← gRPC server (ValidateToken, GetUserById...)
    ├── security/
    │   └── JwtTokenProvider.java         ← Generate + validate JWT
    ├── filter/
    │   └── JwtAuthenticationFilter.java  ← Service-level JWT filter
    ├── config/
    │   └── SecurityConfig.java
    └── payload/
        ├── request/UserLoginRequest.java
        ├── request/UserRegisterRequest.java
        └── request/GoogleLoginRequest.java
```

### REST API Endpoints

| Method | Path | Mô tả | Auth |
|--------|------|-------|------|
| `POST` | `/api/v1/auth/register` | Đăng ký user mới | ❌ |
| `POST` | `/api/v1/auth/login` | Đăng nhập email/password | ❌ |
| `POST` | `/api/v1/auth/google` | Đăng nhập Google OAuth2 | ❌ |
| `POST` | `/api/v1/auth/validate` | Validate JWT token | ❌ |
| `POST` | `/api/v1/auth/logout` | Logout — blacklist token vào Redis | ✅ |
| `POST` | `/api/v1/auth/refresh` | Refresh access token | ✅ |
| `GET` | `/api/v1/users/me` | Lấy thông tin user hiện tại | ✅ |
| `PUT` | `/api/v1/users/me` | Cập nhật profile | ✅ |
| `GET` | `/api/v1/families` | Danh sách gia đình | ✅ |
| `POST` | `/api/v1/families` | Tạo family group | ✅ |
| `POST` | `/api/v1/families/{id}/members` | Thêm thành viên | ✅ |
| `GET` | `/api/v1/users/preferences` | Lấy preferences (language, currency) | ✅ |
| `PUT` | `/api/v1/users/preferences` | Cập nhật preferences | ✅ |

### gRPC API (Server — port 9090)

| RPC | Input | Output | Ai gọi |
|-----|-------|--------|--------|
| `ValidateToken` | `TokenRequest` | `TokenValidationResponse` | API Gateway |
| `GetUserById` | `UserIdRequest` | `UserResponse` | wallet-service |
| `GetUsersByIds` | `UserIdsRequest` | `UsersResponse` | reporting-service |
| `GetFamilyMembers` | `FamilyIdRequest` | `FamilyMembersResponse` | reporting-service |
| `CheckUserExists` | `UserIdRequest` | `UserExistsResponse` | wallet-service |

### Login Flow

```
POST /api/v1/auth/login
  → Gateway: rate limit check (5/5min per IP)
  → AuthService.login()
    → userRepository.findByEmail()
    → BCrypt.verify(rawPassword, hashedPassword)
    → jwtTokenProvider.generateToken(userId, email, role)
    → Redis: SETEX refresh:token:{userId} 604800 {refreshToken}
  → Response: {accessToken, refreshToken, user}
```

### Kafka Events Published

| Topic | Trigger |
|-------|---------|
| `user.created` | Sau khi đăng ký thành công |

---

## 3. wallet-service

### Thông tin cơ bản

| Field | Value |
|-------|-------|
| **Container** | `fpm-wallet-service` |
| **REST Port** | `8082` |
| **DB** | `wallet_db` (MySQL) |
| **Base package** | `com.fpm_2025.wallet_service` |

### Package Structure

```
wallet-service/
└── src/main/java/com/fpm_2025/wallet_service/
    ├── controller/
    │   ├── WalletController.java      ← /api/v1/wallets/**
    │   └── CategoryController.java    ← /api/v1/categories/**
    ├── service/
    │   ├── WalletService.java
    │   └── CategoryService.java
    ├── entity/
    │   ├── WalletEntity.java          ← @Where(is_deleted=false) Soft delete
    │   ├── CategoryEntity.java
    │   ├── WalletPermissionEntity.java← Shared wallet permissions
    │   └── BudgetEntity.java
    ├── repository/
    │   ├── WalletRepository.java
    │   ├── CategoryRepository.java
    │   └── WalletPermissionRepository.java
    ├── grpc/
    │   ├── WalletGrpcServiceImpl.java ← gRPC server (UpdateBalance, CheckBalance...)
    │   └── client/UserGrpcClient.java ← gRPC client → user-auth-service
    ├── messaging/
    │   └── UserCreatedListener.java   ← Kafka consumer: user.created
    ├── event/
    │   └── WalletEventPublisher.java  ← Publish wallet.created, balance.changed
    ├── dto/
    │   └── payload/request/
    │       ├── CreateWalletRequest.java
    │       └── UpdateWalletRequest.java
    └── config/
        └── KafkaConsumerConfig.java   ← 2 listener factories (String + UserCreatedEvent)
```

### REST API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/wallets` | Tạo ví mới |
| `GET` | `/api/v1/wallets` | Danh sách tất cả ví của user |
| `GET` | `/api/v1/wallets/active` | Chỉ ví đang hoạt động |
| `GET` | `/api/v1/wallets/type/{type}` | Lọc theo loại (CASH, CARD, BANK) |
| `GET` | `/api/v1/wallets/shared` | Ví được chia sẻ với user |
| `GET` | `/api/v1/wallets/{id}` | Chi tiết một ví |
| `PUT` | `/api/v1/wallets/{id}` | Cập nhật ví |
| `PATCH` | `/api/v1/wallets/{id}/toggle` | Bật/tắt ví |
| `DELETE` | `/api/v1/wallets/{id}` | Xóa ví (soft delete) |
| `GET` | `/api/v1/wallets/total-balance` | Tổng số dư tất cả ví |
| `GET` | `/api/v1/wallets/count` | Số lượng ví |
| `POST` | `/api/v1/wallets/{id}/share` | Chia sẻ ví với user khác |
| `GET` | `/api/v1/wallets/{id}/shares` | Danh sách user có quyền truy cập |
| `DELETE` | `/api/v1/wallets/{id}/share/{uid}` | Thu hồi quyền truy cập |
| `GET` | `/api/v1/wallets/family/{familyId}` | Ví của cả family |
| `GET` | `/api/v1/categories` | Danh sách danh mục |
| `POST` | `/api/v1/categories` | Tạo danh mục mới |
| `PUT` | `/api/v1/categories/{id}` | Cập nhật danh mục |
| `DELETE` | `/api/v1/categories/{id}` | Xóa danh mục |

### gRPC API (Server)

| RPC | Mô tả |
|-----|-------|
| `GetWalletById` | Lấy chi tiết ví theo ID |
| `GetWalletsByUserId` | Lấy tất cả ví active của user |
| `UpdateBalance` | ADD/SUBTRACT/SET số dư |
| `ValidateWalletAccess` | Kiểm tra quyền (OWNER/WRITE/READ) |
| `CheckSufficientBalance` | Kiểm tra đủ tiền không |

### Kafka

| Direction | Topic | Trigger/Action |
|-----------|-------|----------------|
| **Consume** | `user.created` | Tạo default wallet cho user mới |
| **Publish** | `wallet.created` | Sau khi tạo ví |
| **Publish** | `balance.changed` | Sau khi số dư thay đổi |

---

## 4. transaction-service

### Thông tin cơ bản

| Field | Value |
|-------|-------|
| **Container** | `fpm-transaction-service` |
| **REST Port** | `8083` |
| **gRPC Port** | `9093` |
| **DB** | `transaction_db` (MySQL) |
| **Base package** | `com.fpm2025.transaction_service` |

### Package Structure

```
transaction-service/
└── src/main/java/com/fpm2025/transaction_service/
    ├── controller/
    │   └── TransactionController.java  ← /api/v1/transactions/**
    ├── service/
    │   └── TransactionService.java
    ├── entity/
    │   └── TransactionEntity.java
    ├── repository/
    │   └── TransactionRepository.java  ← JPA Specifications for dynamic query
    ├── grpc/
    │   ├── TransactionGrpcServiceImpl.java ← gRPC server
    │   └── client/ ← gRPC stubs để gọi wallet-service
    ├── event/
    │   ├── producer/TransactionEventPublisher.java
    │   └── consumer/ParsedNotificationConsumer.java ← Kafka: notification.parsed
    ├── messaging/
    │   └── RabbitMQ handlers
    └── config/
        └── GrpcClientConfig.java
```

### REST API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/transactions` | Tạo giao dịch mới |
| `GET` | `/api/v1/transactions` | Danh sách giao dịch (có filter + pagination) |
| `GET` | `/api/v1/transactions/{id}` | Chi tiết giao dịch |
| `PUT` | `/api/v1/transactions/{id}` | Cập nhật giao dịch |
| `DELETE` | `/api/v1/transactions/{id}` | Xóa giao dịch |
| `POST` | `/api/v1/transactions/notification` | Xử lý thông báo ngân hàng thủ công |

**Query Parameters cho GET /transactions:**
```
walletId, categoryId, type (INCOME/EXPENSE),
startDate, endDate (ISO_DATE_TIME),
page (default 0), size (default 20)
```

### Create Transaction Flow

```
POST /api/v1/transactions
  → TransactionService.createTransaction(userId, request)
    1. gRPC: wallet-service.CheckSufficientBalance()   ← BR-TXN-02
    2. INSERT INTO transactions
    3. gRPC: wallet-service.UpdateBalance(ADD/SUBTRACT)
    4. Kafka: publish "transaction.created"
    5. RabbitMQ: publish domain event
  → Response: TransactionResponse
```

### Kafka

| Direction | Topic | Trigger/Action |
|-----------|-------|----------------|
| **Publish** | `transaction.created` | Sau khi tạo giao dịch |
| **Publish** | `transaction.updated` | Sau khi sửa giao dịch |
| **Publish** | `transaction.deleted` | Sau khi xóa giao dịch |
| **Consume** | `notification.parsed` | Auto-create transaction từ OCR result |

---

## 5. reporting-service

### Thông tin cơ bản

| Field | Value |
|-------|-------|
| **Container** | `fpm-reporting-service` |
| **REST Port** | `8084` |
| **DB** | `reporting_db` (MySQL) |
| **Base package** | `com.fpm_2025.reportingservice` |
| **Đặc điểm** | DDD structure — có `domain/` package riêng |

### Package Structure

```
reporting-service/
└── src/main/java/com/fpm_2025/reportingservice/
    ├── controller/
    │   ├── ReportController.java     ← /api/v1/reports/**
    │   ├── BudgetController.java     ← /api/v1/budgets/**
    │   └── DashboardController.java  ← /api/v1/dashboard/**
    ├── domain/                       ← DDD Domain Layer
    │   ├── model/
    │   │   ├── MonthlyReport.java
    │   │   └── ExportJob.java
    │   ├── valueobject/
    │   │   └── ExportFormat.java     ← PDF, EXCEL, CSV
    │   └── repository/ (interfaces)
    ├── service/
    │   ├── ReportingService.java
    │   ├── BudgetService.java
    │   └── KafkaTransactionConsumer.java
    ├── event/
    │   └── consumer/
    │       └── TransactionEventConsumer.java ← Lắng nghe transaction.*
    ├── entity/
    │   ├── MonthlySummaryEntity.java
    │   ├── CategorySummaryEntity.java
    │   └── ExportJobEntity.java
    ├── repository/
    └── security/
        └── UserPrincipal.java        ← Custom principal từ JWT header
```

### REST API Endpoints

**Reports:**

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/reports/monthly?month=yyyy-MM` | Báo cáo tháng |
| `GET` | `/api/v1/reports/export/pdf?month=yyyy-MM` | Tải PDF |
| `GET` | `/api/v1/reports/export/excel?month=yyyy-MM` | Tải Excel |
| `GET` | `/api/v1/reports/spending-by-category?yearMonth=` | Pie chart data |
| `GET` | `/api/v1/reports/trends?months=6` | Line chart income vs expense |
| `GET` | `/api/v1/reports/budget-comparison?yearMonth=` | Budget vs actual |
| `POST` | `/api/v1/reports/export` | Async export job |
| `GET` | `/api/v1/reports/export/{jobId}` | Kiểm tra trạng thái export |
| `GET` | `/api/v1/reports/export/{jobId}/download` | Download kết quả |
| `GET` | `/api/v1/reports/insights` | AI insights (placeholder) |

**Budgets:**

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/budgets` | Tạo ngân sách |
| `GET` | `/api/v1/budgets` | Danh sách ngân sách |
| `PUT` | `/api/v1/budgets/{id}` | Cập nhật ngân sách |
| `DELETE` | `/api/v1/budgets/{id}` | Xóa ngân sách |

**Dashboard:**

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/dashboard` | Tổng quan tài chính (cached 5 phút) |

### Kafka Consumers

| Topic | Consumer | Action |
|-------|----------|--------|
| `transaction.created` | `TransactionEventConsumer` | Cập nhật `monthly_summaries`, `category_summaries` |
| `transaction.updated` | `TransactionEventConsumer` | Recalculate summaries |
| `transaction.deleted` | `TransactionEventConsumer` | Trừ khỏi summaries |

> 💡 **CQRS Lite:** Reporting không query DB của transaction trực tiếp. Nó duy trì bản sao dữ liệu riêng (`reporting_db`) được cập nhật qua Kafka events — đây là Write side (Kafka consumer) tách khỏi Read side (REST API).

---

## 6. notification-service

### Thông tin cơ bản

| Field | Value |
|-------|-------|
| **Container** | `fpm-notification-service` |
| **REST Port** | `8085` |
| **DB** | `notification_db` (MySQL) |
| **Base package** | `com.fpm2025.notification_service` |

### Package Structure

```
notification-service/
└── src/main/java/com/fpm2025/notification_service/
    ├── controller/
    │   └── NotificationController.java  ← /api/v1/notifications/**
    ├── service/
    │   └── NotificationService.java     ← Firebase FCM, Email
    ├── listener/
    │   └── NotificationListener.java    ← Kafka consumers cho 5 topics
    ├── entity/
    │   ├── NotificationEntity.java
    │   └── NotificationTemplateEntity.java
    ├── repository/
    └── config/
        └── FirebaseConfig.java
```

### Kafka Consumers

| Topic | Group | Action |
|-------|-------|--------|
| `transaction.created` | `notification-service` | Push: "Giao dịch mới: -500,000 VND" |
| `transaction.deleted` | `notification-service` | Push: "Giao dịch đã xóa" |
| `user.created` | `notification-service` | Email: "Chào mừng đến FPM!" |
| `wallet.created` | `notification-service` | Push: "Ví mới đã tạo" |
| `balance.changed` | `notification-service` | Push: "Số dư thay đổi" |
| `budget.alerts` | `notification-service` | Push: "Cảnh báo ngân sách!" |

### REST API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/notifications` | Danh sách thông báo của user |
| `PATCH` | `/api/v1/notifications/{id}/read` | Đánh dấu đã đọc |
| `PATCH` | `/api/v1/notifications/read-all` | Đánh dấu tất cả đã đọc |

---

## 7. ocr-service & ai-service

### ocr-service

| Field | Value |
|-------|-------|
| **Port** | `8086` |
| **DB** | Không có |
| **Chức năng** | Nhận ảnh hóa đơn → OCR → parse ra amount, bank, type |

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/ocr/scan` | Upload ảnh, trả về `ParsedNotificationEvent` |

**Flow sau OCR:**
```
OCR result → notification-service → Kafka: notification.parsed → transaction-service (auto-create)
```

### ai-service

| Field | Value |
|-------|-------|
| **Port** | `8087` |
| **DB** | Không có |
| **Chức năng** | Gợi ý danh mục, phân tích chi tiêu thông minh |

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/ai/suggest-category` | Gợi ý category từ mô tả giao dịch |
| `GET` | `/api/v1/ai/insights` | Phân tích xu hướng tài chính |

---

## Appendix: Port & gRPC Summary

| Service | REST Port | gRPC Port | DB |
|---------|-----------|-----------|-----|
| api-gateway | 8080 | — | — |
| user-auth-service | 8081 | **9090** | user_auth_db |
| wallet-service | 8082 | — | wallet_db |
| transaction-service | 8083 | **9093** | transaction_db |
| reporting-service | 8084 | — | reporting_db |
| notification-service | 8085 | — | notification_db |
| ocr-service | 8086 | — | — |
| ai-service | 8087 | — | — |
| eureka-server | 8761 | — | — |
| config-server | 8888 | — | — |

---

> 📌 **Tiếp theo:** Xem `04_SHARED_LIBS.md` để hiểu chi tiết 9 module thư viện nội bộ `fpm-libs`.
