# 📂 Chi Tiết Cấu Trúc Mã Nguồn Monolith (FPM-2025)

Tài liệu này cung cấp sơ đồ cây thư mục chi tiết và mô tả chức năng của từng file/thư mục trong project `fpm-monolith` sau khi đã hoàn thành tái cấu trúc và đồng nhất cấu trúc gói.

---

## 1. Cấu Trúc Gói Lớn (Base Packages)

Toàn bộ hệ thống hiện đã được gom gọn và chuẩn hóa về **một gói cơ sở duy nhất**:
*   **`com.fpm2025`**: Chứa toàn bộ các service nghiệp vụ cốt lõi, bao gồm cả hai service DDD-based vừa được di chuyển và chuẩn hóa:
    *   `com.fpm2025.wallet_service` (đã gộp từ gói cũ `com.fpm_2025.wallet_service`).
    *   `com.fpm2025.reporting_service` (đã gộp và đổi tên từ gói cũ `com.fpm_2025.reportingservice`).

Mô hình này giúp triệt tiêu hoàn toàn sự phân mảnh package và đảm bảo cấu trúc mã nguồn đạt mức độ đồng bộ cao nhất.

---

## 2. Sơ Đồ Cây Thư Mục Chi Tiết (Source Tree)

```
fpm-monolith/
├── src/main/java/
│   └── com/
│       └── fpm2025/
│           ├── monolith/                         ← ENTRYPOINT & GLOBAL CONFIG
│           │   ├── MonolithApplication.java      ← Khởi chạy App, ComponentScan toàn bộ
│           │   └── config/
│           │       └── MonolithSecurityConfig.java ← Cấu hình bảo mật JWT & PasswordEncoder tập trung
│           │
│           ├── user_auth_service/                ← USER PROFILE & FAMILY MANAGEMENT
│           │   ├── controller/
│           │   │   ├── AuthController.java       ← REST: Đăng ký, đăng nhập JWT, Google
│           │   │   ├── UserController.java       ← REST: Cập nhật thông tin cá nhân
│           │   │   ├── FamilyController.java     ← REST: Quản lý nhóm và lời mời gia đình
│           │   │   └── UserPreferencesController.java ← REST: Tùy chỉnh tiền tệ, ngôn ngữ
│           │   ├── entity/
│           │   │   ├── UserEntity.java
│           │   │   ├── FamilyEntity.java
│           │   │   ├── FamilyMemberEntity.java
│           │   │   ├── FamilyInvitationEntity.java
│           │   │   └── UserPreferencesEntity.java
│           │   ├── repository/                   ← JPA Repository truy vấn DB schema 'auth'
│           │   ├── service/                      ← Business logic xác thực & gia đình
│           │   └── filter/CustomJwtFilter.java   ← JWT filter cục bộ của auth service
│           │
│           ├── transaction_service/              ← MAIN TRANSACTION MANAGEMENT
│           │   ├── controller/
│           │   │   └── TransactionController.java ← REST: CRUD giao dịch & đính kèm hóa đơn
│           │   ├── entity/                       
│           │   │   ├── TransactionEntity.java    ← Map với bảng transaction.transactions
│           │   │   └── TransactionAttachmentEntity.java
│           │   ├── repository/                   ← JPA Specification hỗ trợ dynamic filter
│           │   ├── service/TransactionService.java ← Thực hiện tạo giao dịch & gọi WalletService
│           │   └── event/consumer/
│           │       └── ParsedNotificationConsumer.java ← Event: Tự tạo giao dịch sau OCR
│           │
│           ├── notification_service/             ← PUSH NOTIFICATION & BANK PARSER
│           │   ├── config/FirebaseConfig.java    ← Khởi tạo Firebase Admin SDK
│           │   ├── controller/
│           │   │   └── NotificationController.java ← REST: Xem & đánh dấu đã đọc thông báo
│           │   ├── entity/
│           │   │   ├── FcmTokenEntity.java
│           │   │   ├── BankNotificationEntity.java
│           │   │   └── NotificationHistoryEntity.java
│           │   ├── listener/                     
│           │   │   └── NotificationListener.java ← Lắng nghe Spring Events (FCM, Email, Budget alert)
│           │   ├── repository/                   ← Thao tác DB schema 'notification'
│           │   └── service/
│           │       ├── BankNotificationParser.java ← Bóc tách SMS/biến động số dư MB, VCB, Momo
│           │       ├── FcmPushService.java       ← Gửi tin nhắn real push qua Firebase
│           │       └── NotificationService.java
│           │
│           ├── ocr_service/                      ← RECEIPT OCR SCANNER
│           │   ├── controller/OcrController.java ← REST: Upload ảnh hóa đơn
│           │   └── service/OcrService.java       ← Sử dụng Tesseract bóc tách text
│           │
│           ├── ai_service/                       ← NLP & CHAT ASSISTANT
│           │   ├── controller/AiController.java  ← REST: Gợi ý danh mục, Anomaly, Chat AI
│           │   └── service/AiService.java        ← Tích hợp Google Gemini 1.5 Flash API
│           │
│           ├── wallet_service/                   ← WALLET & CATEGORY HUB
│           │   ├── config/
│           │   │   ├── DatabaseConfig.java
│           │   │   └── RedisConfig.java
│           │   ├── controller/
│           │   │   ├── WalletController.java     ← REST: CRUD Ví, Share ví, Tổng số dư
│           │   │   └── CategoryController.java   ← REST: CRUD Danh mục chi tiêu hệ thống/cá nhân
│           │   ├── entity/
│           │   │   ├── WalletEntity.java         
│           │   │   ├── WalletPermissionEntity.java ← Quyền truy cập ví dùng chung
│           │   │   ├── CategoryEntity.java       
│           │   │   └── TransactionEntity.java    ← Bảng dư thừa (wallet.transactions)
│           │   ├── repository/                   ← Thao tác DB schema 'wallet'
│           │   ├── service/
│           │   │   ├── WalletService.java        ← Logic tạo ví, cập nhật số dư ví
│           │   │   └── TransactionService.java   ← Service dư thừa (walletTransactionService)
│           │   ├── event/publisher/
│           │   │   └── WalletEventPublisher.java ← Phát event Spring khi đổi số dư/tạo ví
│           │   └── messaging/
│           │       └── UserCreatedListener.java  ← Tự động tạo ví "Tiền Mặt" khi có user mới
│           │
│           └── reporting_service/                ← DDD REPORTING & BUDGETING
│               ├── controller/
│               │   ├── ReportController.java     ← REST: Báo cáo tháng, Báo cáo Pie, Xu hướng Line
│               │   ├── BudgetController.java     ← REST: CRUD Ngân sách giới hạn
│               │   └── DashboardController.java  ← REST: Thống kê tổng quan Dashboard
│               ├── domain/                       ← DDD Domain model & Value objects
│               │   ├── model/                    ← Model nghiệp vụ: Budget, CategorySummary...
│               │   └── valueobject/              ← AlertThreshold, BudgetPeriod, ExportFormat...
│               ├── entity/                       
│               │   ├── TransactionSummaryEntity.java
│               │   ├── MonthlySummaryEntity.java
│               │   ├── CategorySummaryEntity.java
│               │   └── ExportJobEntity.java
│               ├── repository/                   ← Thao tác DB schema 'reporting'
│               ├── event/consumer/
│               │   └── TransactionEventConsumer.java ← Cập nhật báo cáo & Xóa cache Redis in-memory
│               └── service/
│                   ├── BudgetService.java        ← Kiểm tra vượt giới hạn chi tiêu & Cảnh báo
│                   ├── DashboardService.java     ← Tổng hợp Dashboard (Sử dụng Redis cache)
│                   ├── ReportGeneratorService.java ← Biên soạn PDF (iText) & Excel (POI)
│                   ├── TransactionGrpcClient.java ← Local Wrapper gọi TransactionService trực tiếp
│                   └── WalletGrpcClient.java      ← Local Wrapper gọi WalletService trực tiếp
│
└── src/main/resources/
    ├── application.yml                           ← Cấu hình PostgreSQL, Redis, PermitAll
    ├── application-prod.yml
    ├── firebase-service-account.json             ← File credentials của Firebase Admin
    └── db/migration/
        └── V1__init_monolith_schema.sql          ← Script khởi tạo toàn bộ database schemas
```

---

## 3. Vai Trò Chi Tiết của Các File Cốt Lõi

*   **`MonolithApplication.java`**: Điểm khởi đầu của ứng dụng Spring Boot. Sử dụng `@ComponentScan` để quét gói cơ sở `com.fpm2025` và nạp toàn bộ cấu hình vào Application Context. Kích hoạt xử lý bất đồng bộ qua `@EnableAsync` giúp chạy các Event Listener chạy trên luồng phụ.
*   **`MonolithSecurityConfig.java`**: Trái tim bảo mật của ứng dụng. Đăng ký bộ lọc `JwtAuthenticationFilter` (được nhập từ thư viện dùng chung `fpm-security`), cung cấp Bean `BCryptPasswordEncoder` dùng chung và phân quyền truy cập cho tất cả URL.
*   **`TransactionEventConsumer.java`**: Thay thế cho Kafka consumer cũ trong microservices. Lắng nghe in-memory sự kiện `TransactionCreatedEvent`, tự động tính toán lại dữ liệu phân tích và lưu vào `reporting.monthly_summaries` đồng thời kích hoạt dọn dẹp cache Redis cho dashboard.
*   **`TransactionGrpcClient.java` (trong reporting)**: Một giải pháp chuyển đổi thông minh. Thay vì thực hiện cuộc gọi gRPC qua mạng tới transaction-service cũ, class này trực tiếp tiêm `@Autowired TransactionService` để truy vấn dữ liệu từ bộ nhớ JVM, giữ nguyên interface của hệ thống cũ.

---
> [!NOTE]
> Hệ thống hiện đã được dọn sạch hoàn toàn các cấu hình bảo mật trùng lặp cục bộ (`SecurityConfig.java`), chỉ duy trì cấu hình tập trung duy nhất nhằm bảo đảm an toàn thông tin tuyệt đối.

*Tài liệu cấu trúc mã nguồn được thực hiện bởi Trợ lý AI (Antigravity).*
