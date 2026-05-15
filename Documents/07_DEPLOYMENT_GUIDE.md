# 07 — Deployment Guide

> **Version:** 1.0 | **Updated:** 2026-05-15  
> **Môi trường:** Local Development với Docker Compose

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Project Structure & Build Order](#2-project-structure--build-order)
3. [Startup Order & Dependencies](#3-startup-order--dependencies)
4. [Environment Variables Reference](#4-environment-variables-reference)
5. [Step-by-step: First-time Setup](#5-step-by-step-first-time-setup)
6. [Daily Development Workflow](#6-daily-development-workflow)
7. [Service Health Verification](#7-service-health-verification)
8. [Troubleshooting Guide](#8-troubleshooting-guide)
9. [Port Reference](#9-port-reference)

---

## 1. Prerequisites

### Phần mềm bắt buộc

| Tool | Version | Link |
|------|---------|------|
| **Java JDK** | 21 (LTS) | [adoptium.net](https://adoptium.net) |
| **Apache Maven** | 3.9+ | [maven.apache.org](https://maven.apache.org) |
| **Docker Desktop** | Latest | [docker.com](https://docker.com) |
| **Docker Compose** | v2.x (bundled) | Included với Docker Desktop |
| **Git** | Latest | — |

### Kiểm tra môi trường

```bash
java -version        # openjdk version "21.x.x"
mvn -version         # Apache Maven 3.9.x
docker -version      # Docker version 2x.x.x
docker compose version  # Docker Compose version v2.x
```

### Docker Desktop Settings (Windows)

> ⚠️ **RAM Allocation:** Docker Desktop mặc định dùng 2GB RAM. FPM cần ít nhất **6GB** để chạy đầy đủ (MySQL + Kafka + 7 services).
>
> Settings → Resources → Memory → tăng lên **6144 MB (6GB)**

---

## 2. Project Structure & Build Order

### Tổng quan workspace

```
FPM_Project/
├── libs/fpm-libs/          ← BUILD ĐẦU TIÊN (shared libraries)
├── config/                 ← Config Server source
└── Backend/
    ├── docker-compose.yml  ← Orchestration file
    ├── eureka-server/
    ├── api-gateway/
    ├── user-auth-service/
    ├── wallet-service/
    ├── transaction-service/
    ├── reporting_service/  ← NOTE: dấu gạch dưới
    ├── notification-service/
    ├── ocr-service/
    └── ai-service/
```

### Build dependency order

```
fpm-libs (Maven install) → Config Server → Eureka → Microservices
```

> 💡 **Tại sao build `fpm-libs` trước?** Các microservice có `<dependency>` vào `fpm-core`, `fpm-domain`, `fpm-security`... Nếu chưa install vào local Maven repository (`~/.m2`), Maven sẽ báo lỗi "artifact not found".

---

## 3. Startup Order & Dependencies

Đây là thứ tự **bắt buộc** khi khởi động — Docker Compose đã cấu hình `depends_on` để đảm bảo điều này:

```
Layer 1 (Infrastructure):
├── mysql          ──(healthcheck: ping)──► HEALTHY trước khi layer 2 start
├── redis          ──(service_started)
├── zookeeper      ──(service_started)
├── kafka          ──(depends: zookeeper)
└── rabbitmq       ──(service_started)
           │
           ▼
Layer 2 (Platform):
├── config-server  ──(healthcheck: /actuator/health)──► HEALTHY
│       │
│       └── eureka-server ──(depends: config-server)
           │
           ▼
Layer 3 (Business Services):
├── user-auth-service   ──(depends: mysql:healthy, config:healthy, eureka:started)
├── wallet-service      ──(depends: mysql:healthy, config:healthy, eureka:started, redis)
├── transaction-service ──(depends: mysql:healthy, config:healthy, kafka, rabbitmq, eureka)
├── reporting-service   ──(depends: mysql:healthy, config:healthy, eureka, redis)
├── notification-service──(depends: mysql:healthy, config:healthy, kafka, rabbitmq, eureka)
└── api-gateway         ──(depends: config:healthy, eureka:started)
```

**Thời gian khởi động ước tính:**

| Layer | Wait time |
|-------|-----------|
| MySQL healthcheck | ~30-60 giây (lần đầu) |
| Config Server | ~15 giây |
| Eureka | ~15 giây |
| Mỗi microservice | ~20-40 giây |
| **Tổng** | **~3-5 phút** |

---

## 4. Environment Variables Reference

Mọi service nhận config từ 2 nguồn: **Docker Compose env vars** (ưu tiên cao hơn) và **Config Server YAML**.

### Infrastructure credentials

| Variable | Value | Service |
|----------|-------|---------|
| `MYSQL_ROOT_PASSWORD` | `root` | mysql |
| `MYSQL_USER` | `dev` | mysql |
| `MYSQL_PASSWORD` | `secret` | mysql |
| `RABBITMQ_DEFAULT_USER` | `admin` | rabbitmq |
| `RABBITMQ_DEFAULT_PASS` | `admin` | rabbitmq |

### Per-service environment variables

```yaml
# user-auth-service
SPRING_DATASOURCE_URL:      jdbc:mysql://mysql:3306/user_auth_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME: root
SPRING_DATASOURCE_PASSWORD: root
SPRING_CLOUD_CONFIG_URI:    http://config-server:8888
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://admin:admin@eureka-server:8761/eureka/

# wallet-service (thêm Redis)
SPRING_REDIS_HOST: redis

# transaction-service (thêm Kafka + RabbitMQ)
SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
SPRING_RABBITMQ_HOST:           rabbitmq

# reporting-service (thêm Redis)
SPRING_REDIS_HOST: redis
```

### Biến cần thêm cho Production

> ⚠️ **Security**: Các biến sau đang hardcoded trong YAML — cần chuyển sang env vars khi deploy production.

```yaml
# Thêm vào docker-compose.yml environment section:
JWT_SECRET:               ${JWT_SECRET:-change_me_in_production}
SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-root}
```

---

## 5. Step-by-step: First-time Setup

### Bước 1: Build shared libraries

```bash
cd FPM_Project/libs/fpm-libs
mvn clean install -DskipTests
```

**Kết quả mong đợi:**
```
[INFO] fpm-libs ........................... SUCCESS
[INFO] fpm-bom ............................ SUCCESS
[INFO] fpm-common ......................... SUCCESS
[INFO] fpm-core ........................... SUCCESS
[INFO] fpm-domain ......................... SUCCESS
[INFO] fpm-grpc ........................... SUCCESS
[INFO] fpm-proto .......................... SUCCESS
[INFO] fpm-messaging ...................... SUCCESS
[INFO] fpm-security ....................... SUCCESS
[INFO] fpm-testing ........................ SUCCESS
[INFO] BUILD SUCCESS
```

### Bước 2: Build Config Server

```bash
cd FPM_Project/config
mvn clean package -DskipTests
```

### Bước 3: Build các microservices

```bash
cd FPM_Project/Backend

# Build tất cả services cùng lúc (parent pom)
mvn clean package -DskipTests

# Hoặc build từng service:
cd user-auth-service && mvn clean package -DskipTests
cd wallet-service && mvn clean package -DskipTests
# ...
```

### Bước 4: Khởi động Infrastructure

```bash
cd FPM_Project/Backend

# Chỉ start infrastructure trước
docker compose up mysql redis zookeeper kafka rabbitmq -d

# Chờ MySQL healthy (~30s)
docker compose ps  # mysql: healthy
```

### Bước 5: Khởi động Platform Services

```bash
# Start Config Server + Eureka
docker compose up config-server eureka-server -d

# Chờ config-server healthy (~20s)
docker logs fpm-config-server --tail 20
# Phải thấy: "Started ConfigServerApplication"
```

### Bước 6: Khởi động Business Services

```bash
# Start tất cả microservices
docker compose up user-auth-service wallet-service transaction-service \
    reporting-service notification-service api-gateway -d

# Hoặc start toàn bộ từ đầu (Docker Compose tự xử lý thứ tự)
docker compose up -d
```

### Bước 7: Verify hệ thống

```bash
# Kiểm tra tất cả containers
docker compose ps

# Mong đợi: tất cả status "running"
# Config-server và MySQL sẽ show "healthy"
```

---

## 6. Daily Development Workflow

### Restart một service sau khi sửa code

```bash
# 1. Rebuild JAR
cd FPM_Project/Backend/user-auth-service
mvn clean package -DskipTests

# 2. Rebuild Docker image và restart container
cd FPM_Project/Backend
docker compose up user-auth-service --build -d

# Theo dõi logs
docker logs fpm-user-auth-service -f
```

### Restart toàn bộ stack

```bash
# Dừng tất cả
docker compose down

# Start lại (không xóa volumes — data được giữ lại)
docker compose up -d

# Xóa hoàn toàn (kể cả data)
docker compose down -v
```

### Xem logs

```bash
# Log của một service
docker logs fpm-user-auth-service -f --tail 100

# Log nhiều services cùng lúc
docker compose logs -f user-auth-service wallet-service

# Log infrastructure
docker logs fpm-mysql -f
docker logs fpm-kafka -f
```

---

## 7. Service Health Verification

### Eureka Dashboard

Truy cập: **http://localhost:8761**

> 💡 **Eureka Dashboard** — Giao diện web hiển thị tất cả services đã đăng ký. Mỗi service cần thấy trạng thái **UP** tại đây mới có thể nhận traffic.

**Credentials:** `admin / admin`

Services cần thấy trong dashboard:
```
API-GATEWAY           (1 instance UP)
USER-AUTH-SERVICE     (1 instance UP)
WALLET-SERVICE        (1 instance UP)
TRANSACTION-SERVICE   (1 instance UP)
REPORTING-SERVICE     (1 instance UP)
NOTIFICATION-SERVICE  (1 instance UP)
```

### Actuator Health Endpoints

```bash
# Kiểm tra từng service
curl http://localhost:8081/actuator/health  # user-auth
curl http://localhost:8082/actuator/health  # wallet
curl http://localhost:8083/actuator/health  # transaction
curl http://localhost:8084/actuator/health  # reporting
curl http://localhost:8085/actuator/health  # notification
curl http://localhost:8080/actuator/health  # api-gateway

# Response mong đợi:
{"status":"UP"}
```

### RabbitMQ Management UI

Truy cập: **http://localhost:15672**  
Credentials: `admin / admin`

Kiểm tra:
- Exchanges: `wallet.exchange`, `transaction.exchange` phải có
- Queues: `wallet.created.queue` phải có consumers

### Config Server — Kiểm tra config load

```bash
# Xem config của wallet-service
curl http://localhost:8888/wallet-service/default

# Kết quả: JSON chứa toàn bộ config đã merge
```

### Test API Gateway

```bash
# Test register endpoint (không cần JWT)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test@1234","username":"testuser"}'

# Mong đợi: HTTP 201 với accessToken
```

---

## 8. Troubleshooting Guide

### ❌ Lỗi: Service không connect được MySQL

**Triệu chứng:**
```
Communications link failure
Connection refused: mysql:3306
```

**Nguyên nhân & Fix:**
```bash
# 1. Kiểm tra MySQL có healthy chưa
docker compose ps mysql
# → Phải là: mysql   running (healthy)

# Nếu chưa healthy, xem logs:
docker logs fpm-mysql --tail 50

# 2. Nếu database chưa được tạo, chạy lại init script
docker exec -i fpm-mysql mysql -uroot -proot < sql/init-databases.sql
```

---

### ❌ Lỗi: Service không đăng ký vào Eureka

**Triệu chứng:**
```
Cannot execute request on any known server
Connection refused: eureka-server:8761
```

**Fix:**
```bash
# Kiểm tra Eureka có running không
curl http://localhost:8761/actuator/health

# Kiểm tra env var đúng chưa
docker inspect fpm-user-auth-service | grep EUREKA
# Phải là: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://admin:admin@eureka-server:8761/eureka/
```

---

### ❌ Lỗi: Config Server không load được config

**Triệu chứng:**
```
Could not resolve placeholder 'spring.datasource.url'
```

**Fix:**
```bash
# Kiểm tra Config Server healthy
curl http://localhost:8888/actuator/health

# Test config endpoint trực tiếp
curl http://localhost:8888/user-auth-service/default

# Kiểm tra volume mount
docker exec fpm-config-server ls /app/config/yml_service/
# Phải thấy: user-auth-service.yaml, wallet-service.yaml...
```

---

### ❌ Lỗi: Kafka consumer không nhận được message

**Triệu chứng:**
```
Error connecting to node kafka:9092
```

**Fix:**
```bash
# Kiểm tra Kafka running
docker logs fpm-kafka --tail 30

# Test tạo topic thủ công
docker exec -it fpm-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Kiểm tra env var
docker inspect fpm-transaction-service | grep KAFKA
# Phải là: SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

---

### ❌ Lỗi: 503 Service Unavailable từ Gateway

**Triệu chứng:** API trả về 503 mặc dù service đang chạy.

**Fix:**
```bash
# 1. Kiểm tra service đã registered vào Eureka chưa
# → http://localhost:8761 → xem danh sách

# 2. Kiểm tra Circuit Breaker có đang OPEN không
curl http://localhost:8080/actuator/circuitbreakers

# 3. Chờ 30s để Circuit Breaker reset sang HALF-OPEN
# 4. Restart service bị lỗi
docker compose restart wallet-service
```

---

### ❌ Lỗi: gRPC connection refused

**Triệu chứng:**
```
io.grpc.StatusRuntimeException: UNAVAILABLE: io exception
```

**Fix:**
```bash
# 1. Kiểm tra gRPC port đã được expose chưa
docker compose ps | grep 9090

# 2. user-auth-service gRPC listen port
docker logs fpm-user-auth-service | grep "gRPC server started"

# 3. Kiểm tra Eureka metadata-map có grpc.port không
curl http://localhost:8761/eureka/apps/USER-AUTH-SERVICE | grep grpcPort
```

---

## 9. Port Reference

### Services (Host Machine)

| Service | REST | gRPC | Notes |
|---------|------|------|-------|
| api-gateway | **8080** | — | Entry point duy nhất |
| eureka-server | **8761** | — | Dashboard: /eureka |
| config-server | **8888** | — | Config API |
| user-auth-service | **8081** | 9090 | — |
| wallet-service | **8082** | — | — |
| transaction-service | **8083** | **9093** | — |
| reporting-service | **8084** | — | — |
| notification-service | **8085** | — | — |
| ocr-service | **8086** | — | — |
| ai-service | **8087** | — | — |

### Infrastructure (Host Machine)

| Service | Port | Access |
|---------|------|--------|
| MySQL | **3306** | `mysql -h localhost -P 3306 -u root -proot` |
| Redis | **6379** | `redis-cli -h localhost` |
| Kafka | **29092** | External listeners (từ host) |
| RabbitMQ AMQP | **5672** | — |
| RabbitMQ UI | **15672** | http://localhost:15672 (admin/admin) |

### Docker Internal Network

Các service giao tiếp với nhau qua Docker internal DNS (tên container):

```
mysql:3306          ← DB connections
redis:6379          ← Cache
kafka:9092          ← Kafka (internal)
rabbitmq:5672       ← RabbitMQ
config-server:8888  ← Config
eureka-server:8761  ← Discovery
```

---

## Quick Commands Cheatsheet

```bash
# Start full stack
docker compose up -d

# Stop all (giữ data)
docker compose down

# Stop all + xóa data
docker compose down -v

# Rebuild một service
docker compose up <service-name> --build -d

# Xem status
docker compose ps

# Follow logs
docker logs fpm-<service-name> -f

# Access MySQL
docker exec -it fpm-mysql mysql -uroot -proot

# Access Redis CLI
docker exec -it fpm-redis redis-cli

# List Kafka topics
docker exec fpm-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

> 📌 **Xem thêm:** `03_SERVICES_CATALOG.md` để biết API endpoints của từng service.
