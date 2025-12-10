---

# 1. Chi Tiết Microservices {#services}

---

## 1.1. **config-service (Port: 8888)**

**Format:** YAML

### **Chức năng**

* Centralized configuration
* Environment-specific configs
* Refresh configs without restart

### **Tech**

* Spring Cloud Config Server
* Git repository backend

### **Dependencies**

* spring-cloud-config-server
* spring-boot-actuator

---

## 1.2. **eureka-server (Port: 8761)**

**Format:** YAML

### **Chức năng**

* Service discovery & registration
* Health checks
* Load balancing metadata

### **Tech**

* Spring Cloud Netflix Eureka

### **Dependencies**

* spring-cloud-starter-netflix-eureka-server

---

## 1.3. **api-gateway (Port: 8080)**

**Format:** YAML

### **Chức năng**

* Single entry point
* JWT authentication filter
* Rate limiting
* Request routing
* Circuit breaker

### **Tech**

* Spring Cloud Gateway
* Spring Security
* Redis (rate limiting)

### **Routes**

```
/api/auth/**            → user-service  
/api/wallets/**         → wallet-service  
/api/transactions/**    → transaction-service  
/api/categories/**      → category-service  
/api/reports/**         → reporting-service  
/api/ocr/**             → ocr-service
```

### **Dependencies**

* spring-cloud-starter-gateway
* spring-cloud-starter-circuitbreaker-reactor-resilience4j
* spring-boot-starter-data-redis-reactive

---

## 1.4. **user-service (Port: 8081)**

**Format:** YAML

### **Chức năng**

* User registration/login
* Google OAuth2 integration
* JWT token generation
* Profile management
* Family/Group management
* Member invitations

### **Database:** `user_db`

**Tables**

* `users`
* `families`
* `family_members`
* `refresh_tokens`

### **APIs (REST)**

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/google
POST   /api/auth/refresh
GET    /api/users/me
PUT    /api/users/me
POST   /api/families
GET    /api/families/{id}/members
POST   /api/families/{id}/invite
```

### **gRPC Services**

```
rpc GetUserById(UserIdRequest) returns (UserResponse)
rpc ValidateToken(TokenRequest) returns (TokenValidationResponse)
rpc GetFamilyMembers(FamilyIdRequest) returns (MembersResponse)
```

### **Dependencies**

* spring-boot-starter-web
* spring-boot-starter-security
* spring-boot-starter-oauth2-client
* spring-boot-starter-data-jpa
* postgresql
* jjwt
* grpc-spring-boot-starter

---

## 1.5. **wallet-service (Port: 8082)**

**Format:** YAML

### **Chức năng**

* CRUD wallets
* Balance management
* Multi-currency
* Shared wallets

### **Database:** `wallet_db`

**Tables**

* `wallets`
* `wallet_permissions`

### **APIs (REST)**

```
POST   /api/wallets
GET    /api/wallets
GET    /api/wallets/{id}
PUT    /api/wallets/{id}
DELETE /api/wallets/{id}
GET    /api/wallets/family/{familyId}
```

### **gRPC**

```
rpc GetWalletById(...)
rpc UpdateBalance(...)
rpc ValidateWalletAccess(...)
```

### **Events Published (Kafka)**

* wallet.created
* wallet.updated
* balance.changed

### **Dependencies**

* spring-boot-starter-web
* spring-boot-starter-data-jpa
* postgresql
* grpc-spring-boot-starter
* spring-kafka

---

## 1.6. **transaction-service (Port: 8083)**

**Format:** YAML

### **Chức năng**

* CRUD transactions
* Voice input
* Banking notifications
* OCR integration
* Attachments
* Filtering / searching

### **Database:** `transaction_db`

**Tables**

* `transactions`
* `attachments`
* `recurring_transactions`

### **APIs (REST)**

```
POST   /api/transactions
POST   /api/transactions/voice
POST   /api/transactions/notification
POST   /api/transactions/ocr
GET    /api/transactions
GET    /api/transactions/{id}
PUT    /api/transactions/{id}
DELETE /api/transactions/{id}
GET    /api/transactions/search
```

### **gRPC**

```
rpc GetTransactionById(...)
rpc GetTransactionsByWallet(...)
rpc GetTransactionsByDateRange(...)
```

### **Kafka**

**Publish**

* transaction.created
* transaction.updated
* transaction.deleted

**Consume**

* notification.parsed
* ocr.completed
* category.assigned

### **RabbitMQ**

**Send**

* voice.transcription

### **Dependencies**

* spring-boot-starter-web
* spring-boot-starter-data-jpa
* postgresql
* grpc-spring-boot-starter
* spring-kafka
* spring-boot-starter-amqp
* spring-cloud-starter-openfeign

---

## 1.7. **category-service (Port: 8084)**

### **Chức năng**

* Category management
* AI auto-categorization
* Custom categories
* Budget management
* Icon & color customization

### **Database:** `category_db`

### **REST APIs**

```
POST    /api/categories
GET     /api/categories
GET     /api/categories/{id}
PUT     /api/categories/{id}
DELETE  /api/categories/{id}
POST    /api/categories/budgets
GET     /api/categories/{id}/budget-status
```

### **gRPC**

```
rpc GetCategoryById(...)
rpc AutoCategorize(...)
rpc GetCategoriesByFamily(...)
```

### **Kafka**

**Consume**

* transaction.created

**Publish**

* category.assigned

### **RabbitMQ**

**Consume**

* ai.classification.result

### **Dependencies**

* spring-boot-starter-web
* spring-boot-starter-data-jpa
* postgresql
* grpc-spring-boot-starter
* spring-kafka
* spring-boot-starter-amqp
* spring-data-redis

---

## 1.8. **notification-service (Port: 8085)**

### **Chức năng**

* Receive Android banking notifications
* Parse banking formats
* Extract merchant/amount/time
* Deduplicate
* Push FCM notifications

### **Database:** `notification_db`

### **REST APIs**

```
POST  /api/notifications/receive
POST  /api/notifications/fcm/register
GET   /api/notifications/history
```

### **Kafka Publish**

* notification.parsed

### **RabbitMQ Send**

* fcm.send

### **Dependencies**

* spring-boot-starter-web
* spring-boot-starter-data-jpa
* postgresql
* spring-kafka
* spring-boot-starter-amqp
* firebase-admin

---

## 1.9. **ocr-service (Port: 8086)**

### **Chức năng**

* Image upload
* OCR (Tesseract / Google Vision)
* Extract bill data
* Multi-format support

### **Database:** `ocr_db`

### **REST APIs**

```
POST  /api/ocr/upload
GET   /api/ocr/{requestId}/status
GET   /api/ocr/{requestId}/result
```

### **Kafka Publish**

* ocr.completed

### **RabbitMQ Consume**

* ocr.process.request

### **Dependencies**

* spring-boot-starter-web
* spring-boot-starter-data-jpa
* postgresql
* spring-kafka
* spring-boot-starter-amqp
* tess4j
* google-cloud-vision
* spring-boot-starter-cloud-aws

---

## 1.10. **ai-service (Port: 8087)**

### **Chức năng**

* Speech-to-text
* NLP categorization
* Predictive analytics
* Spending anomaly detection
* Insights generation

### **Database:** `ai_db`

### **REST APIs**

```
POST  /api/ai/speech-to-text
POST  /api/ai/categorize
GET   /api/ai/predict/{userId}/next-month
GET   /api/ai/anomalies/{userId}
GET   /api/ai/insights/{userId}
```

### **RabbitMQ Consume**

* voice.transcription
* ai.classification.request

### **AI Models**

* Google Cloud Speech-to-Text
* Vietnamese BERT
* TensorFlow Lite
* Isolation Forest

### **Dependencies**

* spring-boot-starter-web
* spring-boot-starter-data-jpa
* postgresql
* spring-boot-starter-amqp
* google-cloud-speech
* deeplearning4j / djl
* python-bridge

---

## 1.11. **reporting-service (Port: 8088)**

### **Chức năng**

* Dashboard analytics
* Spending charts
* Trends
* Budget vs actual
* Export PDF/Excel
* Scheduled reports

### **Database:** `reporting_db`

### **REST APIs**

```
GET   /api/reports/dashboard
GET   /api/reports/spending-by-category
GET   /api/reports/trends
GET   /api/reports/budget-comparison
POST  /api/reports/export
GET   /api/reports/export/{jobId}/download
```

### **gRPC Calls**

* transaction-service.GetTransactionsByDateRange
* category-service.GetCategoriesByFamily
* wallet-service.GetWalletById

### **RabbitMQ Send**

* report.generate

### **Dependencies**

* spring-boot-starter-web
* grpc-spring-boot-starter
* spring-boot-starter-amqp
* spring-data-redis
* apache-poi
* openpdf

---

Nếu bạn muốn, tôi có thể:
✅ Generate **table of contents tự động**
✅ Chuyển thành **từng module riêng**
✅ Format sang **Mermaid Diagram** (Service Map)
✅ Generate file `.md` hoàn chỉnh để tải về

Chỉ cần nói **"xuất file markdown"** hoặc **"vẽ diagram"** nhé!
