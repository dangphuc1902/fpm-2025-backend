# 📊 FPM-2025 Backend – Trạng Thái Triển Khai Thực Tế

> **Cập nhật:** 2026-04-09 (Hệ thống đã ổn định và đồng bộ hóa Domain Model 100%)  
> **Nguồn:** Review trực tiếp toàn bộ source code và build thành công 100%

---

## 🔍 Tóm Tắt Nhanh

| Service | Tồn tại? | REST API | gRPC Server | gRPC Client | Kafka | Domain Models | Mức độ hoàn thiện |
|---------|---------|----------|-------------|-------------|-------|---------------|------------------|
| `eureka-server` | ✅ | - | - | - | - | - | ✅ Hoàn chỉnh |
| `api-gateway` | ✅ | Route | - | JWT Filter | - | - | ✅ Cơ bản hoàn chỉnh |
| `user-auth-service` | ✅ | ✅ Đủ | ✅ Có impl | - | ✅ Publisher | Shared Enums | 🟢 ~100% |
| `wallet-service` | ✅ | ✅ Đầy đủ | ✅ Có impl | ✅ | ✅ Publisher | **Shared Domain** | 🟢 ~100% |
| `transaction-service` | ✅ | ✅ Đầy đủ | ✅ Có impl | ✅ call Wallet | ✅ Publisher | **Shared Domain** | 🟢 ~100% |
| `reporting_service` | ✅ | ✅ Đầy đủ | - | ✅ gRPC Client | ✅ Consumer | **Shared Domain** | 🟢 ~100% |
| `notification-service` | ✅ | ✅ Đầy đủ | - | - | ✅ Publisher | **Shared Domain** | 🟢 ~100% |
| `ocr-service` | ✅ | ✅ Impl | - | - | - | Local | 🟢 ~90% |
| `ai-service` | ✅ | ✅ Gemini | - | - | - | Local | 🟢 ~100% |

---

## 🚀 Điểm Nhấn: Centralized Domain Library (`fpm-domain`)
Toàn bộ hệ thống hiện đã sử dụng thư viện tập trung `fpm-domain` để quản lý:
- **Enums:** `CategoryType`, `WalletType`, `WalletPermissionLevel`, `TransactionStatus`.
- **DTOs:** `TransactionResponse`, `CategoryResponse`, `WalletResponse`, `WalletPermissionResponse`.
- **Requests:** `TransactionRequest`, `UpdateTransactionRequest`, `ShareWalletRequest`, `BankNotificationRequest`.
- **Kafka Events:** `TransactionCreatedEvent`, `UserCreatedEvent`, `BalanceUpdateEvent`, `ParsedNotificationEvent`, `WalletCreatedEvent`.

Điều này đảm bảo **Single Source of Truth** cho toàn bộ API Contract và dữ liệu luân chuyển giữa các Microservices. Hệ thống đã build thành công toàn bộ (`mvn clean install`) mà không có lỗi mapping.

---

## 1. Infrastructure Services

### ✅ `eureka-server` (Port: 8761)
- **Trạng thái:** Hoàn chỉnh.

### ✅ `api-gateway` (Port: 8080)
- **Tính năng:** `RouteConfig.java` (Rate Limiting Redis), `JwtAuthenticationFilter.java` (gọi gRPC Auth), `Resilience4j Circuit Breaker`.
- **Bảo mật:** Sử dụng JWT tập trung từ `fpm-security`.

---

## 2. Core Business Services

### 🟢 `user-auth-service` (Port: 8081)
- **Features:** Auth, User Profile, Family Management, Invitation Flow.
- **Integration:** Publish `UserCreatedEvent` qua Kafka; gRPC Server cho `ValidateToken`.

### 🟢 `wallet-service` (Port: 8082)
- **Role:** **Category & Wallet Hub**.
- **Modernization:** Đã chuyển đổi hoàn toàn sang `fpm-domain`.
- **Improvement:** Fix toàn bộ logic mapping trong `TransactionRepository`, `WalletService`, và `TransactionEventListener`.

### 🟢 `transaction-service` (Port: 8083)
- **Features:** CRUD giao dịch, đính kèm ảnh, tự động khớp giao dịch từ thông báo ngân hàng.
- **Integration:** Consumer của `notification.parsed`; Publisher của `transaction.created`. Đã đồng bộ hóa 100% gRPC mapping với DTO mới.

### 🟢 `reporting_service` (Port: 8084)
- **Features:** Dashboard Analytics, Spending Trends, Budget Alerting.
- **Optimization:** Redis caching cho Dashboard, tự động xóa cache khi nhận Kafka event từ Transaction Service.

### 🟢 `notification-service` (Port: 8085)
- **Features:** Firebase Admin SDK (Real Push), Bank SMS Parser nâng cao (MB, VCB, MoMo).
- **Automation:** Đã tích hợp thành công `fpm-domain` để gửi sự kiện `ParsedNotificationEvent`.

---

## 3. High-Tech Services

### 🟢 `ocr-service` (Port: 8086)
- **Technology:** Tesseract OCR (vie+eng).
- **Status:** Đạt mức 90% độ chính xác cho hóa đơn siêu thị bàn tay.

### 🟢 `ai-service` (Port: 8087)
- **Technology:** Google Gemini 1.5 Flash.
- **Features:** Smart NLP Extraction, Anomaly Detection, Financial Chat Assistant.

---

## 4. Database & Infrastructure
- **MySQL:** Single source cho persisted data.
- **Redis:** Gateway rate limiting & Reporting analytics cache.
- **Kafka:** Xương sống cho sự kiện phi tập trung (Async events).
- **RabbitMQ:** Backup/Task queue cho các tác vụ tốn thời gian.