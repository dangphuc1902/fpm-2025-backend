# 📱 FPM-2025 Backend – Danh Mục API & Tính Năng Chi Tiết (Full API v1)

> **Cập nhật:** 2026-04-04 (Hậu kiểm source code thực tế)  
> **Tech Stack:** Spring Boot 3.5.5 + Java 21 + MySQL 8.4 + Redis + Kafka + gRPC

---

## 1. Mục Lục API Theo Service

| Service              | Route Prefix           | Port | Giao tiếp nội bộ | Trạng thái |
|----------------------|-----------------------|------|-----------------|------------|
| **`api-gateway`**    | `/` (Entry)           | 8080 | gRPC call Auth  | ✅ Done    |
| **`user-auth-service`** | `/api/v1/auth`, `/api/v1/users`, `/api/v1/families` | 8081 | gRPC Server, Kafka Pub | ✅ Done |
| **`wallet-service`** | `/api/v1/wallets`, `/api/v1/categories` | 8082 | gRPC Server, Kafka Pub/Sub | ✅ Done |
| **`transaction-service`** | `/api/v1/transactions` | 8083 | gRPC Server/Client, Kafka Pub | ✅ Done |
| **`reporting_service`** | `/api/v1/dashboard`, `/api/v1/reports` | 8084 | gRPC Client, Kafka Sub | ✅ Done |
| **`notification-service`** | `/api/v1/notifications` | 8085 | Kafka Pub, MQ Sub | ✅ Done |
| **`ocr-service`**    | `/api/v1/ocr`         | 8086 | REST            | ✅ Xong (Tesseract Native) |
| **`ai-service`**     | `/api/v1/ai`          | 8087 | REST            | ✅ Xong (Gemini Flash) |

---

## 2. Chi Tiết Các Endpoint (Đã Review Code)

### 🔑 Authentication & Users (Auth Service)
- `POST /api/v1/auth/register`: Đăng ký tài khoản.
- `POST /api/v1/auth/login`: Trả JWT & Refresh Token (Rate Limited).
- `POST /api/v1/auth/google`: Login OAuth2 Google.
- `POST /api/v1/auth/validate`: Verify JWT (Gateway call gRPC Auth).
- `POST /api/v1/auth/logout`: Blacklist token trong Redis.
- `GET /api/v1/users/me`: Profile người dùng.
- `POST /api/v1/families`: Tạo nhóm hộ gia đình.
- `GET /api/v1/families/invitations`: Xem danh sách lời mời gia nhập gia đình.
- `POST /api/v1/families/invitations/{id}/accept`: Chấp nhận gia nhập nhóm.
- `POST /api/v1/families/invitations/{id}/reject`: Từ chối lời mời.
- `GET /api/v1/families/{id}/members`: Danh sách thành viên + quyền hạn.
- `POST /api/v1/families/{id}/invite`: Gửi lời mời gia nhập nhóm.

### 💳 Wallet & Categories (Wallet Service)
- `POST /api/v1/wallets`: Tạo ví mới (Tiền mặt, Thẻ, Ngân hàng, Ví dùng chung).
- `GET /api/v1/wallets/active`: Danh sách ví đang hoạt động.
- `GET /api/v1/wallets/shared`: Các ví được gia đình chia sẻ.
- `POST /api/v1/wallets/{id}/share`: Share ví cho thành viên gia đình (theo userId).
- `GET /api/v1/categories`: Lấy toàn bộ danh mục chi tiêu (Category Hub).
- `GET /api/v1/categories/root`: Chỉ lấy danh mục gốc (Food, Transport...).

### 💸 Transactions (Transaction Service)
- `POST /api/v1/transactions`: Tạo giao dịch mới. **Trigger: call gRPC Wallet update Balance, Kafka Pub event.**
- `GET /api/v1/transactions`: Liệt kê giao dịch (Paginate: `page`, `size`).
- `GET /api/v1/transactions/wallet/{walletId}`: Giao dịch theo từng ví.
- `PUT /api/v1/transactions/{id}`: Sửa giao dịch.
- `DELETE /api/v1/transactions/{id}`: Xóa giao dịch + hoàn lại số dư ví qua gRPC.
- `POST /api/v1/transactions/{id}/attachments`: Tải lên ảnh hóa đơn đính kèm.
- `DELETE /api/v1/transactions/{id}/attachments/{attachId}`: Xóa file đính kèm.

### 📊 Reporting & Budget (Reporting Service)
- `GET /api/v1/reports/budget-comparison`: So sánh ngân sách vs thực tế.
- `POST /api/v1/reports/export`: Gửi yêu cầu xuất báo cáo (Async Job).
- `GET /api/v1/reports/export/{jobId}`: Kiểm tra trạng thái Job xuất file.
- `GET /api/v1/reports/export/{jobId}/download`: Tải file kết quả (sau khi STATUS=DONE).

### 🔔 Notification & Bank Parser (Notification Service)
- `POST /api/v1/notifications/receive`: Android app gửi Bank SMS lên.
- `POST /api/v1/notifications/fcm/register`: Lưu FCM Token.
- `GET /api/v1/notifications/history`: Xem lịch sử thông báo đã nhận.
- **Worker**: Parse SMS MBBank, Vietcombank, MoMo cực nhanh (Idempotent 100%).

### 🤖 High-Tech (AI & OCR Service)
- `POST /api/v1/ocr/extract`: Trích xuất số tiền, merchant từ ảnh hóa đơn (VIE support).
- `POST /api/v1/ai/nlp`: Phân tích câu lệnh ví dụ: "Anh vừa tốn 20k cafe" (AI Gemini).
- `POST /api/v1/ai/anomaly`: Phát hiện giao dịch bất thường (AI Gemini).
- `POST /api/v1/ai/chat`: Hội thoại tư vấn tài trợ với trợ lý AI (AI Gemini).

---

## 3. Quy Định Phản Hồi (BaseResponse)
Tất cả các API trả về cấu trúc chuẩn:
```json
{
  "success": true,
  "messageCode": "SUCCESS",
  "data": { ... },
  "timestamp": "2026-04-04T..."
}
```
---
**Project FPM-2025 - Backend Documentation Team**
