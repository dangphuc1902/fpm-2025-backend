# FPM-2025: API Testing Guide — Toàn Diện (Backend + Client)

> **Phiên bản:** 3.0 — Viết lại dựa trên source code thực tế  
> **Cập nhật:** 2026-05-22  
> **Phạm vi:** Toàn bộ 8 Microservices qua API Gateway `:8080`  
> **Mục tiêu:** Manual Test + Automation Test + Kiểm tra Business Rules + Lỗi Backend & Client

---

## 📋 Mục Lục

1. [Chuẩn Bị Môi Trường](#1-chuẩn-bị-môi-trường)
2. [Quy Ước Test Case](#2-quy-ước-test-case)
3. [Module AUTH — User Authentication](#3-module-auth--user-authentication)
4. [Module USER — Profile & Preferences](#4-module-user--profile--preferences)
5. [Module FAMILY — Quản Lý Gia Đình](#5-module-family--quản-lý-gia-đình)
6. [Module WALLET — Quản Lý Ví](#6-module-wallet--quản-lý-ví)
7. [Module CATEGORY — Danh Mục Chi Tiêu](#7-module-category--danh-mục-chi-tiêu)
8. [Module TRANSACTION — Giao Dịch](#8-module-transaction--giao-dịch)
9. [Module REPORTING — Báo Cáo & Dashboard](#9-module-reporting--báo-cáo--dashboard)
10. [Module BUDGET — Ngân Sách](#10-module-budget--ngân-sách)
11. [Module NOTIFICATION — Thông Báo](#11-module-notification--thông-báo)
12. [Module OCR — Quét Hóa Đơn](#12-module-ocr--quét-hóa-đơn)
13. [Module AI — Trợ Lý Thông Minh](#13-module-ai--trợ-lý-thông-minh)
14. [Integration Test — End-to-End Flows](#14-integration-test--end-to-end-flows)
15. [Security & Gateway Test](#15-security--gateway-test)
16. [Automation Test (Postman Scripts)](#16-automation-test-postman-scripts)
17. [Troubleshooting & Lỗi Thường Gặp](#17-troubleshooting--lỗi-thường-gặp)

---

## 1. Chuẩn Bị Môi Trường

### 1.1 Khởi Động Backend

```bash
cd Backend/
docker-compose up -d

# Chờ tất cả services healthy (60-90 giây)
docker ps --format "table {{.Names}}\t{{.Status}}"
```

**Kiểm tra services đang chạy:**

| Container | Port | Kiểm tra |
|-----------|------|----------|
| `fpm-api-gateway` | `8080` | `GET http://localhost:8080/actuator/health` |
| `fpm-user-auth-service` | `8081` | `GET http://localhost:8081/api/v1/auth/health` |
| `fpm-wallet-service` | `8082` | `GET http://localhost:8082/actuator/health` |
| `fpm-transaction-service` | `8083` | `GET http://localhost:8083/actuator/health` |
| `fpm-reporting-service` | `8084` | `GET http://localhost:8084/actuator/health` |
| `fpm-notification-service` | `8085` | `GET http://localhost:8085/actuator/health` |
| `fpm-ocr-service` | `8086` | `GET http://localhost:8086/actuator/health` |
| `fpm-ai-service` | `8087` | `GET http://localhost:8087/actuator/health` |
| `fpm-eureka-server` | `8761` | `http://localhost:8761` (Eureka Dashboard) |
| `fpm-mysql` | `3306` | MySQL đang chạy |
| `fpm-redis` | `6379` | Redis ping OK |
| `fpm-kafka` | `29092` | Kafka broker up |
| `fpm-rabbitmq` | `15672` | RabbitMQ Management UI |

### 1.2 Import Postman Collection

1. Mở **Postman**
2. Chọn **Import** → chọn file `Backend/Documents/FPM_2025_Postman_Collection.json`
3. Vào tab **Variables** của collection, đặt:
   - `base_url` = `http://localhost:8080/api/v1`
   - `token` = _(để trống, sẽ điền sau khi login)_
   - `wallet_id` = `1`
   - `transaction_id` = `1`
   - `family_id` = `1`

### 1.3 Cấu Trúc BaseResponse

Mọi API đều trả về cấu trúc chuẩn:

```json
{
  "success": true,
  "messageCode": "SUCCESS",
  "data": { ... },
  "timestamp": "2026-05-22T14:00:00"
}
```

**Lỗi trả về:**

```json
{
  "success": false,
  "messageCode": "ERROR_CODE",
  "data": null,
  "timestamp": "2026-05-22T14:00:00"
}
```

---

## 2. Quy Ước Test Case

Mỗi test case theo format:

| Field | Mô tả |
|-------|-------|
| **TC-ID** | Mã định danh (VD: `AUTH-TC-001`) |
| **Priority** | `P0` (blocker) / `P1` (critical) / `P2` (major) / `P3` (minor) |
| **Type** | `Happy Path` / `Negative` / `Edge Case` / `Security` |
| **BR** | Business Rule liên quan |
| **Expected** | HTTP status + response mong đợi |

---

## 3. Module AUTH — User Authentication

**Base URL:** `POST/GET {{base_url}}/auth/**`  
**Service:** `user-auth-service` (port 8081)  
**Auth required:** ❌ (trừ logout, refresh)

---

### 3.1 Register — Đăng Ký

**Endpoint:** `POST /api/v1/auth/register`

**Request Body:**
```json
{
  "email": "testuser@example.com",
  "password": "Password123!",
  "username": "testuser"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AUTH-TC-001` | P0 | Happy Path | Đăng ký thành công với đầy đủ thông tin hợp lệ | `201 Created` — Trả về `accessToken`, `refreshToken`, `user.id` |
| `AUTH-TC-002` | P0 | Negative | Email đã tồn tại trong DB | `400 Bad Request` — `"Email already exists"` hoặc tương tự |
| `AUTH-TC-003` | P1 | Negative | Email sai format (VD: `notanemail`) | `400 Bad Request` — Validation error |
| `AUTH-TC-004` | P1 | Negative | Password thiếu chữ hoa — `"password123!"` | `400 Bad Request` — Validation error |
| `AUTH-TC-005` | P1 | Negative | Password thiếu ký tự đặc biệt — `"Password123"` | `400 Bad Request` — Validation error |
| `AUTH-TC-006` | P1 | Negative | Password < 8 ký tự — `"Pa1!"` | `400 Bad Request` — Validation error |
| `AUTH-TC-007` | P2 | Edge Case | Email chứa ký tự uppercase `"Test@Example.COM"` — phải xử lý lowercase | `201 Created` hoặc `400` (check normalize) |
| `AUTH-TC-008` | P2 | Edge Case | `username` bị trùng với user đã tồn tại | `400 Bad Request` |
| `AUTH-TC-009` | P1 | Negative | Body rỗng `{}` | `400 Bad Request` — Missing required fields |
| `AUTH-TC-010` | P2 | Negative | `email` field là `null` trong JSON | `400 Bad Request` |

**Automation Script (Postman Tests tab):**
```javascript
// Dán vào Tests tab của request "Register"
pm.test("Status is 201", function() {
    pm.response.to.have.status(201);
});

pm.test("Response has accessToken", function() {
    var json = pm.response.json();
    pm.expect(json.data).to.have.property("accessToken");
    pm.expect(json.data.accessToken).to.not.be.empty;
});

pm.test("Response has refreshToken", function() {
    var json = pm.response.json();
    pm.expect(json.data).to.have.property("refreshToken");
});

// Tự động lưu token vào biến
var json = pm.response.json();
if (json.data && json.data.accessToken) {
    pm.collectionVariables.set("token", json.data.accessToken);
}
```

---

### 3.2 Login — Đăng Nhập

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**
```json
{
  "email": "testuser@example.com",
  "password": "Password123!"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AUTH-TC-011` | P0 | Happy Path | Đăng nhập thành công | `200 OK` — `data.accessToken`, `data.refreshToken` |
| `AUTH-TC-012` | P0 | Negative | Sai password | `401 Unauthorized` |
| `AUTH-TC-013` | P0 | Negative | Email chưa đăng ký | `401 Unauthorized` hoặc `404 Not Found` |
| `AUTH-TC-014` | P0 | Security (BR-SEC-03) | Gửi 6 request login sai liên tiếp từ cùng 1 IP | Lần thứ 6 phải bị `429 Too Many Requests` (Rate limit: 5 lần/5 phút) |
| `AUTH-TC-015` | P1 | Negative | Body thiếu `password` | `400 Bad Request` |
| `AUTH-TC-016` | P2 | Edge Case | Password đúng nhưng user bị `is_active = false` | `403 Forbidden` hoặc `401` |

**Automation Script:**
```javascript
pm.test("Login OK", function() {
    pm.response.to.have.status(200);
});

var json = pm.response.json();
pm.test("Has accessToken", function() {
    pm.expect(json.data.accessToken).to.be.a("string");
});

// Auto-set token
pm.collectionVariables.set("token", json.data.accessToken);
pm.collectionVariables.set("refresh_token", json.data.refreshToken);
console.log("Token saved:", json.data.accessToken.substring(0, 20) + "...");
```

---

### 3.3 Google OAuth2 Login

**Endpoint:** `POST /api/v1/auth/google`

```json
{
  "idToken": "google_id_token_from_firebase_auth_sdk"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AUTH-TC-017` | P1 | Happy Path | Token Google hợp lệ | `200 OK` — Trả về JWT |
| `AUTH-TC-018` | P1 | Negative | `idToken` không hợp lệ/giả mạo | `401 Unauthorized` |
| `AUTH-TC-019` | P2 | Edge Case | Google user đăng nhập lần đầu → kiểm tra auto-tạo ví mặc định | Sau login, gọi `GET /wallets` phải thấy ít nhất 1 ví "Tiền mặt" |

---

### 3.4 Validate Token

**Endpoint:** `POST /api/v1/auth/validate`

**Header:** `Authorization: Bearer {token}`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AUTH-TC-020` | P1 | Happy Path | Token còn hiệu lực | `200 OK` — `valid: true` |
| `AUTH-TC-021` | P1 | Negative | Token hết hạn | `401 Unauthorized` |
| `AUTH-TC-022` | P1 | Negative | Token giả (sai signature) | `401 Unauthorized` |
| `AUTH-TC-023` | P1 | Security (BR-AUTH-06) | Token đã bị logout (trong blacklist Redis) | `401 Unauthorized` |
| `AUTH-TC-024` | P2 | Negative | Header `Authorization` thiếu `Bearer ` prefix | `401 Unauthorized` — "Invalid authorization header" |

---

### 3.5 Logout

**Endpoint:** `POST /api/v1/auth/logout`

**Header:** `Authorization: Bearer {token}`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AUTH-TC-025` | P0 | Happy Path | Logout thành công | `200 OK` |
| `AUTH-TC-026` | P0 | Security (BR-AUTH-06) | Sau logout, dùng lại token cũ để gọi API khác | `401 Unauthorized` — Token đã bị blacklist Redis |
| `AUTH-TC-027` | P2 | Edge Case | Logout hai lần liên tiếp với cùng token | `200 OK` (idempotent) hoặc `401` lần 2 |

---

### 3.6 Refresh Token

**Endpoint:** `POST /api/v1/auth/refresh`

**Header:** `Authorization: Bearer {refresh_token}`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AUTH-TC-028` | P0 | Happy Path | Refresh token hợp lệ | `200 OK` — Token mới `accessToken` |
| `AUTH-TC-029` | P1 | Negative | Refresh token hết hạn | `401 Unauthorized` |
| `AUTH-TC-030` | P1 | Negative | Dùng `accessToken` thay vì `refreshToken` | `401 Unauthorized` |

---

## 4. Module USER — Profile & Preferences

**Base URL:** `{{base_url}}/users/**`  
**Auth required:** ✅ `Authorization: Bearer {token}`

---

### 4.1 Get Profile

**Endpoint:** `GET /api/v1/users/me`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `USER-TC-001` | P0 | Happy Path | Lấy thông tin user đang login | `200 OK` — `{id, email, username, avatar, created_at}` |
| `USER-TC-002` | P0 | Security | Gọi không có token | `401 Unauthorized` |
| `USER-TC-003` | P1 | Security (BR-AUTH-07) | Gọi với `X-User-Id` header tùy ý (giả mạo) | API phải lấy userId từ JWT, không tin `X-User-Id` từ client |

---

### 4.2 Update Profile

**Endpoint:** `PUT /api/v1/users/me`

```json
{
  "username": "newusername",
  "avatar": "https://example.com/avatar.jpg"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `USER-TC-004` | P1 | Happy Path | Cập nhật `username` | `200 OK` — profile mới |
| `USER-TC-005` | P1 | Negative | `username` đã dùng bởi user khác | `400 Bad Request` — conflict |
| `USER-TC-006` | P2 | Edge Case | Cập nhật `username` rỗng `""` | `400 Bad Request` |

---

### 4.3 Get/Update Preferences

**Endpoint:** `GET /PUT /api/v1/users/preferences`

**Request (PUT):**
```json
{
  "language": "vi",
  "currency": "VND",
  "theme": "dark"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `USER-TC-007` | P1 | Happy Path | Lấy preferences của user | `200 OK` — `{language, currency, theme}` |
| `USER-TC-008` | P1 | Happy Path | Cập nhật language → `"en"` | `200 OK` — `language: "en"` |
| `USER-TC-009` | P2 | Negative | Cập nhật `language` với giá trị không hợp lệ `"xx"` | `400` hoặc store và trả về (kiểm tra validation có hay không) |
| `USER-TC-010` | P2 | Edge Case | User mới chưa có preferences → GET | `200 OK` — trả về null hoặc default values |

---

## 5. Module FAMILY — Quản Lý Gia Đình

**Base URL:** `{{base_url}}/families/**`  
**Auth required:** ✅

---

### 5.1 Create Family

**Endpoint:** `POST /api/v1/families`

```json
{
  "name": "Gia đình nhà A",
  "description": "Gia đình nhỏ của chúng tôi"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `FAM-TC-001` | P0 | Happy Path | Tạo family thành công | `201 Created` — `{id, name, ownerId}` |
| `FAM-TC-002` | P1 | Negative | `name` rỗng | `400 Bad Request` |
| `FAM-TC-003` | P2 | Edge Case | `name` quá dài (> 100 ký tự) | `400 Bad Request` |
| `FAM-TC-004` | P1 | Happy Path | Sau khi tạo, người tạo phải là `OWNER` trong `family_members` | Gọi `GET /families/{id}/members` → thấy `role: "OWNER"` |

**Automation Script:**
```javascript
pm.test("Family created", function() {
    pm.response.to.have.status(201);
    var json = pm.response.json();
    pm.expect(json.data.id).to.be.a("number");
    pm.collectionVariables.set("family_id", json.data.id);
});
```

---

### 5.2 Get Families

**Endpoint:** `GET /api/v1/families`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `FAM-TC-005` | P1 | Happy Path | Lấy danh sách family user tham gia | `200 OK` — List |
| `FAM-TC-006` | P2 | Edge Case | User chưa tạo family nào | `200 OK` — `data: []` |

---

### 5.3 Get Family Members

**Endpoint:** `GET /api/v1/families/{familyId}/members`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `FAM-TC-007` | P1 | Happy Path | Lấy members của family mình sở hữu | `200 OK` — List members |
| `FAM-TC-008` | P1 | Security | Lấy members của family người khác (không phải member) | `403 Forbidden` |
| `FAM-TC-009` | P2 | Negative | `familyId` không tồn tại | `404 Not Found` |

---

### 5.4 Invite Member

**Endpoint:** `POST /api/v1/families/{familyId}/invite`

```json
{
  "email": "member@example.com",
  "role": "MEMBER"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `FAM-TC-010` | P0 | Happy Path | Mời thành viên mới | `201 Created` — `FamilyMemberResponse` |
| `FAM-TC-011` | P1 | Negative | Email người được mời chưa đăng ký trong hệ thống | `404 Not Found` hoặc `400` |
| `FAM-TC-012` | P1 | Negative | Member gọi invite (không phải Owner/Admin) | `403 Forbidden` |
| `FAM-TC-013` | P2 | Edge Case | Mời email đã là thành viên của family | `409 Conflict` hoặc `400` |
| `FAM-TC-014` | P2 | Negative | `role` không hợp lệ (VD: `"SUPERADMIN"`) | `400 Bad Request` |

---

### 5.5 Invitation Flow

**Endpoint:** `GET /api/v1/families/invitations?email={email}`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `FAM-TC-015` | P1 | Happy Path | Xem danh sách lời mời đang PENDING | `200 OK` — List invitations |
| `FAM-TC-016` | P0 | Happy Path | Accept invitation `POST /families/invitations/{id}/accept?email={email}` | `200 OK` — User được thêm vào family |
| `FAM-TC-017` | P1 | Happy Path | Reject invitation `POST /families/invitations/{id}/reject?email={email}` | `200 OK` — Status = REJECTED |
| `FAM-TC-018` | P1 | Negative | Accept invitation đã bị rejected | `400 Bad Request` hoặc `409 Conflict` |
| `FAM-TC-019` | P2 | Security | Accept invitation với email khác (giả mạo) | `403 Forbidden` |

---

## 6. Module WALLET — Quản Lý Ví

**Base URL:** `{{base_url}}/wallets/**`  
**Auth required:** ✅

---

### 6.1 Create Wallet

**Endpoint:** `POST /api/v1/wallets`

```json
{
  "name": "Ví sinh hoạt",
  "type": "CASH",
  "currency": "VND",
  "initialBalance": 5000000,
  "icon": "ic_wallet"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `WAL-TC-001` | P0 | Happy Path | Tạo ví cá nhân loại `CASH` | `201 Created` — `{id, name, balance, type, isActive: true}` |
| `WAL-TC-002` | P1 | Happy Path | Tạo ví `CARD` | `201 Created` |
| `WAL-TC-003` | P1 | Happy Path | Tạo ví `BANK` | `201 Created` |
| `WAL-TC-004` | P1 | Happy Path | Tạo ví gia đình với `familyId` | `201 Created` — `familyId != null` |
| `WAL-TC-005` | P1 | Negative (BR-WALLET-02) | `initialBalance` âm (`-1000`) | `400 Bad Request` — balance phải >= 0 |
| `WAL-TC-006` | P1 | Negative | `name` rỗng | `400 Bad Request` |
| `WAL-TC-007` | P1 | Negative | `type` không hợp lệ (VD: `"CRYPTO"`) | `400 Bad Request` |
| `WAL-TC-008` | P2 | Edge Case | `currency` không hợp lệ (VD: `"XYZ"`) | `400 Bad Request` hoặc store |
| `WAL-TC-009` | P0 | Security (BR-WALLET-03) | Gọi không có token | `401 Unauthorized` |

**Automation Script:**
```javascript
pm.test("Wallet created", function() {
    pm.response.to.have.status(201);
    var json = pm.response.json();
    pm.expect(json.data.id).to.be.a("number");
    pm.expect(json.data.isActive).to.be.true;
    pm.collectionVariables.set("wallet_id", json.data.id);
});
```

---

### 6.2 Get Wallets

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `WAL-TC-010` | P0 | Happy Path | `GET /wallets` | Lấy tất cả ví của user | `200 OK` — List |
| `WAL-TC-011` | P1 | Happy Path | `GET /wallets/active` | Chỉ ví đang active | `200 OK` — chỉ ví `isActive: true` |
| `WAL-TC-012` | P1 | Happy Path | `GET /wallets/type/CASH` | Lọc theo loại | `200 OK` — chỉ CASH |
| `WAL-TC-013` | P1 | Happy Path | `GET /wallets/shared` | Ví được chia sẻ với user | `200 OK` — List |
| `WAL-TC-014` | P1 | Happy Path | `GET /wallets/{id}` | Chi tiết một ví | `200 OK` |
| `WAL-TC-015` | P1 | Security | `GET /wallets/{id}` — ví của người khác | `403 Forbidden` |
| `WAL-TC-016` | P2 | Negative | `GET /wallets/{id}` — id không tồn tại | `404 Not Found` |
| `WAL-TC-017` | P2 | Security (BR-WALLET-04) | `GET /wallets/{id}` với ví đã soft-deleted | `404 Not Found` (bị ẩn bởi `@Where`) |
| `WAL-TC-018` | P1 | Happy Path | `GET /wallets/total-balance` | Tổng số dư | `200 OK` — BigDecimal sum |
| `WAL-TC-019` | P1 | Happy Path | `GET /wallets/count` | Số lượng ví | `200 OK` — số nguyên |
| `WAL-TC-020` | P1 | Happy Path | `GET /wallets/family/{familyId}` | Ví của gia đình | `200 OK` |
| `WAL-TC-021` | P1 | Security | `GET /wallets/family/{familyId}` — không phải thành viên | `403 Forbidden` |

---

### 6.3 Update & Toggle Wallet

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `WAL-TC-022` | P1 | Happy Path | `PUT /wallets/{id}` | Cập nhật tên ví | `200 OK` — tên mới |
| `WAL-TC-023` | P1 | Security | `PUT /wallets/{id}` — ví người khác | `403 Forbidden` |
| `WAL-TC-024` | P0 | Happy Path | `PATCH /wallets/{id}/toggle` | Toggle: active → inactive | `200 OK` — `isActive: false` |
| `WAL-TC-025` | P0 | Happy Path | `PATCH /wallets/{id}/toggle` lần 2 | Toggle: inactive → active | `200 OK` — `isActive: true` |

---

### 6.4 Delete Wallet (Soft Delete)

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `WAL-TC-026` | P0 | Happy Path (BR-WALLET-04) | Xóa ví | `200 OK` — thành công |
| `WAL-TC-027` | P0 | Verification | Sau khi xóa, `GET /wallets/{id}` | `404 Not Found` (soft delete ẩn) |
| `WAL-TC-028` | P1 | Security | Xóa ví của người khác | `403 Forbidden` |
| `WAL-TC-029` | P2 | Edge Case | Kiểm tra DB: record vẫn tồn tại với `is_deleted=1` | Truy vấn MySQL trực tiếp |

---

### 6.5 Share Wallet

**Endpoint:** `POST /api/v1/wallets/{id}/share`

```json
{
  "userId": 2,
  "permissionLevel": "VIEWER"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `WAL-TC-030` | P0 | Happy Path | Chia sẻ ví với `VIEWER` | `201 Created` — `WalletPermissionResponse` |
| `WAL-TC-031` | P1 | Happy Path | Chia sẻ với `EDITOR` | `201 Created` |
| `WAL-TC-032` | P1 | Negative | `userId` không tồn tại | `404 Not Found` |
| `WAL-TC-033` | P1 | Security | Non-owner cố share ví | `403 Forbidden` |
| `WAL-TC-034` | P1 | Happy Path | `GET /wallets/{id}/shares` — danh sách người có quyền | `200 OK` — List permissions |
| `WAL-TC-035` | P1 | Happy Path | `DELETE /wallets/{id}/share/{targetUserId}` — thu hồi quyền | `200 OK` |
| `WAL-TC-036` | P2 | Edge Case | Share ví cho bản thân (owner) | `400 Bad Request` hoặc `409 Conflict` |

---

## 7. Module CATEGORY — Danh Mục Chi Tiêu

**Base URL:** `{{base_url}}/categories/**`  
**Auth required:** ✅

---

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `CAT-TC-001` | P1 | Happy Path | `GET /categories` | Lấy tất cả category | `200 OK` — List |
| `CAT-TC-002` | P1 | Happy Path | `GET /categories/type/EXPENSE` | Chỉ lấy EXPENSE | `200 OK` — chỉ type=EXPENSE |
| `CAT-TC-003` | P1 | Happy Path | `GET /categories/type/INCOME` | Chỉ lấy INCOME | `200 OK` |
| `CAT-TC-004` | P1 | Happy Path | `GET /categories/root` | Chỉ category gốc | `200 OK` — không có parent |
| `CAT-TC-005` | P1 | Happy Path | `GET /categories/root/type/EXPENSE` | Root category loại Expense | `200 OK` |
| `CAT-TC-006` | P1 | Happy Path | `GET /categories/{id}` | Chi tiết category | `200 OK` |
| `CAT-TC-007` | P1 | Happy Path | `GET /categories/{id}/with-children` | Category kèm subcategory | `200 OK` — nested |
| `CAT-TC-008` | P1 | Happy Path | `GET /categories/{parentId}/sub-categories` | Sub-categories của parent | `200 OK` |
| `CAT-TC-009` | P2 | Negative | `GET /categories/{id}` — id không tồn tại | `404 Not Found` |
| `CAT-TC-010` | P2 | Negative | `GET /categories/type/INVALID` | `400 Bad Request` |

**Tạo Category:**
```json
// POST /api/v1/categories
{
  "name": "Ăn uống",
  "type": "EXPENSE",
  "icon": "ic_food",
  "color": "#FF5733"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `CAT-TC-011` | P1 | Happy Path | Tạo category mới EXPENSE | `201 Created` |
| `CAT-TC-012` | P1 | Negative (BR-CAT-01) | `type` không phải INCOME/EXPENSE | `400 Bad Request` |
| `CAT-TC-013` | P1 | Happy Path | Xóa category | `200 OK` |

---

## 8. Module TRANSACTION — Giao Dịch

**Base URL:** `{{base_url}}/transactions/**`  
**Auth required:** ✅  
**⚠️ Quan trọng:** Tạo transaction trigger gRPC + Kafka — test cẩn thận!

---

### 8.1 Create Transaction

**Endpoint:** `POST /api/v1/transactions`

```json
{
  "walletId": 1,
  "categoryId": 1,
  "amount": 50000,
  "type": "EXPENSE",
  "note": "Ăn sáng",
  "transactionDate": "2026-05-22T08:00:00"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `TXN-TC-001` | P0 | Happy Path | Tạo giao dịch EXPENSE thành công | `201 Created` — balance ví giảm |
| `TXN-TC-002` | P0 | Verification | Sau TC-001: `GET /wallets/{id}` — balance giảm đúng amount | balance_mới = balance_cũ - 50000 |
| `TXN-TC-003` | P0 | Happy Path | Tạo giao dịch INCOME | `201 Created` — balance ví tăng |
| `TXN-TC-004` | P0 | Verification | Sau TC-003: balance tăng đúng amount | balance_mới = balance_cũ + amount |
| `TXN-TC-005` | P0 | Negative (BR-TXN-02) | EXPENSE khi `amount > wallet.balance` (ví ít tiền) | `400 Bad Request` — "Insufficient balance" |
| `TXN-TC-006` | P1 | Negative (BR-TXN-01) | `amount = 0` | `400 Bad Request` |
| `TXN-TC-007` | P1 | Negative (BR-TXN-01) | `amount < 0` (âm) | `400 Bad Request` |
| `TXN-TC-008` | P1 | Negative | `walletId` không tồn tại | `404 Not Found` |
| `TXN-TC-009` | P1 | Security | `walletId` là ví của người khác (không có quyền) | `403 Forbidden` |
| `TXN-TC-010` | P1 | Negative (BR-WALLET-05) | Tạo transaction trên ví `isActive = false` | `400 Bad Request` — "Wallet is inactive" |
| `TXN-TC-011` | P1 | Negative (BR-TXN-06) | `transactionDate` > hiện tại + 30 ngày | `400 Bad Request` |
| `TXN-TC-012` | P2 | Edge Case | `transactionDate` trong quá khứ xa (1 năm trước) | `201 Created` (không có giới hạn past?) |
| `TXN-TC-013` | P1 | Kafka (BR-TXN-07) | Sau khi tạo TXN thành công → kiểm tra Kafka topic `transaction.created` | Event xuất hiện trong topic |

**Automation Script:**
```javascript
pm.test("Transaction created - 201", function() {
    pm.response.to.have.status(201);
});

var json = pm.response.json();
pm.test("Has transaction ID", function() {
    pm.expect(json.data.id).to.be.a("number");
    pm.collectionVariables.set("transaction_id", json.data.id);
});

pm.test("Type is correct", function() {
    pm.expect(json.data.type).to.equal("EXPENSE");
});
```

---

### 8.2 List Transactions (với Filters)

**Endpoint:** `GET /api/v1/transactions`

| TC-ID | Priority | Loại | Params | Expected |
|-------|----------|------|--------|----------|
| `TXN-TC-014` | P0 | Happy Path | `page=0&size=20` | `200 OK` — Page object |
| `TXN-TC-015` | P1 | Happy Path | `walletId={id}` | Chỉ transactions của ví đó |
| `TXN-TC-016` | P1 | Happy Path | `type=EXPENSE` | Chỉ EXPENSE |
| `TXN-TC-017` | P1 | Happy Path | `type=INCOME` | Chỉ INCOME |
| `TXN-TC-018` | P1 | Happy Path | `categoryId={id}` | Chỉ category đó |
| `TXN-TC-019` | P1 | Happy Path | `startDate=2026-01-01T00:00:00&endDate=2026-05-31T23:59:59` | Trong khoảng ngày |
| `TXN-TC-020` | P2 | Edge Case | `page=999&size=20` (trang vượt quá) | `200 OK` — `content: []` |
| `TXN-TC-021` | P1 | Security | Filter `walletId` của người khác | `[]` hoặc `403` (chỉ thấy dữ liệu của mình) |

---

### 8.3 Get, Update, Delete Transaction

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `TXN-TC-022` | P1 | Happy Path | `GET /transactions/{id}` | Chi tiết transaction | `200 OK` |
| `TXN-TC-023` | P1 | Security | `GET /transactions/{id}` — của người khác | `403 Forbidden` |
| `TXN-TC-024` | P2 | Negative | `GET /transactions/99999` | `404 Not Found` |
| `TXN-TC-025` | P1 | Happy Path (BR-TXN-04) | `PUT /transactions/{id}` — sửa amount | `200 OK` — balance được recalculate |
| `TXN-TC-026` | P1 | Verification | Sau TC-025: `GET /wallets/{id}` balance đúng | balance = balance_cũ ± delta |
| `TXN-TC-027` | P0 | Happy Path (BR-TXN-05) | `DELETE /transactions/{id}` | `200 OK` — balance hoàn trở lại |
| `TXN-TC-028` | P0 | Verification | Sau TC-027: `GET /wallets/{id}` balance được hoàn | balance tăng lại (nếu EXPENSE) |
| `TXN-TC-029` | P1 | Security | `DELETE /transactions/{id}` — của người khác | `403 Forbidden` |

---

### 8.4 Process Bank Notification

**Endpoint:** `POST /api/v1/transactions/notification`

```json
{
  "bankName": "MB Bank",
  "rawMessage": "MB Bank: TK 0123456789 giao dich 50,000 VND. So du 4,950,000 VND",
  "timestamp": "2026-05-22T10:00:00"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `TXN-TC-030` | P1 | Happy Path | Gửi bank SMS hợp lệ | `200 OK` — auto-created transaction |
| `TXN-TC-031` | P2 | Negative | SMS không parse được (nội dung ngẫu nhiên) | `400 Bad Request` hoặc store với `parsedStatus: FAILED` |

---

## 9. Module REPORTING — Báo Cáo & Dashboard

**Base URL:** `{{base_url}}/reports/** ` và `{{base_url}}/dashboard`  
**Auth required:** ✅

---

### 9.1 Dashboard

**Endpoint:** `GET /api/v1/dashboard?yearMonth=2026-05`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `RPT-TC-001` | P0 | Happy Path | Lấy dashboard tháng hiện tại | `200 OK` — `DashboardResponse` |
| `RPT-TC-002` | P0 | Happy Path | Không truyền `yearMonth` — dùng tháng hiện tại | `200 OK` — default tháng hiện tại |
| `RPT-TC-003` | P1 | Performance (BR-REPORT-03) | Gọi lần 1 → chờ 1 giây → gọi lần 2 | Lần 2 nhanh hơn đáng kể (cache Redis 5 phút) |
| `RPT-TC-004` | P2 | Negative | `yearMonth` sai format (VD: `"05-2026"`) | `400 Bad Request` |
| `RPT-TC-005` | P1 | Verification | Sau khi tạo transaction mới → Dashboard cache bị invalidate | Gọi dashboard → data mới được cập nhật |

---

### 9.2 Monthly Report

**Endpoint:** `GET /api/v1/reports/monthly?month=2026-05`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `RPT-TC-006` | P0 | Happy Path | Báo cáo tháng có giao dịch | `200 OK` — `{totalIncome, totalExpense, netIncome, transactionCount}` |
| `RPT-TC-007` | P1 | Edge Case | Báo cáo tháng chưa có giao dịch | `200 OK` — `{totalIncome: 0, totalExpense: 0}` |
| `RPT-TC-008` | P1 | Verification (BR-REPORT-02) | `netIncome = totalIncome - totalExpense` | Kiểm tra phép tính đúng |
| `RPT-TC-009` | P2 | Negative | `month` format sai | `400 Bad Request` |

---

### 9.3 Chart Data

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `RPT-TC-010` | P1 | Happy Path | `GET /reports/spending-by-category?yearMonth=2026-05` | Pie chart data | `200 OK` — List `{categoryName, amount, percentage}` |
| `RPT-TC-011` | P1 | Happy Path | `GET /reports/spending-by-category?type=INCOME` | Pie chart cho INCOME | `200 OK` |
| `RPT-TC-012` | P1 | Happy Path | `GET /reports/trends?months=6` | Line chart 6 tháng | `200 OK` — 6 data points |
| `RPT-TC-013` | P2 | Happy Path | `GET /reports/trends?months=1` | Line chart 1 tháng | `200 OK` — 1 data point |
| `RPT-TC-014` | P2 | Edge Case | `GET /reports/trends?months=0` | `400 Bad Request` hoặc error |
| `RPT-TC-015` | P1 | Happy Path | `GET /reports/budget-comparison?yearMonth=2026-05` | So sánh budget vs thực tế | `200 OK` |

---

### 9.4 Export PDF/Excel

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `RPT-TC-016` | P1 | Happy Path | `GET /reports/export/pdf?month=2026-05` | Download PDF | `200 OK` — `Content-Type: application/pdf` |
| `RPT-TC-017` | P1 | Happy Path | `GET /reports/export/excel?month=2026-05` | Download Excel | `200 OK` — Content-Type xlsx |
| `RPT-TC-018` | P2 | Negative | Tháng chưa có giao dịch — export PDF | `200 OK` với file rỗng hoặc `404` |

---

### 9.5 Async Export Job (BR-REPORT-04)

**Flow:** `POST /export` → nhận `jobId` → `GET /export/{jobId}` polling → `GET /export/{jobId}/download`

```json
// POST /api/v1/reports/export
{
  "format": "PDF",
  "startDate": "2026-05-01",
  "endDate": "2026-05-31"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `RPT-TC-019` | P1 | Happy Path | Submit export job PDF | `200 OK` — `data: {jobId}` |
| `RPT-TC-020` | P1 | Happy Path | `GET /reports/export/{jobId}` ngay sau đó | `status: "PENDING"` hoặc `"PROCESSING"` |
| `RPT-TC-021` | P1 | Happy Path | Polling đến khi `status = "DONE"` | `200 OK` — `{status: "DONE", fileUrl: "..."}` |
| `RPT-TC-022` | P1 | Happy Path | `GET /reports/export/{jobId}/download` | File download |
| `RPT-TC-023` | P1 | Negative | Download khi `status != "DONE"` | `400 Bad Request` |
| `RPT-TC-024` | P2 | Security | Lấy job của người khác | `403 Forbidden` |

---

## 10. Module BUDGET — Ngân Sách

**Base URL:** `{{base_url}}/budgets/**`  
**Auth required:** ✅

---

### 10.1 Create Budget

**Endpoint:** `POST /api/v1/budgets`

```json
{
  "categoryId": 1,
  "categoryName": "Ăn uống",
  "amountLimit": 5000000,
  "period": "MONTHLY",
  "yearMonth": "2026-05"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `BUD-TC-001` | P0 | Happy Path | Tạo budget cho Ăn uống tháng 5 | `201 Created` — `{id, amountLimit, amountUsed: 0}` |
| `BUD-TC-002` | P1 | Negative (BR-BUDGET-01) | Tạo budget cho category type INCOME | `400 Bad Request` — Chỉ áp dụng cho EXPENSE |
| `BUD-TC-003` | P1 | Negative | `amountLimit = 0` | `400 Bad Request` |
| `BUD-TC-004` | P1 | Negative | `amountLimit < 0` | `400 Bad Request` |
| `BUD-TC-005` | P2 | Edge Case | Tạo budget cho tháng đã có budget của category đó | `409 Conflict` hoặc `400` |

---

### 10.2 Budget CRUD & Summary

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `BUD-TC-006` | P1 | Happy Path | `GET /budgets` | Danh sách budget active | `200 OK` — List |
| `BUD-TC-007` | P1 | Happy Path | `GET /budgets/summary?yearMonth=2026-05` | Tổng kết ngân sách | `200 OK` — `{totalLimit, totalUsed, warningCount}` |
| `BUD-TC-008` | P1 | Happy Path | `GET /budgets/{id}` | Chi tiết budget | `200 OK` |
| `BUD-TC-009` | P1 | Security | `GET /budgets/{id}` — của người khác | `403 Forbidden` |
| `BUD-TC-010` | P1 | Happy Path | `PUT /budgets/{id}` — tăng limit | `200 OK` — limit mới |
| `BUD-TC-011` | P1 | Happy Path | `DELETE /budgets/{id}` | `200 OK` |

---

### 10.3 Budget Alert Integration Test

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `BUD-TC-012` | P0 | Integration | Tạo budget Ăn uống: 100,000đ → Tạo EXPENSE 80,000đ | Status = `WARNING` (80%), Firebase push notification gửi |
| `BUD-TC-013` | P0 | Integration | Tiếp tục EXPENSE thêm 25,000đ | Status = `OVER_BUDGET` (105%), push notification cảnh báo 100% |
| `BUD-TC-014` | P1 | Verification | Sau các TXN trên: `GET /budgets/{id}` | `amountUsed` cập nhật đúng qua Kafka |

---

## 11. Module NOTIFICATION — Thông Báo

**Base URL:** `{{base_url}}/notifications/**`  
**Auth required:** ✅

---

### 11.1 FCM Token Registration

**Endpoint:** `POST /api/v1/notifications/fcm/register`

```json
{
  "deviceId": "android-device-id-abc123",
  "fcmToken": "firebase_fcm_token_here",
  "deviceType": "ANDROID"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `NOTIF-TC-001` | P0 | Happy Path | Đăng ký FCM token | `200 OK` — `{id, userId, deviceId, isActive: true}` |
| `NOTIF-TC-002` | P1 | Happy Path | Đăng ký token mới cho cùng device (update) | `200 OK` — token được update |
| `NOTIF-TC-003` | P1 | Negative | `fcmToken` rỗng | `400 Bad Request` |
| `NOTIF-TC-004` | P1 | Negative | `deviceId` rỗng | `400 Bad Request` |

---

### 11.2 Bank SMS Parsing

**Endpoint:** `POST /api/v1/notifications/receive`

```json
{
  "packageName": "com.mbbank.mbanking",
  "rawContent": "MB Bank: TK 0123456789 giao dich ghi no 50,000 VND. So du 2,000,000 VND luc 08:00 22/05/2026"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `NOTIF-TC-005` | P0 | Happy Path | Parse MB Bank SMS hợp lệ | `201 Created` — `{bankName: "MB Bank", parsedAmount: 50000, isProcessed: true}` |
| `NOTIF-TC-006` | P1 | Happy Path | Parse Vietcombank SMS | `201 Created` — parsed OK |
| `NOTIF-TC-007` | P1 | Happy Path | Parse MoMo notification | `201 Created` |
| `NOTIF-TC-008` | P1 | Edge Case | Gửi cùng SMS 2 lần (duplicate) | `409 Conflict` — `"already processed"` (idempotent) |
| `NOTIF-TC-009` | P2 | Negative | `rawContent` không phải bank SMS | `201 Created` với `parsedStatus: FAILED` |
| `NOTIF-TC-010` | P1 | Negative | `rawContent` rỗng | `400 Bad Request` |

---

### 11.3 Notification History & Read Status

| TC-ID | Priority | Loại | Endpoint | Mô Tả | Expected |
|-------|----------|------|----------|-------|----------|
| `NOTIF-TC-011` | P1 | Happy Path | `GET /notifications/history?page=0&size=20` | Lịch sử thông báo | `200 OK` — `{content, totalElements, unreadCount}` |
| `NOTIF-TC-012` | P1 | Happy Path | `GET /notifications/unread-count` | Số thông báo chưa đọc | `200 OK` — `{unreadCount: N}` |
| `NOTIF-TC-013` | P1 | Happy Path | `PATCH /notifications/{id}/read` | Đánh dấu đã đọc | `200 OK` |
| `NOTIF-TC-014` | P1 | Verification | Sau TC-013: `GET /notifications/unread-count` | Count giảm 1 |
| `NOTIF-TC-015` | P1 | Happy Path | `PATCH /notifications/read-all` | Đánh dấu tất cả đã đọc | `200 OK` |
| `NOTIF-TC-016` | P1 | Verification | Sau TC-015: `GET /notifications/unread-count` | `unreadCount: 0` |

---

### 11.4 FCM Mode Check

**Endpoint:** `GET /api/v1/notifications/fcm/status`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `NOTIF-TC-017` | P2 | Happy Path | Kiểm tra FCM mode | `200 OK` — `{mode: "PRODUCTION"/"SIMULATION", firebaseConnected: true/false}` |

---

## 12. Module OCR — Quét Hóa Đơn

**Base URL:** `{{base_url}}/ocr/**`  
**Auth required:** ✅  
**Content-Type:** `multipart/form-data`

---

**Endpoint:** `POST /api/v1/ocr/extract`

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `OCR-TC-001` | P0 | Happy Path | Upload ảnh hóa đơn siêu thị rõ nét (JPEG) | `200 OK` — `{amount, merchant, date, success: true}` |
| `OCR-TC-002` | P1 | Happy Path | Upload ảnh hóa đơn nhà hàng | `200 OK` — parsed data |
| `OCR-TC-003` | P1 | Negative | Không attach file | `400 Bad Request` — `"No file provided"` |
| `OCR-TC-004` | P1 | Negative | Upload file không phải ảnh (VD: `.txt`) | `400 Bad Request` hoặc parse fail |
| `OCR-TC-005` | P2 | Edge Case | Ảnh quá nhỏ/mờ | `200 OK` — `{success: false, errorMessage: "Cannot parse"}` hoặc low confidence |
| `OCR-TC-006` | P2 | Edge Case | File ảnh > 5MB | `400 Bad Request` — file too large (nếu có giới hạn) |
| `OCR-TC-007` | P2 | Happy Path | Ảnh hóa đơn tiếng Việt | `200 OK` — text tiếng Việt parse OK (Tesseract `vie` data) |

---

## 13. Module AI — Trợ Lý Thông Minh

**Base URL:** `{{base_url}}/ai/**`  
**Auth required:** ✅  
**Powered by:** Google Gemini 1.5 Flash

---

### 13.1 NLP Analysis

**Endpoint:** `POST /api/v1/ai/nlp`

```json
{
  "text": "Tôi vừa uống cafe hết 50k bằng ví tiền mặt",
  "context": "transaction_extraction"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AI-TC-001` | P0 | Happy Path | Text tiếng Việt rõ ràng có số tiền | `200 OK` — `{amount: 50000, category: "Cafe", type: "EXPENSE"}` |
| `AI-TC-002` | P1 | Happy Path | Text tiếng Anh | `200 OK` — parsed OK |
| `AI-TC-003` | P1 | Negative | `text` rỗng `""` | `400 Bad Request` |
| `AI-TC-004` | P1 | Negative | `text` là `null` | `400 Bad Request` |
| `AI-TC-005` | P2 | Edge Case | Text quá ngắn `"50k"` | `200 OK` — partial parse hoặc low confidence |

---

### 13.2 Anomaly Detection

**Endpoint:** `POST /api/v1/ai/anomaly`

```json
{
  "text": "Giao dịch 50 triệu VND lúc 3 giờ sáng từ nước ngoài"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AI-TC-006` | P1 | Happy Path | Giao dịch bất thường (số tiền lớn, giờ kỳ lạ) | `200 OK` — `{isAnomaly: true, reason: "..."}` |
| `AI-TC-007` | P1 | Happy Path | Giao dịch bình thường | `200 OK` — `{isAnomaly: false}` |
| `AI-TC-008` | P1 | Negative | `text` rỗng | `400 Bad Request` |

---

### 13.3 Financial Chat

**Endpoint:** `POST /api/v1/ai/chat`

```json
{
  "text": "Tôi nên làm gì để tiết kiệm tiền hiệu quả hơn?"
}
```

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `AI-TC-009` | P1 | Happy Path | Câu hỏi tài chính chung | `200 OK` — AI response text |
| `AI-TC-010` | P2 | Happy Path | Câu hỏi về app | `200 OK` — advice |
| `AI-TC-011` | P1 | Negative | `text` rỗng | `400 Bad Request` |

---

## 14. Integration Test — End-to-End Flows

### E2E Flow 1: Người Dùng Mới — Onboarding

```
1. Register → 201 ✅
2. Login → 200 + accessToken ✅
3. GET /wallets → ví "Tiền mặt" mặc định được tạo tự động qua Kafka ✅
4. GET /categories → danh mục hệ thống ✅
5. POST /transactions (EXPENSE 50k) → 201 ✅
6. GET /wallets/{id} → balance giảm 50k ✅
7. GET /dashboard → thấy transaction vừa tạo ✅
```

| TC-ID | Priority | Mô Tả | Expected |
|-------|----------|-------|----------|
| `E2E-TC-001` | P0 | Luồng đăng ký → tạo giao dịch đầu tiên | Tất cả bước pass, balance đúng |
| `E2E-TC-002` | P0 | Xác nhận auto-tạo ví mặc định qua Kafka sau register | `GET /wallets` → 1 ví mặc định |

---

### E2E Flow 2: Gia Đình Chia Sẻ Ví

```
1. UserA Login
2. UserA tạo Family
3. UserA invite UserB (email)
4. UserB Login
5. UserB accept invitation
6. UserA tạo "Ví đi chợ" loại SHARED → share cho UserB (EDITOR)
7. UserB: GET /wallets/shared → thấy ví đi chợ
8. UserB tạo EXPENSE 200k từ ví đi chợ
9. UserA: GET /wallets/{id} → balance giảm 200k
```

| TC-ID | Priority | Mô Tả | Expected |
|-------|----------|-------|----------|
| `E2E-TC-003` | P0 | Luồng gia đình đầy đủ | Tất cả bước pass |
| `E2E-TC-004` | P1 | UserC (không phải member) cố truy cập ví gia đình | `403 Forbidden` |

---

### E2E Flow 3: OCR → AI → Transaction

```
1. Upload ảnh hóa đơn → OCR extract amount, merchant
2. Dùng AI NLP để phân loại category
3. Tạo transaction với data từ OCR + AI
4. Verify balance ví giảm
5. GET /reports/spending-by-category → thấy category mới
```

| TC-ID | Priority | Mô Tả | Expected |
|-------|----------|-------|----------|
| `E2E-TC-005` | P1 | Luồng quét hóa đơn → giao dịch | Balance đúng, category đúng |

---

### E2E Flow 4: Budget Alert

```
1. Tạo Budget: Ăn uống 100,000đ
2. Tạo EXPENSE 85,000đ category Ăn uống
3. GET /budgets/{id} → status = WARNING (85%)
4. GET /notifications/history → thấy push notification cảnh báo
5. Tạo thêm EXPENSE 20,000đ
6. GET /budgets/{id} → status = OVER_BUDGET (105%)
```

| TC-ID | Priority | Mô Tả | Expected |
|-------|----------|-------|----------|
| `E2E-TC-006` | P0 | Luồng cảnh báo ngân sách | Alert gửi đúng threshold |

---

### E2E Flow 5: Export Report

```
1. Tạo nhiều transactions trong tháng
2. POST /reports/export (format: PDF)
3. Polling GET /reports/export/{jobId} đến DONE
4. GET /reports/export/{jobId}/download → file PDF OK
```

| TC-ID | Priority | Mô Tả | Expected |
|-------|----------|-------|----------|
| `E2E-TC-007` | P1 | Export async job đầy đủ | File download thành công |

---

## 15. Security & Gateway Test

### 15.1 API Gateway — JWT Validation

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `SEC-TC-001` | P0 | Security | Gọi bất kỳ protected endpoint không có token | `401 Unauthorized` từ Gateway |
| `SEC-TC-002` | P0 | Security | Token giả (random string) | `401 Unauthorized` |
| `SEC-TC-003` | P0 | Security | Token hợp lệ nhưng đã hết hạn | `401 Unauthorized` |
| `SEC-TC-004` | P0 | Security (BR-AUTH-06) | Token đã logout (trong Redis blacklist) | `401 Unauthorized` |
| `SEC-TC-005` | P1 | Security | Token tạo từ secret sai | `401 Unauthorized` |

---

### 15.2 Rate Limiting

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `SEC-TC-006` | P0 | Rate Limit (BR-SEC-03) | Gửi login sai 5 lần liên tiếp / 5 phút / IP | Lần thứ 6: `429 Too Many Requests` |
| `SEC-TC-007` | P1 | Rate Limit | Chờ 5 phút sau khi bị block → thử lại | Login thành công |
| `SEC-TC-008` | P2 | Rate Limit | Rate limit từ IP khác | Không bị ảnh hưởng (per-IP) |

---

### 15.3 Circuit Breaker

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `SEC-TC-009` | P1 | Circuit Breaker | Stop container `fpm-wallet-service` → gọi `GET /wallets` | `503 Service Unavailable` hoặc fallback response |
| `SEC-TC-010` | P1 | Circuit Breaker | Restart wallet-service → gọi lại | `200 OK` sau khi service healthy |

---

### 15.4 Data Isolation / Authorization

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `SEC-TC-011` | P0 | Security (BR-AUTH-07) | UserA dùng token hợp lệ để truy cập wallet của UserB | `403 Forbidden` |
| `SEC-TC-012` | P0 | Security | UserA dùng token hợp lệ để xóa transaction của UserB | `403 Forbidden` |
| `SEC-TC-013` | P1 | Security | Truyền `X-User-Id` header giả từ Postman client | Gateway phải overwrite header → không ảnh hưởng |

---

### 15.5 Input Validation (BR-SEC-04)

| TC-ID | Priority | Loại | Mô Tả | Expected |
|-------|----------|------|-------|----------|
| `SEC-TC-014` | P1 | Injection | Truyền `"note": "<script>alert('XSS')</script>"` trong transaction | `201 Created` nhưng data được escape, không execute |
| `SEC-TC-015` | P1 | Injection | Truyền `"name": "'; DROP TABLE wallets;--"` | `201 Created` — không bị SQL injection (JPA parameterized) |
| `SEC-TC-016` | P2 | Injection | Email: `"test@email.com'OR'1'='1"` khi register | `400 Bad Request` — email validation fail |
| `SEC-TC-017` | P2 | Edge Case | Truyền `amount: "abc"` (string thay vì number) | `400 Bad Request` — type mismatch |

---

## 16. Automation Test (Postman Scripts)

### 16.1 Pre-request Script — Tự Động Setup

Thêm vào Pre-request Script ở **Collection level**:

```javascript
// Tự động set base_url nếu chưa có
if (!pm.collectionVariables.get("base_url")) {
    pm.collectionVariables.set("base_url", "http://localhost:8080/api/v1");
}

// Log request info
console.log("[PRE-REQ]", pm.request.method, pm.request.url.toString());
```

---

### 16.2 Test Script — Kiểm Tra Chung

Thêm vào Tests tab ở **Collection level** (áp dụng cho mọi request):

```javascript
// Kiểm tra response time
pm.test("Response time < 3000ms", function() {
    pm.expect(pm.response.responseTime).to.be.below(3000);
});

// Kiểm tra Content-Type (trừ binary downloads)
var url = pm.request.url.toString();
if (!url.includes("/download") && !url.includes("/export/pdf") && !url.includes("/export/excel")) {
    pm.test("Content-Type is JSON", function() {
        pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json");
    });
}

// Log status
console.log("[RESPONSE]", pm.response.status, "| Time:", pm.response.responseTime + "ms");
```

---

### 16.3 Collection Runner — Full Regression Test

**Thứ tự chạy Collection Runner:**

1. `Register` (AUTH-TC-001)
2. `Login` (AUTH-TC-011) — auto-save token
3. `Get Profile` (USER-TC-001)
4. `Create Family` (FAM-TC-001)
5. `Create Wallet CASH` (WAL-TC-001) — auto-save wallet_id
6. `Get All Wallets` (WAL-TC-010)
7. `Create Category` (CAT-TC-011)
8. `Create Transaction EXPENSE` (TXN-TC-001) — auto-save transaction_id
9. `Get Transactions` (TXN-TC-014)
10. `Get Dashboard` (RPT-TC-001)
11. `Get Monthly Report` (RPT-TC-006)
12. `Create Budget` (BUD-TC-001)
13. `Register FCM Token` (NOTIF-TC-001)
14. `Receive Bank SMS` (NOTIF-TC-005)
15. `AI NLP` (AI-TC-001)
16. `Logout` (AUTH-TC-025)
17. `Verify token blacklisted` (AUTH-TC-026)

**Chạy bằng Newman (CLI):**

```bash
# Cài Newman
npm install -g newman

# Chạy toàn bộ collection
newman run Backend/Documents/FPM_2025_Postman_Collection.json \
  --environment FPM_ENV.json \
  --reporters cli,json \
  --reporter-json-export test-results.json
```

---

### 16.4 Environment File (FPM_ENV.json)

Tạo file `Backend/Documents/FPM_ENV.json`:

```json
{
  "name": "FPM Local",
  "values": [
    { "key": "base_url", "value": "http://localhost:8080/api/v1", "enabled": true },
    { "key": "token", "value": "", "enabled": true },
    { "key": "refresh_token", "value": "", "enabled": true },
    { "key": "wallet_id", "value": "1", "enabled": true },
    { "key": "transaction_id", "value": "1", "enabled": true },
    { "key": "family_id", "value": "1", "enabled": true },
    { "key": "budget_id", "value": "1", "enabled": true },
    { "key": "test_email", "value": "testuser@fpm.dev", "enabled": true },
    { "key": "test_password", "value": "Password123!", "enabled": true }
  ]
}
```

---

## 17. Troubleshooting & Lỗi Thường Gặp

### 17.1 Lỗi HTTP Phổ Biến

| HTTP Code | Nguyên Nhân | Cách Xử Lý |
|-----------|-------------|------------|
| `401 Unauthorized` | Token hết hạn / sai / blacklisted | Đăng nhập lại, copy token mới vào Variables |
| `403 Forbidden` | Không có quyền truy cập resource | Kiểm tra resource có thuộc user hiện tại không |
| `404 Not Found` | Resource không tồn tại hoặc bị soft-deleted | Kiểm tra ID, kiểm tra soft-delete status |
| `400 Bad Request` | Validation fail / body sai format | Đọc response message, kiểm tra request body |
| `409 Conflict` | Duplicate resource (email, unique constraint) | Dùng email/data khác |
| `429 Too Many Requests` | Rate limit (login 5 lần/5 phút) | Chờ 5 phút hoặc đổi IP |
| `500 Internal Server Error` | Lỗi backend (NullPointer, DB error) | Xem logs: `docker logs fpm-{service-name}` |
| `503 Service Unavailable` | Service down hoặc Circuit Breaker OPEN | Kiểm tra container, xem `docker ps` |

---

### 17.2 Lỗi Kafka / Messaging

| Triệu Chứng | Nguyên Nhân | Cách Kiểm Tra |
|-------------|-------------|---------------|
| Sau register không thấy ví mặc định | Kafka consumer `user.created` chưa xử lý | `docker logs fpm-wallet-service` — tìm "Received user.created" |
| Dashboard không cập nhật sau TXN | Reporting service chưa consume event | `docker logs fpm-reporting-service` — tìm "transaction.created" |
| Push notification không gửi | FCM token chưa register / Firebase disconnected | `GET /notifications/fcm/status` kiểm tra mode |

---

### 17.3 Lỗi gRPC

| Triệu Chứng | Nguyên Nhân | Cách Kiểm Tra |
|-------------|-------------|---------------|
| Tạo transaction bị 500 | gRPC `wallet-service.UpdateBalance` fail | `docker logs fpm-transaction-service` — tìm "gRPC error" |
| `GET /wallets/family/{id}` bị 500 | gRPC `user-auth-service.CheckUserInFamily` fail | Kiểm tra port 9090 của user-auth-service |

---

### 17.4 Lệnh Docker Hữu Ích

```bash
# Xem logs của service cụ thể
docker logs fpm-user-auth-service --tail=100 -f
docker logs fpm-wallet-service --tail=100 -f
docker logs fpm-transaction-service --tail=100 -f
docker logs fpm-reporting-service --tail=100 -f
docker logs fpm-notification-service --tail=100 -f

# Kiểm tra Redis blacklist
docker exec -it fpm-redis redis-cli KEYS "blacklist:*"

# Kiểm tra Kafka topics
docker exec -it fpm-kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Restart một service
docker restart fpm-wallet-service

# Xem tất cả services
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Truy cập MySQL trực tiếp
docker exec -it fpm-mysql mysql -u root -p
# USE wallet_db; SELECT * FROM wallets WHERE user_id = 1;
```

---

### 17.5 Checklist Trước Khi Test

- [ ] Tất cả Docker containers đang chạy (`docker ps`)
- [ ] Eureka Dashboard hiển thị đủ 8 services (`http://localhost:8761`)
- [ ] `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] Postman Collection đã import và `base_url` đúng
- [ ] Redis đang chạy (`docker exec fpm-redis redis-cli ping` → `PONG`)
- [ ] Kafka broker up (xem Eureka hoặc Docker logs)

---

### 17.6 Test Data Chuẩn (Seed Data)

Để test nhất quán, dùng các test user sau:

| Role | Email | Password | Ghi Chú |
|------|-------|----------|---------|
| User A (Owner) | `usera@fpm.dev` | `Password123!` | Owner của Family |
| User B (Member) | `userb@fpm.dev` | `Password123!` | Member được invite |
| User C (Stranger) | `userc@fpm.dev` | `Password123!` | Không liên quan |

---

*Tài liệu này được viết lại dựa trên source code thực tế của toàn bộ 8 Microservices FPM-2025.*  
*Cập nhật: 2026-05-22 | Phiên bản: 3.0*
