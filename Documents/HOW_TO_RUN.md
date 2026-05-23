# FPM-2025: Hướng Dẫn Chạy Dự Án Từ Đầu Đến Cuối

> **Phiên bản:** 1.0 | **Cập nhật:** 2026-05-22  
> **Mục tiêu:** Hướng dẫn chi tiết cho developer mới hoặc khi chạy dự án lần đầu  
> **OS:** Windows (PowerShell) — có ghi chú tương đương cho Linux/macOS

---

## 📋 Mục Lục

1. [Tổng Quan Kiến Trúc Build](#1-tổng-quan-kiến-trúc-build)
2. [Yêu Cầu Phần Mềm (Prerequisites)](#2-yêu-cầu-phần-mềm-prerequisites)
3. [Cấu Trúc Thư Mục Dự Án](#3-cấu-trúc-thư-mục-dự-án)
4. [BƯỚC 1 — Build Shared Libraries (fpm-libs)](#4-bước-1--build-shared-libraries-fpm-libs)
5. [BƯỚC 2 — Build Config Server](#5-bước-2--build-config-server)
6. [BƯỚC 3 — Build Các Microservices (Backend)](#6-bước-3--build-các-microservices-backend)
7. [BƯỚC 4 — Khởi Động Docker (Infrastructure)](#7-bước-4--khởi-động-docker-infrastructure)
8. [BƯỚC 5 — Verify Hệ Thống Đang Chạy](#8-bước-5--verify-hệ-thống-đang-chạy)
9. [BƯỚC 6 — Test API Đầu Tiên](#9-bước-6--test-api-đầu-tiên)
10. [Workflow Hàng Ngày (Sau Lần Đầu)](#10-workflow-hàng-ngày-sau-lần-đầu)
11. [Giải Thích Config & Libs (Tại Sao Quan Trọng)](#11-giải-thích-config--libs-tại-sao-quan-trọng)
12. [Troubleshooting — Lỗi Thường Gặp](#12-troubleshooting--lỗi-thường-gặp)
13. [Cheat Sheet — Lệnh Nhanh](#13-cheat-sheet--lệnh-nhanh)

---

## 1. Tổng Quan Kiến Trúc Build

Trước khi bắt đầu, cần hiểu **TẠI SAO** phải build theo thứ tự này:

```
┌─────────────────────────────────────────────────────────────┐
│                   FPM_Project/                              │
│                                                             │
│  ① libs/fpm-libs/          ← Phải build TRƯỚC TIÊN         │
│     (fpm-core, fpm-domain, fpm-security, fpm-grpc...)       │
│     → Install vào ~/.m2 (local Maven repository)           │
│                     │                                       │
│                     ▼ (các service depend vào libs này)    │
│  ② config/                 ← Build thứ HAI                  │
│     (Spring Cloud Config Server)                           │
│     → Cung cấp YAML config cho tất cả services            │
│                     │                                       │
│                     ▼                                       │
│  ③ Backend/*/              ← Build thứ BA                   │
│     (user-auth, wallet, transaction, reporting...)          │
│     → Tạo JAR files, đóng gói vào Docker images            │
│                     │                                       │
│                     ▼                                       │
│  ④ docker-compose up -d    ← KHỞI ĐỘNG                     │
│     Infrastructure + Platform + Business Services           │
└─────────────────────────────────────────────────────────────┘
```

### Luồng Config khi chạy

```
Config Server (:8888)
    ↑ đọc YAML từ
config/src/main/resources/config/yml_service/
    ├── user-auth-service.yaml   (JWT secret, DB, Kafka, gRPC port...)
    ├── wallet-service.yaml      (DB, Redis, gRPC...)
    ├── transaction-service.yaml (DB, Kafka, RabbitMQ, gRPC...)
    ├── reporting-service.yaml   (DB, Redis, Kafka consumer...)
    ├── notification-service.yaml (FCM, Kafka, RabbitMQ...)
    ├── api-gateway.yml          (Routes, Circuit Breaker, Rate Limit...)
    └── ...

Khi service start → kết nối Config Server → pull YAML về → apply config
```

---

## 2. Yêu Cầu Phần Mềm (Prerequisites)

### 2.1 Cài Đặt Bắt Buộc

| Phần Mềm | Version | Kiểm Tra | Link |
|----------|---------|----------|------|
| **Java JDK** | **21** (LTS) | `java -version` | [adoptium.net](https://adoptium.net) |
| **Apache Maven** | **3.9+** | `mvn -version` | [maven.apache.org](https://maven.apache.org) |
| **Docker Desktop** | Latest | `docker -version` | [docker.com](https://docker.com) |
| **Git** | Latest | `git -version` | — |

### 2.2 Kiểm Tra Môi Trường (Chạy Trên PowerShell)

```powershell
# Chạy các lệnh này để verify
java -version
# Mong đợi: openjdk version "21.x.x"

mvn -version
# Mong đợi: Apache Maven 3.9.x

docker -version
# Mong đợi: Docker version 27.x.x

docker compose version
# Mong đợi: Docker Compose version v2.x.x
```

> ⚠️ **QUAN TRỌNG cho Windows:** Đảm bảo Java 21 đã được set trong `JAVA_HOME` và có trong `PATH`.
> 
> Kiểm tra: `echo $env:JAVA_HOME` → phải trỏ đến thư mục JDK 21

### 2.3 Cấu Hình Docker Desktop (Windows)

> ⚠️ **RAM cần thiết:** FPM cần tối thiểu **6GB RAM** cho Docker:
> - MySQL: ~512MB
> - Kafka + Zookeeper: ~1GB  
> - 8 Microservices: ~3.5GB
> - Redis + RabbitMQ: ~256MB

**Tăng RAM cho Docker Desktop:**
1. Mở Docker Desktop → Settings (⚙️)
2. Resources → Memory → tăng lên **6144 MB (6 GB)**
3. Apply & Restart

---

## 3. Cấu Trúc Thư Mục Dự Án

```
FPM_Project/                          ← Root project
│
├── libs/                             ← Shared Libraries
│   └── fpm-libs/                     ← Multi-module Maven project
│       ├── pom.xml                   ← Parent POM (fpm-libs-parent)
│       ├── fpm-bom/                  ← Bill of Materials (version control)
│       ├── fpm-core/                 ← BaseResponse, Exceptions, JWT utils
│       ├── fpm-common/               ← DateUtil, ValidationUtil, PageUtil
│       ├── fpm-domain/               ← Shared DTOs, Enums, Kafka Events
│       ├── fpm-grpc/                 ← gRPC interceptors, client config
│       ├── fpm-proto/                ← .proto definitions (Protobuf)
│       ├── fpm-messaging/            ← RabbitMQ config, EventPublisher
│       ├── fpm-security/             ← JWT Filter, SecurityConfig tái sử dụng
│       └── fpm-testing/              ← Testcontainers base, TestFactory
│
├── config/                           ← Spring Cloud Config Server
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/resources/
│       └── config/
│           ├── yml_service/          ← ⭐ Config YAML cho từng service
│           │   ├── api-gateway.yml
│           │   ├── user-auth-service.yaml
│           │   ├── wallet-service.yaml
│           │   ├── transaction-service.yaml
│           │   ├── reporting-service.yaml
│           │   ├── notification-service.yaml
│           │   ├── ocr-service.yaml
│           │   ├── ai-service.yaml
│           │   └── eureka-server.yaml
│           └── csv/                  ← Data templates
│
├── Backend/                          ← Tất cả Microservices
│   ├── pom.xml                       ← Parent POM (fpm-backend-parent)
│   ├── docker-compose.yml            ← ⭐ File khởi động toàn bộ hệ thống
│   ├── api-gateway/
│   ├── eureka-server/
│   ├── user-auth-service/
│   ├── wallet-service/
│   ├── transaction-service/
│   ├── reporting_service/            ← ⚠️ Dấu gạch DƯỚI (không phải gạch ngang)
│   ├── notification-service/
│   ├── ocr-service/
│   └── ai-service/
│
├── sql/
│   └── init-databases.sql            ← Script tạo databases MySQL
│
└── fpm_client/                       ← Android app (Kotlin)
```

---

## 4. BƯỚC 1 — Build Shared Libraries (fpm-libs)

> 💡 **Tại sao phải làm bước này?**  
> Các microservices trong `Backend/` đều có dependency vào `fpm-core`, `fpm-domain`, `fpm-security`... Nếu chưa install, Maven sẽ báo lỗi `"artifact not found"` khi build service.

### 4.1 Mở Terminal và Điều Hướng

```powershell
# Windows PowerShell
cd D:\WorkSpace\App_Dev\FPM_Project\libs\fpm-libs

# Verify đang ở đúng thư mục
ls
# Phải thấy: pom.xml, fpm-bom/, fpm-core/, fpm-domain/, ...
```

### 4.2 Build và Install Vào Local Repository

```powershell
# Lệnh build chuẩn (bỏ qua tests để nhanh hơn)
mvn clean install -DskipTests

# Nếu muốn thấy output đầy đủ hơn:
mvn clean install -DskipTests -X 2>&1 | Tee-Object build-libs.log
```

### 4.3 Kết Quả Mong Đợi

```
[INFO] Reactor Summary for FPM Shared Libraries Parent 1.0.0-SNAPSHOT:
[INFO]
[INFO] FPM Shared Libraries Parent .................... SUCCESS [  0.123 s]
[INFO] fpm-bom ........................................ SUCCESS [  1.234 s]
[INFO] fpm-common ..................................... SUCCESS [  3.456 s]
[INFO] fpm-core ....................................... SUCCESS [  5.678 s]
[INFO] fpm-domain ..................................... SUCCESS [  4.321 s]
[INFO] fpm-grpc ....................................... SUCCESS [  6.789 s]
[INFO] fpm-proto ...................................... SUCCESS [  8.901 s]
[INFO] fpm-messaging .................................. SUCCESS [  3.210 s]
[INFO] fpm-security ................................... SUCCESS [  4.567 s]
[INFO] fpm-testing .................................... SUCCESS [  2.345 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4.4 Verify Libs Đã Được Install

```powershell
# Kiểm tra trong local Maven repository
dir "$env:USERPROFILE\.m2\repository\com\fpm2025"

# Phải thấy các thư mục: fpm-bom, fpm-common, fpm-core, fpm-domain, fpm-grpc, ...
```

> ❌ **Nếu thấy lỗi `BUILD FAILURE`:**  
> → Xem Section [12. Troubleshooting](#12-troubleshooting--lỗi-thường-gặp) → Lỗi Build Libs

---

## 5. BƯỚC 2 — Build Config Server

> 💡 **Tại sao cần build riêng?**  
> Config Server là một Spring Boot app riêng biệt nằm ở `config/`. Docker Compose sẽ build Docker image từ source này. Cần build JAR trước.

### 5.1 Điều Hướng và Build

```powershell
cd D:\WorkSpace\App_Dev\FPM_Project\config

# Build JAR
mvn clean package -DskipTests
```

### 5.2 Kết Quả Mong Đợi

```
[INFO] BUILD SUCCESS
[INFO] --- spring-boot:3.4.0:repackage (repackage) ---
[INFO] Replacing main artifact /config/target/config-0.0.1-SNAPSHOT.jar
```

Sau khi build, file JAR sẽ xuất hiện tại:
```
config/target/config-0.0.1-SNAPSHOT.jar   ← Docker sẽ COPY file này
```

### 5.3 Hiểu Cấu Trúc Config

```powershell
# Xem các file config của từng service
dir "D:\WorkSpace\App_Dev\FPM_Project\config\src\main\resources\config\yml_service"
```

**Mỗi file YAML là config riêng của một service:**

```yaml
# Ví dụ: user-auth-service.yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/user_auth_db   # ← DB connection
    username: dev
    password: secret
jwt:
  secret: your_jwt_secret_key                   # ← JWT secret
  expiration: 86400000                           # ← 24h
grpc:
  server:
    port: 9090                                   # ← gRPC port
```

> ⚠️ **Nếu muốn thay đổi config** (VD: JWT secret, DB password):
> - Sửa file trong `config/src/main/resources/config/yml_service/`
> - **Không cần rebuild** — Docker mount volume trực tiếp vào container!
> - Chỉ cần restart service: `docker compose restart user-auth-service`

---

## 6. BƯỚC 3 — Build Các Microservices (Backend)

### 6.1 Build Toàn Bộ Backend Cùng Lúc

```powershell
cd D:\WorkSpace\App_Dev\FPM_Project\Backend

# Build tất cả services (parent POM build tất cả modules)
mvn clean package -DskipTests
```

Lệnh này sẽ build theo thứ tự được định nghĩa trong `Backend/pom.xml`:
- api-gateway
- user-auth-service
- ocr-service
- ai-service
- notification-service
- wallet-service
- reporting_service
- eureka-server
- transaction-service

### 6.2 Kết Quả Mong Đợi

```
[INFO] Reactor Summary for FPM Backend Parent:
[INFO]
[INFO] FPM Backend Parent ............................ SUCCESS
[INFO] api-gateway ................................... SUCCESS
[INFO] user-auth-service ............................. SUCCESS
[INFO] ocr-service ................................... SUCCESS
[INFO] ai-service .................................... SUCCESS
[INFO] notification-service .......................... SUCCESS
[INFO] wallet-service ................................ SUCCESS
[INFO] reporting_service ............................. SUCCESS
[INFO] eureka-server ................................. SUCCESS
[INFO] transaction-service ........................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### 6.3 Verify JAR Files Đã Được Tạo

```powershell
# Kiểm tra JAR files đã có
dir D:\WorkSpace\App_Dev\FPM_Project\Backend\user-auth-service\target\*.jar
dir D:\WorkSpace\App_Dev\FPM_Project\Backend\wallet-service\target\*.jar
dir D:\WorkSpace\App_Dev\FPM_Project\Backend\transaction-service\target\*.jar

# Tất cả phải có file *.jar trong thư mục target/
```

### 6.4 Build Từng Service Riêng Lẻ (Tùy Chọn)

```powershell
# Chỉ build một service cụ thể
cd D:\WorkSpace\App_Dev\FPM_Project\Backend\user-auth-service
mvn clean package -DskipTests

cd D:\WorkSpace\App_Dev\FPM_Project\Backend\wallet-service
mvn clean package -DskipTests

# Trở về Backend để chạy docker
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
```

---

## 7. BƯỚC 4 — Khởi Động Docker (Infrastructure)

### 7.1 Chuẩn Bị: Kiểm Tra Docker

```powershell
# Đảm bảo Docker Desktop đang chạy
docker ps
# Nếu không có lỗi = Docker OK

# Điều hướng về thư mục chứa docker-compose.yml
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
```

### 7.2 Chiến Lược: Khởi Động Theo Từng Lớp (Khuyến Nghị Lần Đầu)

#### 🔴 Lớp 1: Infrastructure (MySQL, Redis, Kafka, RabbitMQ)

```powershell
# Khởi động infrastructure
docker compose up mysql redis zookeeper kafka rabbitmq -d

# Theo dõi để biết khi nào MySQL healthy
Write-Host "Đang chờ MySQL healthy (~30-60 giây)..."
docker compose ps
```

**Chờ cho đến khi thấy:**
```
NAME            STATUS
fpm-mysql       running (healthy)    ← ✅ PHẢI là "healthy"
fpm-redis       running
fpm-zookeeper   running
fpm-kafka       running
fpm-rabbitmq    running
```

> ⏳ **Tip:** MySQL lần đầu cần khởi tạo databases từ `sql/init-databases.sql`. Có thể mất **30-90 giây**.

**Kiểm tra MySQL đã tạo databases chưa:**
```powershell
docker exec fpm-mysql mysql -uroot -proot -e "SHOW DATABASES;"
# Phải thấy: user_auth_db, wallet_db, transaction_db, reporting_db, notification_db
```

#### 🟡 Lớp 2: Platform Services (Config Server, Eureka)

```powershell
# Khởi động Config Server và Eureka
docker compose up config-server eureka-server -d

# Theo dõi logs Config Server
docker logs fpm-config-server -f
```

**Chờ thấy trong logs:**
```
Started ConfigServerApplication in X.XXX seconds
Tomcat started on port(s): 8888
```

**Verify Config Server hoạt động:**
```powershell
# Lấy config của wallet-service (test)
Invoke-RestMethod http://localhost:8888/wallet-service/default
# Phải trả về JSON với config YAML đã được load
```

#### 🟢 Lớp 3: Business Services

```powershell
# Khởi động tất cả microservices
docker compose up user-auth-service wallet-service transaction-service `
    reporting-service notification-service ocr-service ai-service api-gateway -d

# Hoặc đơn giản hơn — start tất cả còn lại
docker compose up -d
```

### 7.3 Cách Nhanh: Start Toàn Bộ Cùng Lúc

> Dùng cho lần thứ 2 trở đi khi images đã được build:

```powershell
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
docker compose up -d
```

Docker Compose sẽ tự động xử lý thứ tự nhờ `depends_on` trong file `docker-compose.yml`.

### 7.4 Build Images Lần Đầu (Nếu Chưa Có)

```powershell
# Build TẤT CẢ Docker images từ source code
docker compose build

# Sau đó start
docker compose up -d
```

Hoặc kết hợp:
```powershell
docker compose up --build -d
```

> ⏳ **Lần đầu build images có thể mất 10-20 phút** (phải tải base image `eclipse-temurin:21-jre-jammy`).

---

## 8. BƯỚC 5 — Verify Hệ Thống Đang Chạy

### 8.1 Kiểm Tra Trạng Thái Containers

```powershell
docker compose ps
```

**Kết quả mong đợi (sau ~5 phút):**

```
NAME                       STATUS              PORTS
fpm-mysql                  running (healthy)   0.0.0.0:3306->3306/tcp
fpm-redis                  running             0.0.0.0:6379->6379/tcp
fpm-zookeeper              running
fpm-kafka                  running             0.0.0.0:29092->29092/tcp
fpm-rabbitmq               running             0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
fpm-config-server          running (healthy)   0.0.0.0:8888->8888/tcp
fpm-eureka-server          running             0.0.0.0:8761->8761/tcp
fpm-user-auth-service      running             0.0.0.0:8081->8081/tcp
fpm-wallet-service         running             0.0.0.0:8082->8082/tcp
fpm-transaction-service    running             0.0.0.0:8083->8083/tcp
fpm-reporting-service      running             0.0.0.0:8084->8084/tcp
fpm-notification-service   running             0.0.0.0:8085->8085/tcp
fpm-ocr-service            running             0.0.0.0:8086->8086/tcp
fpm-ai-service             running             0.0.0.0:8087->8087/tcp
fpm-api-gateway            running             0.0.0.0:8080->8089/tcp
```

### 8.2 Kiểm Tra Eureka Dashboard

**Mở trình duyệt:** http://localhost:8761

> Thấy danh sách services đã registered:
> ```
> API-GATEWAY           → UP (1)
> USER-AUTH-SERVICE     → UP (1)
> WALLET-SERVICE        → UP (1)
> TRANSACTION-SERVICE   → UP (1)
> REPORTING-SERVICE     → UP (1)
> NOTIFICATION-SERVICE  → UP (1)
> OCR-SERVICE           → UP (1)
> AI-SERVICE            → UP (1)
> ```

> ⚠️ Nếu service chưa xuất hiện, chờ thêm 30-60 giây (Eureka heartbeat interval).

### 8.3 Kiểm Tra Health Endpoints

```powershell
# Test từng service qua direct port
Invoke-RestMethod http://localhost:8081/api/v1/auth/health   # user-auth
Invoke-RestMethod http://localhost:8082/actuator/health       # wallet
Invoke-RestMethod http://localhost:8083/actuator/health       # transaction
Invoke-RestMethod http://localhost:8084/actuator/health       # reporting
Invoke-RestMethod http://localhost:8085/actuator/health       # notification
Invoke-RestMethod http://localhost:8080/actuator/health       # api-gateway

# Test qua API Gateway (cách client thực sự dùng)
Invoke-RestMethod http://localhost:8080/actuator/health
```

**Kết quả mong đợi:** `{"status":"UP"}`

### 8.4 Kiểm Tra Config Server Đang Phục Vụ Config

```powershell
# Xem config đang được cung cấp cho wallet-service
Invoke-RestMethod "http://localhost:8888/wallet-service/default" | ConvertTo-Json -Depth 5

# Xem config gateway
Invoke-RestMethod "http://localhost:8888/api-gateway/default" | ConvertTo-Json -Depth 5
```

### 8.5 Kiểm Tra Các Infrastructure Tools

**RabbitMQ Management UI:**  
→ http://localhost:15672 | User: `admin` | Pass: `admin`  
→ Phải thấy các Exchanges và Queues đã được tạo

**Kafka Topics (qua terminal):**
```powershell
docker exec fpm-kafka kafka-topics --bootstrap-server localhost:9092 --list
# Phải thấy: transaction.created, user.created, wallet.created, balance.changed...
```

**MySQL Databases:**
```powershell
docker exec fpm-mysql mysql -uroot -proot -e "SHOW DATABASES;"
# Phải thấy 5 databases: user_auth_db, wallet_db, transaction_db, reporting_db, notification_db
```

**Redis:**
```powershell
docker exec fpm-redis redis-cli ping
# PONG
```

---

## 9. BƯỚC 6 — Test API Đầu Tiên

### 9.1 Test Thủ Công Bằng PowerShell

```powershell
# 1. Đăng ký user mới
$registerBody = @{
    email    = "test@fpm.dev"
    password = "Password123!"
    username = "testuser"
} | ConvertTo-Json

$registerResult = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/auth/register" `
    -Method POST `
    -Body $registerBody `
    -ContentType "application/json"

Write-Host "Register result:" ($registerResult | ConvertTo-Json)
```

**Kết quả mong đợi:**
```json
{
  "success": true,
  "messageCode": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc..."
  }
}
```

```powershell
# 2. Lấy token từ kết quả register
$token = $registerResult.data.accessToken

# 3. Lấy profile
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/me" -Headers $headers
```

```powershell
# 4. Tạo ví (wallet service thông qua Gateway)
$walletBody = @{
    name           = "Ví tiền mặt"
    type           = "CASH"
    currency       = "VND"
    initialBalance = 1000000
} | ConvertTo-Json

$walletResult = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/wallets" `
    -Method POST `
    -Headers $headers `
    -Body $walletBody `
    -ContentType "application/json"

Write-Host "Wallet created: ID =" $walletResult.data.id
$walletId = $walletResult.data.id
```

```powershell
# 5. Tạo giao dịch
$txnBody = @{
    walletId        = $walletId
    categoryId      = 1
    amount          = 50000
    type            = "EXPENSE"
    note            = "Cà phê sáng"
    transactionDate = "2026-05-22T08:00:00"
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/transactions" `
    -Method POST `
    -Headers $headers `
    -Body $txnBody `
    -ContentType "application/json"
```

### 9.2 Test Bằng Postman

1. Mở **Postman**
2. Import `Backend/Documents/FPM_2025_Postman_Collection.json`
3. Đặt `base_url = http://localhost:8080/api/v1` trong Collection Variables
4. Chạy `Register` → copy `accessToken` → paste vào biến `token`
5. Chạy các request khác

### 9.3 Xem Swagger UI (Optional)

Nếu Swagger đã được cấu hình:
- user-auth: http://localhost:8081/swagger-ui.html
- wallet: http://localhost:8082/swagger-ui.html
- transaction: http://localhost:8083/swagger-ui.html

---

## 10. Workflow Hàng Ngày (Sau Lần Đầu)

### 10.1 Bắt Đầu Buổi Làm Việc

```powershell
# Chỉ cần start Docker (đã build trước đó)
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
docker compose up -d

# Chờ ~3-5 phút để tất cả services healthy
docker compose ps
```

### 10.2 Kết Thúc Buổi Làm Việc

```powershell
# Dừng tất cả containers (GIỮ DATA — volumes vẫn còn)
docker compose down

# Hôm sau start lại sẽ có đầy đủ data cũ
```

### 10.3 Sau Khi Sửa Code Một Service

```powershell
# Ví dụ: sửa code trong wallet-service

# 1. Rebuild JAR của service đó
cd D:\WorkSpace\App_Dev\FPM_Project\Backend\wallet-service
mvn clean package -DskipTests

# 2. Rebuild Docker image và restart service
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
docker compose up wallet-service --build -d

# 3. Xem logs để confirm
docker logs fpm-wallet-service -f --tail 50
```

### 10.4 Sau Khi Sửa Config (YAML Files)

```powershell
# Config được mount trực tiếp từ filesystem → KHÔNG cần rebuild!

# Chỉ cần restart service tương ứng
docker compose restart user-auth-service

# Hoặc restart tất cả services (không restart infrastructure)
docker compose restart user-auth-service wallet-service transaction-service `
    reporting-service notification-service api-gateway
```

### 10.5 Sau Khi Sửa fpm-libs

```powershell
# 1. Rebuild và reinstall libs
cd D:\WorkSpace\App_Dev\FPM_Project\libs\fpm-libs
mvn clean install -DskipTests

# 2. Rebuild services phụ thuộc vào lib đã sửa
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
mvn clean package -DskipTests

# 3. Rebuild Docker images
docker compose up --build -d
```

### 10.6 Reset Hoàn Toàn (Xóa Hết Data)

```powershell
# ⚠️ CẢNH BÁO: Lệnh này xóa toàn bộ data (MySQL, Redis...)!
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
docker compose down -v

# Start lại từ đầu (MySQL sẽ chạy lại init-databases.sql)
docker compose up -d
```

---

## 11. Giải Thích Config & Libs (Tại Sao Quan Trọng)

### 11.1 fpm-libs — Tại Sao Phải Build Trước?

Mỗi microservice trong `Backend/` đều khai báo dependency vào các module `fpm-libs`:

```xml
<!-- Ví dụ trong wallet-service/pom.xml -->
<dependency>
    <groupId>com.fpm2025</groupId>
    <artifactId>fpm-domain</artifactId>  <!-- ← fpm-libs module -->
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.fpm2025</groupId>
    <artifactId>fpm-security</artifactId>  <!-- ← fpm-libs module -->
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Khi `mvn clean package` được chạy cho wallet-service, Maven tìm các artifacts này trong:
1. **Local repository** (`~/.m2/repository/com/fpm2025/`) ← ưu tiên
2. Remote repository (GitHub Packages)

Nếu không có trong `~/.m2` và remote cũng không có → **BUILD FAILURE**

→ Đó là lý do **bước 1 (`mvn install` fpm-libs) là bắt buộc**.

### 11.2 Vai Trò Của Từng Module fpm-libs

| Module | Chứa Gì | Service Dùng |
|--------|---------|-------------|
| `fpm-bom` | Quản lý version tập trung | Tất cả |
| `fpm-core` | `BaseResponse`, Exception handling, JWT utilities | Tất cả |
| `fpm-domain` | Shared DTOs (`WalletResponse`, `TransactionResponse`), Enums (`CategoryType`, `WalletType`), Kafka Events (`TransactionCreatedEvent`) | wallet, transaction, reporting |
| `fpm-common` | `DateUtil`, `ValidationUtil`, `PageUtil` | Tất cả |
| `fpm-grpc` | gRPC interceptors, client configuration | wallet, transaction, reporting, user-auth |
| `fpm-proto` | `.proto` definitions — contract cho gRPC | wallet, transaction, user-auth |
| `fpm-messaging` | `DomainEvent`, `EventPublisher`, RabbitMQ config | wallet, transaction, notification |
| `fpm-security` | JWT Filter, Spring Security config có thể tái sử dụng | user-auth, wallet, transaction, reporting |
| `fpm-testing` | Testcontainers base, `TestDataFactory` | Testing |

### 11.3 Config Server — Cách Hoạt Động

```
Khi service start:
  1. Service đọc bootstrap config: "SPRING_CLOUD_CONFIG_URI=http://config-server:8888"
  2. Kết nối Config Server → GET /user-auth-service/default
  3. Config Server đọc file: config/src/main/resources/config/yml_service/user-auth-service.yaml
  4. Trả về JSON chứa toàn bộ config
  5. Service apply config: DB URL, JWT secret, gRPC port, Kafka, ...
```

**Ưu điểm:** Thay đổi config không cần rebuild service — chỉ restart!

```
config/src/main/resources/config/yml_service/
├── api-gateway.yml           → Routes, Rate Limit, Circuit Breaker
├── user-auth-service.yaml    → DB, JWT Secret, gRPC port 9090, Kafka, RabbitMQ
├── wallet-service.yaml       → DB, Redis, gRPC port 9091, Kafka, RabbitMQ
├── transaction-service.yaml  → DB, Kafka topics, gRPC client config
├── reporting-service.yaml    → DB, Redis cache, Kafka consumer groups
├── notification-service.yaml → Firebase config, Kafka topics, SMS parser
├── ocr-service.yaml          → Tesseract config
└── ai-service.yaml           → Gemini API key
```

### 11.4 Thứ Tự Phụ Thuộc (Dependency Chain)

```
Docker Compose startup order:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 mysql (healthcheck: mysqladmin ping)
    ↓ healthy
 redis, zookeeper, kafka, rabbitmq (service_started)
    ↓
 config-server (healthcheck: /actuator/health)
    ↓ healthy
 eureka-server (depends: config-server healthy)
    ↓
 user-auth-service (depends: mysql healthy + config healthy + eureka started)
 wallet-service    (depends: mysql healthy + config healthy + eureka + redis)
 transaction-service (depends: mysql + config + kafka + rabbitmq + eureka)
 reporting-service  (depends: mysql + config + eureka + redis)
 notification-service (depends: mysql + config + kafka + rabbitmq + eureka)
 api-gateway         (depends: config healthy + eureka + redis)
```

---

## 12. Troubleshooting — Lỗi Thường Gặp

### ❌ Lỗi A: Build Libs Fail — "Compilation Error"

**Triệu chứng:**
```
[ERROR] COMPILATION ERROR
[ERROR] cannot find symbol
```

**Nguyên nhân & Fix:**
```powershell
# 1. Kiểm tra Java version
java -version
# Phải là Java 21! Nếu là Java 17/11 → cài Java 21

# 2. Kiểm tra JAVA_HOME
echo $env:JAVA_HOME
# Phải trỏ đến JDK 21

# 3. Set JAVA_HOME nếu cần
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.x.x"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 4. Retry build
mvn clean install -DskipTests
```

---

### ❌ Lỗi B: Backend Build Fail — "Artifact Not Found" (fpm-domain, fpm-core...)

**Triệu chứng:**
```
[ERROR] Could not resolve dependencies for project com.fpm2025:wallet-service
[ERROR] Artifact com.fpm2025:fpm-domain:jar:1.0.0-SNAPSHOT not found
```

**Nguyên nhân:** Chưa build `fpm-libs`!

**Fix:**
```powershell
# Quay lại build libs trước
cd D:\WorkSpace\App_Dev\FPM_Project\libs\fpm-libs
mvn clean install -DskipTests

# Sau đó retry build Backend
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
mvn clean package -DskipTests
```

---

### ❌ Lỗi C: Docker — "Cannot Start Container — Port Already in Use"

**Triệu chứng:**
```
Error: port is already allocated
Bind for 0.0.0.0:3306 failed: port is already allocated
```

**Fix:**
```powershell
# Tìm process đang dùng port 3306 (MySQL)
netstat -ano | findstr :3306

# Kill process (thay PID với số tìm được)
taskkill /PID <PID> /F

# Hoặc dừng MySQL service Windows nếu có
Stop-Service -Name "MySQL80" -ErrorAction SilentlyContinue
```

---

### ❌ Lỗi D: Service Không Start — "Communications link failure" (MySQL)

**Triệu chứng trong logs:**
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**Fix:**
```powershell
# 1. Kiểm tra MySQL status
docker compose ps mysql
# Phải là: running (healthy)

# 2. Nếu MySQL đang "running" nhưng không "healthy" → xem logs
docker logs fpm-mysql --tail 50

# 3. Kiểm tra databases đã được tạo chưa
docker exec fpm-mysql mysql -uroot -proot -e "SHOW DATABASES;"

# 4. Nếu thiếu database → chạy init script thủ công
docker exec -i fpm-mysql mysql -uroot -proot < D:\WorkSpace\App_Dev\FPM_Project\sql\init-databases.sql

# 5. Restart service
docker compose restart user-auth-service
```

---

### ❌ Lỗi E: Config Server Không Load Config

**Triệu chứng:**
```
Could not resolve placeholder 'spring.datasource.url' in value "${spring.datasource.url}"
```

**Fix:**
```powershell
# 1. Kiểm tra Config Server có healthy không
Invoke-RestMethod http://localhost:8888/actuator/health

# 2. Test config endpoint
Invoke-RestMethod "http://localhost:8888/user-auth-service/default"

# 3. Kiểm tra volume mount có đúng không
docker exec fpm-config-server ls /app/config/yml_service/
# Phải thấy: user-auth-service.yaml, wallet-service.yaml...

# 4. Nếu file không có → Config Server build chưa đúng
# Rebuild config server
cd D:\WorkSpace\App_Dev\FPM_Project\config
mvn clean package -DskipTests
docker compose up config-server --build -d
```

---

### ❌ Lỗi F: Service Không Đăng Ký Vào Eureka

**Triệu chứng:** Service không xuất hiện ở http://localhost:8761

**Fix:**
```powershell
# 1. Kiểm tra service đang chạy
docker compose ps user-auth-service

# 2. Xem logs
docker logs fpm-user-auth-service --tail 100

# 3. Kiểm tra Eureka URL có đúng không
docker inspect fpm-user-auth-service | Select-String "EUREKA"
# Phải là: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://admin:admin@eureka-server:8761/eureka/

# 4. Kiểm tra Eureka có running không
Invoke-RestMethod http://localhost:8761/actuator/health
```

---

### ❌ Lỗi G: 503 Service Unavailable Từ API Gateway

**Triệu chứng:** Gọi API qua `localhost:8080` → `503`

**Fix:**
```powershell
# 1. Xem service đã registered vào Eureka chưa
# → Truy cập http://localhost:8761

# 2. Kiểm tra Circuit Breaker
Invoke-RestMethod "http://localhost:8080/actuator/circuitbreakers"

# 3. Restart service bị lỗi
docker compose restart wallet-service

# 4. Chờ service registered vào Eureka (30-60s)
Start-Sleep 60
```

---

### ❌ Lỗi H: Kafka Consumer Không Nhận Event

**Triệu chứng:** Sau khi đăng ký, ví mặc định không được tạo tự động

**Fix:**
```powershell
# 1. Kiểm tra Kafka có running không
docker logs fpm-kafka --tail 30

# 2. List topics
docker exec fpm-kafka kafka-topics --bootstrap-server localhost:9092 --list

# 3. Xem consumer groups
docker exec fpm-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# 4. Xem logs wallet-service (consumer user.created)
docker logs fpm-wallet-service --tail 100 | Select-String "user.created"
docker logs fpm-wallet-service --tail 100 | Select-String "Received"
```

---

### ❌ Lỗi I: Docker Build Fail — "JAR not found"

**Triệu chứng:**
```
COPY failed: file not found in build context: target/*.jar
```

**Nguyên nhân:** Chưa build JAR trước khi build Docker image.

**Fix:**
```powershell
# Build JAR trước
cd D:\WorkSpace\App_Dev\FPM_Project\Backend\user-auth-service
mvn clean package -DskipTests

# Sau đó mới build Docker image
cd D:\WorkSpace\App_Dev\FPM_Project\Backend
docker compose up user-auth-service --build -d
```

---

### ❌ Lỗi J: Out of Memory / Docker Crashes

**Triệu chứng:** Container bị kill, `docker compose ps` thấy service `Exited`

**Fix:**
```powershell
# 1. Tăng RAM Docker Desktop lên 6-8GB (xem mục 2.3)

# 2. Xem container nào bị OOM killed
docker inspect fpm-wallet-service | Select-String "OOMKilled"

# 3. Giảm bớt JVM heap nếu cần — sửa Dockerfile
# ENV JAVA_OPTS="-Xms128m -Xmx256m"  ← giảm xuống

# 4. Hoặc chỉ chạy services cần thiết
docker compose up mysql redis config-server eureka-server user-auth-service api-gateway -d
```

---

## 13. Cheat Sheet — Lệnh Nhanh

### Build Commands

```powershell
# Build libs (PHẢI làm trước tiên)
cd D:\WorkSpace\App_Dev\FPM_Project\libs\fpm-libs && mvn clean install -DskipTests

# Build config server
cd D:\WorkSpace\App_Dev\FPM_Project\config && mvn clean package -DskipTests

# Build tất cả backend services
cd D:\WorkSpace\App_Dev\FPM_Project\Backend && mvn clean package -DskipTests

# Build một service cụ thể
cd D:\WorkSpace\App_Dev\FPM_Project\Backend\wallet-service && mvn clean package -DskipTests
```

### Docker Commands

```powershell
# ─── Khởi động ───────────────────────────────────────────
docker compose up -d                          # Start tất cả
docker compose up --build -d                  # Build image rồi start
docker compose up mysql redis kafka -d         # Chỉ infrastructure

# ─── Dừng ────────────────────────────────────────────────
docker compose down                           # Dừng, giữ data
docker compose down -v                        # Dừng + xóa data (RESET)

# ─── Xem trạng thái ──────────────────────────────────────
docker compose ps                             # Status tất cả containers
docker compose ps mysql                       # Status MySQL cụ thể

# ─── Logs ────────────────────────────────────────────────
docker logs fpm-user-auth-service -f          # Follow logs
docker logs fpm-wallet-service --tail 100     # 100 dòng cuối
docker compose logs -f user-auth-service wallet-service  # Multiple

# ─── Restart ─────────────────────────────────────────────
docker compose restart user-auth-service      # Restart service
docker compose up user-auth-service --build -d  # Rebuild + restart

# ─── Debug Infrastructure ─────────────────────────────────
docker exec -it fpm-mysql mysql -uroot -proot       # MySQL shell
docker exec -it fpm-redis redis-cli                  # Redis CLI
docker exec fpm-kafka kafka-topics --bootstrap-server localhost:9092 --list  # Kafka topics
```

### Kiểm Tra Nhanh

```powershell
# Health checks
Invoke-RestMethod http://localhost:8080/actuator/health   # Gateway
Invoke-RestMethod http://localhost:8888/actuator/health   # Config Server
Invoke-RestMethod http://localhost:8761/actuator/health   # Eureka

# Config check
Invoke-RestMethod "http://localhost:8888/user-auth-service/default"

# API test nhanh (đăng ký user)
$body = '{"email":"quick@test.dev","password":"Password123!","username":"quicktest"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method POST -Body $body -ContentType "application/json"

# Xem databases
docker exec fpm-mysql mysql -uroot -proot -e "SHOW DATABASES;"

# Redis check
docker exec fpm-redis redis-cli ping         # → PONG
docker exec fpm-redis redis-cli KEYS "*"      # Tất cả keys
```

---

## Tóm Tắt Quy Trình (Summary)

```
┌─────────────────────────────────────────────────────────────────┐
│                    FIRST TIME SETUP                             │
│                                                                 │
│  1. cd libs/fpm-libs && mvn clean install -DskipTests           │
│     (Install shared libraries vào ~/.m2)                        │
│                         │                                       │
│                         ▼                                       │
│  2. cd config && mvn clean package -DskipTests                  │
│     (Build Config Server JAR)                                   │
│                         │                                       │
│                         ▼                                       │
│  3. cd Backend && mvn clean package -DskipTests                 │
│     (Build tất cả microservice JARs)                            │
│                         │                                       │
│                         ▼                                       │
│  4. docker compose up --build -d                                │
│     (Build Docker images + Khởi động toàn bộ hệ thống)        │
│                         │                                       │
│                         ▼                                       │
│  5. Chờ ~3-5 phút → Kiểm tra http://localhost:8761             │
│     (Tất cả services phải UP)                                   │
│                         │                                       │
│                         ▼                                       │
│  6. Test: POST http://localhost:8080/api/v1/auth/register       │
│     (Thấy 201 + accessToken = Hệ thống chạy tốt! ✅)          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    DAILY WORKFLOW                               │
│                                                                 │
│  cd Backend && docker compose up -d                             │
│  (Chỉ cần lệnh này mỗi sáng — images đã có sẵn)               │
└─────────────────────────────────────────────────────────────────┘
```

---

*Tài liệu này được viết dựa trên source code thực tế của dự án FPM-2025.*  
*Cập nhật: 2026-05-22 | Phiên bản: 1.0*
