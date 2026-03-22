======================================================================================
### Các Tính Năng và Công Nghệ Của Dự Án FPM-2025 (Personal Finance Manager)

Dựa trên phân tích toàn bộ cuộc trò chuyện và thiết kế dự án **FPM-2025**, dưới đây là liệt kê đầy đủ các tính năng chính (theo từng service, ưu tiên MVP và mở rộng) và công nghệ sử dụng. Dự án tập trung vào quản lý tài chính cá nhân với kiến trúc microservices, backend Java, và client mobile Kotlin. Tôi sắp xếp theo service để dễ theo dõi, dựa trên thiết kế trước đó (4 service chính + Gateway + Config).

#### 1. Liệt Kê Các Tính Năng Theo Service

##### a. **User-Auth Service** (Tập trung authentication và quản lý user)
- **MVP (Cốt lõi)**:
  - Đăng nhập Google OAuth2 (nhận code, exchange token, generate JWT).
  - Đăng ký/đăng nhập local (username/password, hash BCrypt, JWT).
  - Validate JWT (gRPC cho internal calls).
  - Lưu user info (email, google_id, username, hashed_password).
- **Mở rộng (Phụ)**:
  - Logout/invalidate JWT (blacklist Redis).
  - Quản lý profile (update email/password, last_login).
  - RBAC (role USER/ADMIN, user_roles table).
  - Audit log auth events (Kafka publish user_created/logged_in).

##### b. **Wallet Service** (Quản lý ví và danh mục)
- **MVP**:
  - CRUD ví (create/update/delete wallet, balance, type cash/card/bank).
  - Xem ledger (lịch sử biến động số dư, gRPC call transaction-service).
  - Cache balance Redis.
- **Mở rộng**:
  - CRUD danh mục (category with icon, parent/child, type expense/income).
  - Kafka listener user_created → tạo default wallet.
  - gRPC GetBalance/UpdateBalance (internal).

##### c. **Transaction Service** (Quản lý giao dịch)
- **MVP**:
  - CRUD giao dịch (add/update/delete transaction with location JSON, type expense/income, sub-category).
  - gRPC call wallet-service update balance (atomic).
  - Audit log (transaction_audit table, trigger).
  - Kafka publish transaction_added.
- **Mở rộng**:
  - Filter giao dịch (by date, category, type).
  - Offline support (client tạo ID, sync with server).

##### d. **Reporting Service** (Thống kê và báo cáo)
- **MVP**:
  - Thống kê chi tiêu (total expense/income, pie chart breakdown by category).
  - Cache stats Redis (total/breakdown query).
- **Mở rộng**:
  - Quản lý ngân sách (set limit, % used progress bar, Kafka listener transaction_added → update used).
  - Export PDF/Excel (iText/Apache POI).
  - gRPC GetStats (internal).

##### e. **API Gateway** (Route và bảo mật)
- **MVP**:
  - Route REST (e.g., /auth → user-auth-service, /wallets → wallet-service).
  - Validate JWT (gRPC call user-auth-service).
- **Mở rộng**:
  - Global CORS (for webview in mobile).
  - Circuit breaker Resilience4j for gRPC calls.

##### f. **Config Server** (Cấu hình tập trung)
- **MVP**:
  - Native mode (file-based config for services).
- **Mở rộng**:
  - Git mode (repo config).

##### g. **Shared-Utils** (Thư viện chung)
- **MVP**:
  - gRPC proto (generate stubs for all services).
- **Mở rộng**:
  - Kotlin stubs for mobile client.

##### h. **Client Kotlin (Mobile)**
- **MVP**:
  - Tích hợp REST (Retrofit) qua Gateway (auth, wallet, transaction, reporting).
- **Mở rộng**:
  - gRPC native (from shared-utils).
  - FCM push (from Kafka events).

#### 3. Liệt Kê Công Nghệ Sử Dụng Trong Dự Án
- **Backend Core**:
  - **Java/Spring Boot**: 3.5.5 (framework chính, auto-config).
  - **Maven**: Build tool, multi-module monorepo.
- **Database & Persistence**:
  - **PostgreSQL**: 4 DBs (user_auth_db, wallet_db, transaction_db, reporting_db), port 5433.
  - **Spring Data JPA**: ORM, repositories.
  - **Flyway**: Migration schema (V1__create_tables.sql).
- **Service Discovery & Communication**:
  - **Eureka Server**: Discovery (port 8761, single instance dev).
  - **gRPC**: Internal calls (net.devh.boot, version 1.72.0, port 9090+).
  - **Kafka**: Event messaging (spring-kafka 3.2.3, topics: user-created, wallet-created, transaction-added).
- **Caching & Messaging**:
  - **Redis**: Caching (balance, stats, port 6379).
  - **Zookeeper**: Kafka coordinator (port 2181).
- **Security & Auth**:
  - **Spring Security**: OAuth2 (Google), local auth (BCrypt).
  - **JJWT**: JWT generation/validation (HS256).
- **API & Documentation**:
  - **Spring Cloud Gateway**: API Gateway (port 8080, routes).
  - **Springdoc OpenAPI**: Swagger UI (version 2.6.0).
- **Config & Monitoring**:
  - **Spring Cloud Config Server**: Native file-based (port 8888).
  - **Actuator**: Health/metrics (management.endpoints.web.exposure.include=health,metrics).
- **Testing & Build**:
  - **JUnit 5**: Unit/integration test.
  - **Testcontainers**: Mock DB/Kafka/Redis in tests.
  - **Lombok**: Reduce boilerplate (1.18.36).
- **Client (Mobile)**:
  - **Kotlin**: 1.9.20 (Android native, Retrofit for REST, gRPC Kotlin for gRPC).
  - **Kotlinx Coroutines**: Async operations.
- **Deployment & Tools**:
  - **Docker Compose**: Multi-container (Postgres, Redis, Zookeeper, Kafka, Kafdrop).
  - **IntelliJ IDEA**: IDE chính.

