📱 **FPM-2025 Backend – API Documentation & Feature List**  
**Dự án SmartWallet Gia Đình 2025**  
**Tech Stack:** Spring Boot 3.x + gRPC + Kafka + RabbitMQ + Redis + MySQL  
**Cập nhật:** 2026-03-27 (đồng bộ với source code thực tế)

---

## API Mapping Theo Service

| STT | Service              | API Endpoint (REST)                              | Method | Tính năng chính                                                    | Protocol nội bộ | Status         |
|-----|----------------------|--------------------------------------------------|--------|--------------------------------------------------------------------|-----------------|----------------|
| 1   | user-auth-service    | POST /api/v1/auth/register                       | REST   | Đăng ký tài khoản (email + password)                              | -               | ✅ Done        |
|     |                      | POST /api/v1/auth/login                          | REST   | Đăng nhập → trả JWT + refresh token                               | -               | ✅ Done        |
|     |                      | POST /api/v1/auth/google                         | REST   | Login Google OAuth2 (token exchange)                              | -               | ✅ Done        |
|     |                      | POST /api/v1/auth/validate                       | REST   | Validate JWT token                                                  | -               | ✅ Done        |
|     |                      | POST /api/v1/auth/logout                         | REST   | Logout + blacklist token (Redis)                                   | -               | ✅ Done        |
|     |                      | POST /api/v1/auth/refresh                        | REST   | Làm mới JWT bằng refresh token                                     | -               | ✅ Done        |
|     |                      | GET /api/v1/users/me                             | REST   | Lấy thông tin profile user hiện tại                                | -               | ✅ Done        |
|     |                      | PUT /api/v1/users/me                             | REST   | Cập nhật profile user                                              | -               | ✅ Done        |
|     |                      | POST /api/v1/families                            | REST   | Tạo gia đình / nhóm mới                                            | -               | ✅ Done        |
|     |                      | GET /api/v1/families                             | REST   | Danh sách gia đình của user                                        | -               | ✅ Done        |
|     |                      | GET /api/v1/families/{id}/members                | REST   | Xem thành viên trong gia đình                                      | -               | ✅ Done        |
|     |                      | POST /api/v1/families/{id}/invite                | REST   | Mời thành viên vào gia đình                                        | -               | ✅ Done        |
|     |                      | gRPC: ValidateToken                              | gRPC   | API Gateway xác thực token nội bộ                                  | gRPC (port 9090)| ❌ Chưa impl   |
|     |                      | gRPC: GetUserById                                | gRPC   | Lấy thông tin user nội bộ                                          | gRPC            | ❌ Chưa impl   |
|     |                      | Kafka: user.created                              | Event  | Publish khi user đăng ký → wallet tạo ví mặc định                 | Kafka           | ❌ Chưa impl   |
| 2   | wallet-service       | POST /api/v1/wallets                             | REST   | Tạo ví mới (CASH, CARD, BANK, SHARED)                              | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets                              | REST   | Liệt kê ví của user                                                | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/active                       | REST   | Liệt kê ví đang active                                             | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/type/{type}                  | REST   | Liệt kê ví theo type                                               | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/shared                       | REST   | Liệt kê ví được người khác share                                   | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/{id}                         | REST   | Xem chi tiết một ví                                                | -               | ✅ Done        |
|     |                      | PUT /api/v1/wallets/{id}                         | REST   | Cập nhật ví                                                        | -               | ✅ Done        |
|     |                      | PATCH /api/v1/wallets/{id}/toggle                | REST   | Bật/tắt ví                                                         | -               | ✅ Done        |
|     |                      | DELETE /api/v1/wallets/{id}                      | REST   | Xóa ví                                                             | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/total-balance                | REST   | Tổng số dư tất cả ví                                               | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/count                        | REST   | Đếm số ví                                                          | -               | ✅ Done        |
|     |                      | POST /api/v1/wallets/{id}/share                  | REST   | Chia sẻ ví cho user khác                                           | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/{id}/shares                  | REST   | Xem danh sách người được share ví                                  | -               | ✅ Done        |
|     |                      | DELETE /api/v1/wallets/{id}/share/{uid}          | REST   | Thu hồi quyền share ví                                             | -               | ✅ Done        |
|     |                      | GET /api/v1/wallets/family/{familyId}            | REST   | Lấy ví theo gia đình                                               | -               | ❌ Chưa impl   |
|     |                      | gRPC: GetWalletById, UpdateBalance, v.v.         | gRPC   | Xử lý balance cho transaction-service                              | gRPC (port 9091)| ✅ Done        |
|     |                      | Kafka: wallet.created, wallet.updated            | Event  | Publish sự kiện ví                                                  | Kafka           | ✅ Done        |
| 3   | transaction-service  | POST /api/v1/transactions                        | REST   | Tạo giao dịch (gRPC→wallet, Kafka, RabbitMQ)                       | gRPC+Kafka+MQ   | ✅ Done        |
|     |                      | GET /api/v1/transactions/wallet/{walletId}       | REST   | Liệt kê giao dịch theo ví (paged)                                  | -               | ✅ Done        |
|     |                      | GET /api/v1/transactions/{id}                    | REST   | Xem chi tiết giao dịch                                             | -               | ✅ Done        |
|     |                      | GET /api/v1/transactions                         | REST   | Liệt kê giao dịch user (filter, search, paginate)                  | -               | ❌ Chưa impl   |
|     |                      | PUT /api/v1/transactions/{id}                    | REST   | Sửa giao dịch                                                      | -               | ❌ Chưa impl   |
|     |                      | DELETE /api/v1/transactions/{id}                 | REST   | Xóa giao dịch                                                      | -               | ❌ Chưa impl   |
|     |                      | GET /api/v1/transactions/search                  | REST   | Tìm kiếm giao dịch (filter)                                        | -               | ❌ Chưa impl   |
|     |                      | POST /api/v1/transactions/voice                  | REST   | Ghi âm → AI phân loại → tạo giao dịch                             | RabbitMQ        | ❌ Upcoming    |
|     |                      | POST /api/v1/transactions/notification           | REST   | Nhận thông báo ngân hàng → tự động tạo giao dịch                  | Kafka           | ❌ Upcoming    |
|     |                      | POST /api/v1/transactions/ocr                    | REST   | Upload bill → OCR → tự động tạo giao dịch                         | RabbitMQ        | ❌ Upcoming    |
|     |                      | gRPC: GetTransactionById, GetByDateRange, v.v.   | gRPC   | Cung cấp data cho reporting-service                                | gRPC (port 9092)| ⚠️ Stub only  |
|     |                      | Kafka: transaction.created                       | Event  | Publish khi tạo giao dịch thành công                               | Kafka           | ✅ Done        |
| 4   | reporting-service    | GET /api/dashboard                               | REST   | Dashboard tổng quan (Redis cached)                                 | Redis           | ✅ Done        |
|     |                      | GET /api/v1/reports/monthly                      | REST   | Báo cáo chi tiêu tháng                                             | -               | ✅ Done        |
|     |                      | GET /api/v1/reports/export/pdf                   | REST   | Export báo cáo PDF                                                 | -               | ✅ Done        |
|     |                      | GET /api/v1/reports/export/excel                 | REST   | Export báo cáo Excel                                               | -               | ✅ Done        |
|     |                      | GET /api/v1/reports/insights                     | REST   | AI Insights (dự báo, bất thường)                                   | -               | ⚠️ Stub only  |
|     |                      | GET /api/v1/reports/spending-by-category         | REST   | Biểu đồ tròn chi tiêu theo danh mục                                | -               | ❌ Chưa impl   |
|     |                      | GET /api/v1/reports/trends                       | REST   | Xu hướng chi tiêu theo tháng                                       | -               | ❌ Chưa impl   |
|     |                      | GET /api/v1/reports/budget-comparison            | REST   | So sánh ngân sách vs thực tế                                       | -               | ❌ Chưa impl   |
|     |                      | POST /api/v1/reports/export (async job)          | REST   | Tạo job export bất đồng bộ                                         | RabbitMQ        | ❌ Chưa impl   |
|     |                      | GET /api/v1/reports/export/{jobId}/download      | REST   | Download file export đã tạo                                        | -               | ❌ Chưa impl   |
|     |                      | Kafka consumer: transaction.created              | Event  | Cập nhật TransactionSummary khi có giao dịch mới                   | Kafka           | ✅ Done        |
| 5   | notification-service | POST /api/notifications/receive                  | REST   | Nhận thông báo ngân hàng từ Android                                | -               | ❌ Chưa impl   |
|     |                      | POST /api/notifications/fcm/register             | REST   | Đăng ký FCM token                                                   | -               | ❌ Chưa impl   |
|     |                      | GET /api/notifications/history                   | REST   | Lịch sử thông báo                                                   | -               | ❌ Chưa impl   |
|     |                      | RabbitMQ consumer: notification.queue            | Event  | Nhận task notification từ transaction-service (log only)           | RabbitMQ        | ⚠️ Partial    |
|     |                      | Kafka publish: notification.parsed               | Event  | Sau khi parse thông báo ngân hàng → gửi cho transaction-service    | Kafka           | ❌ Chưa impl   |
| 6   | category-service     | CRUD /api/v1/categories                          | REST   | Quản lý danh mục chi tiêu                                           | -               | ❌ Module chưa tạo |
|     |                      | POST /api/v1/categories/budgets                  | REST   | Đặt ngân sách                                                       | -               | ❌ Module chưa tạo |
|     |                      | GET /api/v1/categories/{id}/budget-status        | REST   | Xem % ngân sách đã dùng                                             | -               | ❌ Module chưa tạo |
| 7   | ocr-service          | POST /api/ocr/upload                             | REST   | Upload ảnh hóa đơn → OCR                                            | RabbitMQ        | ❌ Module chưa tạo |
| 8   | ai-service           | POST /api/ai/speech-to-text                      | REST   | Chuyển giọng nói thành văn bản                                      | RabbitMQ        | ❌ Module chưa tạo |

---

## Tổng Kết Tính Năng

### ✅ Đã Hoàn Thành (Done)
- Đăng ký / đăng nhập local + Google OAuth2
- JWT + Refresh Token + Logout blacklist
- Quản lý Family (tạo, mời, xem thành viên)
- CRUD Ví (cá nhân + chia sẻ, đủ tính năng nâng cao)
- Tạo giao dịch với tích hợp gRPC + Kafka + RabbitMQ
- Xem danh sách giao dịch theo ví (paged)
- Dashboard + Báo cáo tháng + Export PDF/Excel
- Kafka event pipeline (transaction.created → reporting-service)
- RabbitMQ notification pipeline (transaction-service → notification-service)

### ⚠️ Đang Dở / Cần Bổ Sung
- gRPC Server user-auth-service (chưa có impl)
- CRUD giao dịch (PUT/DELETE/Search)
- gRPC Server transaction-service (stub chưa có logic)
- Reporting: spending-by-category, trends, budget-comparison
- Notification-service: chưa có REST, DB, FCM

### ❌ Chưa Bắt Đầu (Upcoming)
- category-service (CRUD category + Budget + gRPC AutoCategorize)
- ocr-service (scan bill)
- ai-service (voice, NLP, anomaly detection)
- config-service module
- Voice / OCR / Bank-notification flow hoàn chỉnh
