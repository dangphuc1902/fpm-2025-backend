📱 **FPM-2025 Backend – API Documentation & Feature List**  
**Dự án SmartWallet Gia Đình 2025**  
**Tech Stack:** Spring Boot 3.x + gRPC + Kafka + RabbitMQ + Redis + MySQL  
**Cập nhật:** 2026-03-30 (đồng bộ với source code thực tế)

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
|     |                      | gRPC: ValidateToken, GetUserById                 | gRPC   | API Gateway xác thực token nội bộ & thông tin user                 | gRPC (port 9090)| ✅ Done        |
|     |                      | Kafka: user.created                              | Event  | Publish khi user đăng ký → wallet tạo ví mặc định                 | Kafka           | ✅ Done        |
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
|     |                      | gRPC: GetWalletById, UpdateBalance, v.v.         | gRPC   | Xử lý balance cho transaction-service                              | gRPC (port 9091)| ✅ Done        |
|     |                      | Kafka: wallet.created, wallet.updated            | Event  | Publish sự kiện ví                                                  | Kafka           | ✅ Done        |
| 3   | transaction-service  | POST /api/v1/transactions                        | REST   | Tạo giao dịch (gRPC→wallet, Kafka, RabbitMQ)                       | gRPC+Kafka+MQ   | ✅ Done        |
|     |                      | GET /api/v1/transactions/wallet/{walletId}       | REST   | Liệt kê giao dịch theo ví (paged)                                  | -               | ✅ Done        |
|     |                      | GET /api/v1/transactions/{id}                    | REST   | Xem chi tiết giao dịch                                             | -               | ✅ Done        |
|     |                      | GET /api/v1/transactions                         | REST   | Liệt kê giao dịch user (filter, search, paginate)                  | -               | ✅ Done        |
|     |                      | PUT /api/v1/transactions/{id}                    | REST   | Sửa giao dịch                                                      | -               | ✅ Done        |
|     |                      | DELETE /api/v1/transactions/{id}                 | REST   | Xóa giao dịch                                                      | -               | ✅ Done        |
|     |                      | gRPC: TransactionServiceGrpcImpl                 | gRPC   | Cung cấp data cho reporting-service                                | gRPC (port 9092)| ✅ Done        |
|     |                      | Kafka: transaction.created                       | Event  | Publish khi tạo giao dịch thành công                               | Kafka           | ✅ Done        |
| 4   | reporting-service    | GET /api/v1/dashboard                            | REST   | Dashboard tổng quan (Redis cached)                                 | Redis           | ✅ Done        |
|     |                      | GET /api/v1/reports/monthly                      | REST   | Báo cáo chi tiêu tháng                                             | -               | ✅ Done        |
|     |                      | GET /api/v1/reports/export/pdf                   | REST   | Export báo cáo PDF                                                 | -               | ✅ Done        |
|     |                      | GET /api/v1/reports/export/excel                 | REST   | Export báo cáo Excel                                               | -               | ✅ Done        |
|     |                      | GET /api/v1/reports/spending-by-category         | REST   | Biểu đồ tròn chi tiêu theo danh mục                                | -               | ✅ Done        |
|     |                      | GET /api/v1/reports/trends                       | REST   | Xu hướng chi tiêu theo tháng                                       | -               | ✅ Done        |
|     |                      | Kafka consumer: transaction.created              | Event  | Cập nhật TransactionSummary khi có giao dịch mới                   | Kafka           | ✅ Done        |
| 5   | notification-service | POST /api/v1/notifications/receive               | REST   | Nhận thông báo ngân hàng từ Android                                | -               | ✅ Done        |
|     |                      | POST /api/v1/notifications/fcm/register          | REST   | Đăng ký FCM token                                                   | -               | ✅ Done        |
|     |                      | GET /api/v1/notifications/history                | REST   | Lịch sử thông báo                                                   | -               | ✅ Done        |
|     |                      | GET /api/v1/notifications/fcm/status             | REST   | Lấy status FCM                                                     | -               | ✅ Done        |
|     |                      | RabbitMQ & Kafka Event Listener                  | Event  | Consume & parse Bank SMS                                           | Event           | ✅ Done        |
| 6   | category-service     | Category Hub (thuộc Wallet)                      | REST   | Đã gộp vào wallet-service để tối ưu microservices                  | -               | ✅ Merged      |
| 7   | ocr-service          | POST /api/v1/ocr/extract                         | REST   | Upload ảnh hóa đơn → OCR (Tesseract / Stubbed)                     | -               | 🟢 Stub/Done   |
| 8   | ai-service           | POST /api/v1/ai/nlp                              | REST   | NLP text extraction ("tiền cafe 50k")                              | -               | 🟢 Stub/Done   |
|     |                      | POST /api/v1/ai/anomaly                          | REST   | Phát hiện giao dịch bất thường (Anomaly Detection)                 | -               | 🟢 Stub/Done   |

---

## Tổng Kết Tính Năng Đã Tự Động Hóa & Triển Khai

### ✅ Hạng Mục Core Backend Tích Hợp Hoàn Chỉnh
- **Xác thực & Ủy quyền:** Toàn bộ luồng Login, OAuth2, Refresh Token, Blacklist bằng Redis kèm API Gateway Rate Limiting đã xong.
- **Microservices Tái cấu trúc:** Đã loại bỏ module vô thưởng vô phạt (transaction trong wallet, category riêng) -> Hệ thống sạch sẽ, giao tiếp chuẩn gRPC / Kafka.
- **Reporting Tích hợp Cao độ:** Transaction data báo cáo được Sync tự động qua Kafka Events chạy rất mượt. Dữ liệu tổng hợp Dashboard được cache sẵn tại Redis giúp Time-to-First-Byte siêu nhanh.
- **Thông báo đa kênh (Notification Service):** FCM mode đôi (Production / Simulation) đã chạy tốt; Bank Parser regex cho SMS (VCB, MBBank, MoMo) đã sẵn sàng bóc tách transaction thật.
- **Service Phụ trợ AI & OCR:** Tesseract OCR (hóa đơn) và tính năng NLP / Anomaly (AI) đã được dựng sẵn API. Bất cứ khi nào Client gọi, API sẽ chạy stubbing để UI team không bị đợi. Backend Team có thể inject logic TensorFlow/OpenAI thật bất cứ lúc nào.

### 📅 Lộ trình Bước Tiếp (Client Ready)
Toàn bộ phần **Client_Backend_API_Contract** đã thống nhất `Base URL: /api/v1/`. Mobile Client đã đủ dữ liệu Backend để bắt đầu thi công End-To-End không gặp trở ngại.
