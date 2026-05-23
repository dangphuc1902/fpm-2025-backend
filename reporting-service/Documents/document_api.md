📱 **FPM-2025 Backend – API Documentation & Feature List**  
**Dự án SmartWallet Gia Đình 2025**  
**Tech Stack:** Spring Boot 3.5 + gRPC + Kafka + Redis + PostgreSQL + fpm-libs (fpm-core, fpm-domain, fpm-protocol, fpm-messaging)

| STT | Service             | API Endpoint (REST) / gRPC Service                         | Method | Tính năng chính                                                                                   | Dùng libs nào?                                   | Ghi chú / Status |
|-----|----------------------|-------------------------------------------------------------|--------|----------------------------------------------------------------------------------------------------|--------------------------------------------------|------------------|
| 1   | user-auth-service    | POST /api/v1/auth/register                                  | REST   | Đăng ký tài khoản (email + Google OAuth2)                                                          | fpm-core, fpm-domain, fpm-protocol               | Done             |
|     |                      | POST /api/v1/auth/login                                     | REST   | Đăng nhập → trả JWT                                                                                | fpm-core, fpm-protocol                           | Done             |
|     |                      | POST /api/v1/auth/google                                    | REST   | Login Google OAuth2                                                                                | fpm-core                                        | Done             |
|     |                      | GET /api/v1/users/me                                        | REST   | Lấy thông tin profile user                                                                          | fpm-domain, fpm-protocol                         | Done             |
| 2   | wallet-service       | POST /api/v1/wallets                                        | REST   | Tạo ví mới (cash, card, bank)                                                                      | fpm-core, fpm-domain, fpm-protocol               | Done             |
|     |                      | GET /api/v1/wallets                                         | REST   | Liệt kê ví của user                                                                                | fpm-domain, fpm-protocol                         | Done             |
|     |                      | PUT /api/v1/wallets/{id}                                    | REST   | Cập nhật tên ví, balance                                                                           | fpm-core, fpm-domain                             | Done             |
|     |                      | DELETE /api/v1/wallets/{id}                                 | REST   | Xóa ví                                                                                             | fpm-core                                        | Done             |
|     |                      | POST /api/v1/wallets/shared                                 | REST   | Tạo ví chia sẻ gia đình (gọi sharing-service)                                                      | fpm-protocol, fpm-messaging                      | Upcoming         |
| 3   | transaction-service  | POST /api/v1/transactions                                   | REST   | Ghi nhận giao dịch (expense/income)                                                                | fpm-core, fpm-domain, fpm-messaging, fpm-protocol| Done             |
|     |                      | GET /api/v1/transactions                                    | REST   | Liệt kê giao dịch (filter date, category, wallet)                                                  | fpm-domain, fpm-protocol                         | Done             |
|     |                      | PUT /api/v1/transactions/{id}                               | REST   | Sửa giao dịch                                                                                      | fpm-core, fpm-domain                             | Done             |
|     |                      | DELETE /api/v1/transactions/{id}                            | REST   | Xóa giao dịch                                                                                      | fpm-core                                        | Done             |
|     |                      | POST /api/v1/transactions/voice                             | REST   | Ghi âm → AI phân loại (gọi ai-service)                                                             | fpm-messaging, fpm-protocol                      | Upcoming         |
|     |                      | POST /api/v1/transactions/bank-notification                 | REST   | Nhận thông báo ngân hàng (Momo, VCB, TCB) → tự động tạo transaction                               | fpm-messaging, fpm-protocol                      | Upcoming         |
|     |                      | POST /api/v1/transactions/ocr                               | REST   | Upload bill → OCR → tự động tạo transaction                                                        | fpm-messaging, fpm-protocol                      | Upcoming         |
| 4   | category-service     | GET /api/v1/categories                                      | REST   | Liệt kê danh mục chi tiêu (có sub-category)                                                       | fpm-domain, fpm-protocol                         | Done             |
|     |                      | POST /api/v1/categories/ai-classify                         | REST   | AI tự động phân loại (ăn uống, đi lại, mua sắm...)                                                 | fpm-protocol                                     | Upcoming         |
| 5   | budget-service       | POST /api/v1/budgets                                        | REST   | Đặt ngân sách theo category + tháng                                                                | fpm-core, fpm-domain                             | Done             |
|     |                      | GET /api/v1/budgets                                         | REST   | Xem ngân sách + % đã dùng                                                                          | fpm-domain                                      | Done             |
| 6   | reporting-service    | GET /api/v1/reports/monthly                                 | REST   | Báo cáo chi tiêu tháng (pie chart, line chart)                                                     | fpm-domain, fpm-protocol                         | Done             |
|     |                      | GET /api/v1/reports/insights                                | REST   | AI Insights: dự báo tháng sau, phát hiện bất thường                                                | fpm-protocol                                     | Upcoming         |
|     |                      | GET /api/v1/reports/export/pdf                              | REST   | Export báo cáo PDF                                                                                 | fpm-core                                        | Done             |
|     |                      | GET /api/v1/reports/export/excel                            | REST   | Export báo cáo Excel                                                                               | fpm-core                                        | Done             |
| 7   | sharing-service      | POST /api/v1/families                                       | REST   | Tạo nhóm gia đình                                                                                  | fpm-core, fpm-domain, fpm-messaging              | Upcoming         |
|     |                      | POST /api/v1/families/invite                                | REST   | Mời thành viên (qua mã invite)                                                                     | fpm-messaging                                    | Upcoming         |
|     |                      | GET /api/v1/families/{id}/dashboard                         | REST   | Dashboard chi tiêu theo thành viên                                                                  | fpm-domain, fpm-protocol                         | Upcoming         |
|     |                      | POST /api/v1/transactions/approve                           | REST   | Phê duyệt chi tiêu lớn (> threshold)                                                                | fpm-messaging (FCM push)                         | Upcoming         |
| 8   | notification-service | Kafka Consumer → FCM                                         | Event  | Push thông báo khi có giao dịch mới, vượt ngân sách, cần phê duyệt                                 | fpm-messaging, fpm-protocol                      | Upcoming         |
| 9   | ai-service           | gRPC + REST                                                  | Mixed  | Speech-to-text, NLP classify, anomaly detection, predictive analytics                              | fpm-protocol                                     | Upcoming         |
| 10  | ocr-service          | POST /api/v1/ocr/upload                                     | REST   | Quét bill Bách Hóa Xanh, Vinmart, Grab, Uber                                                       | fpm-messaging                                    | Upcoming         |
______________________________________________________________________________________________________________________________________________________________________________

### Tổng kết tính năng đã hoàn thành (Done)
- Auth (local + Google)
- Quản lý ví cá nhân
- Ghi nhận/sửa/xóa giao dịch
- Danh mục chi tiêu
- Ngân sách
- Báo cáo + export PDF/Excel

### Tính năng sắp hoàn thành (Upcoming – chỉ cần thêm service + proto)
- Chia sẻ ví gia đình / cặp đôi / bạn bè
- Phê duyệt chi tiêu lớn
- Ghi âm → AI phân loại tự động
- Đồng bộ thông báo ngân hàng (Momo, VCB, TCB)
- Quét bill OCR tự động
- AI Insights + dự báo + phát hiện bất thường
- Push notification FCM

**Tất cả API trên đều dùng chung fpm-libs (fpm-core, fpm-domain, fpm-protocol, fpm-messaging)** → cực kỳ sạch và dễ maintain!

