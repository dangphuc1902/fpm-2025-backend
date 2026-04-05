# 📌 Chi Tiết Kiến Trúc Microservices – FPM-2025

> **Cập nhật:** 2026-04-04 (Đồng bộ thực tế triển khai)

Dự án **FPM-2025** (Family Personal Management) được xây dựng trên kiến trúc Microservices hiện đại, tối ưu cho khả năng mở rộng và hiệu năng cao.

---

## 1. Danh Sách Các Services & Phân Bổ Cổng (Ports)

| Service Name | Port (REST) | Port (gRPC) | Nhiệm Vụ Chính |
|--------------|-------------|-------------|----------------|
| **`eureka-server`** | 8761 | - | Service Discovery (Registry) |
| **`api-gateway`** | 8080 | - | Central Entry Point, Security, Routing |
| **`config-service`** | 8888 | - | Centralized Configuration Management |
| **`user-auth-service`** | 8081 | 9090 | Authentication (OAuth2/JWT), Family & User Management |
| **`wallet-service`** | 8082 | 9091 | Wallet CRUD, Shared Wallets, Category Hub (Manager) |
| **`transaction-service`**| 8083 | 9092 | Transaction Processing, Audit logs, Balance Sync |
| **`reporting_service`** | 8084 | - | Dashboard, Statistics, PDF/Excel Exports, Budgets |
| **`notification-service`**| 8085 | - | FCM Push, Bank SMS Parser (MB, VCB, MoMo) |
| **`ocr-service`** | 8086 | - | Receipt Processing (Tesseract OCR) |
| **`ai-service`** | 8087 | - | NLP (Intent Extraction), Anomaly Detection |

---

## 2. Công Nghệ Chủ Chốt (Core Stack)

- **Framework**: Spring Boot 3.5.5, Spring Cloud (Eureka, Gateway, Config).
- **Communication**: 
  - **REST**: Cho external client (Mobile) gọi qua Gateway.
  - **gRPC**: Cho internal call giữa các service (Transaction → Wallet, Gateway → Auth).
  - **Kafka**: Cho async event-driven (User Created → Default Wallet, Transaction Created → Reporting Summary).
- **Persistence**: PostgreSQL (Separate DB per service), Redis (Caching balance, stats, JWT blacklist).
- **Security**: JWT (HS256), Spring Security, OAuth2 Google.

---

## 3. Luồng Nghiệp Vụ Đặc Sắc (Unique Flows)

### 🧺 Category Hub (Tích hợp trong Wallet)
Thay vì tách riêng `category-service`, chúng tôi tích hợp danh mục vào `wallet-service` để giảm Network Latency. Các service khác truy cập danh mục qua gRPC hoặc lưu `categoryId` tham chiếu.

### 🏦 Bank Notification Parser (Notification Service)
Hệ thống có khả năng tự động bóc tách tin nhắn biến động số dư từ 3 kênh phổ biến nhất Việt Nam: MB Bank, Vietcombank, MoMo. Sau khi parse, hệ thống publish event lên Kafka để `transaction-service` tự động ghi chép chi tiêu mà không cần người dùng nhập tay.

### 🏛️ Bảo Mật & Hạ Tầng (Step 2 Completed)
Chương trình đã chuyển đổi 100% sang **MySQL** (Port 3306) để tối ưu hóa quản lý giao dịch. API Gateway đã được vũ trang hóa bằng **Redis Rate Limiting**, đảm bảo chống Spam/Brute-force hiệu quả.

---

## 4. Quản Lý Dữ Liệu (External Tools)

- **MySQL**: `localhost:3306` (Dockerized).
- **Redis**: `localhost:6379`.
- **Kafka**: `localhost:9092` (Zookeeper `2181`).
- **Kafdrop**: `localhost:9000` (Giao diện web quản lý Kafka topics).

---

## 5. Quy Định API
Tất cả các API REST phải tuân thủ prefix: `http://{gateway}:8080/api/v1/{service-route}/...`
Ví dụ: `GET http://localhost:8080/api/v1/wallets/active`
