# Thiết Kế Kiến Trúc Giao Tiếp Đa Giao Thức (REST, gRPC, Kafka, RabbitMQ)

Việc kết hợp cả **REST API, gRPC, Kafka, và RabbitMQ** trong một dự án Microservices (như FPM 2025) là một mô hình kiến trúc rất tiên tiến, thường được dùng ở các hệ thống tài chính lớn (Fintech). Mỗi giao thức có thế mạnh riêng và sẽ giải quyết một bài toán cụ thể để đảm bảo hệ thống không bị "thắt cổ chai" (bottleneck).

Dưới đây là cách phân bổ vai trò chuẩn xác nhất cho dự án của bạn:

---

## 1. RESTful API: Giao Tiếp Khách Hàng - Máy Chủ (Client-to-Server)
**Bản chất:** Giao tiếp đồng bộ (Synchronous), dễ đọc, chuẩn phổ biến.
**Nhiệm vụ trong FPM:**
- Dùng cho **Mobile App / Web App** giao tiếp với **API Gateway**.
- **Use-cases:** 
  - Đăng nhập, Đăng ký (User Auth).
  - Lấy danh sách ví (GET `/wallets`).
  - Điền form tạo giao dịch mới (POST `/transactions`).
  - Xem báo cáo biểu đồ (Reporting).
- *Lý do:* Frontend dễ tích hợp, trình duyệt và các framework Mobile đều hỗ trợ tốt HTTP/REST.

---

## 2. gRPC: Giao Tiếp Nội Bộ Tốc Độ Cao (Server-to-Server Synchronous)
**Bản chất:** Giao tiếp đồng bộ cực nhanh qua HTTP/2, payload nhị phân (Protobuf) rất nhỏ.
**Nhiệm vụ trong FPM:**
- Dùng cho các vụ việc đòi hỏi **"Phải có kết quả ngay lập tức để chạy tiếp"** và **Tính nhất quán dữ liệu (ACID)**.
- **Use-cases:**
  - **Xác thực Gateway**: `api-gateway` gọi gRPC `ValidateToken` tới `user-auth-service` trên **mỗi request**. Nếu dùng REST ở đây, hệ thống sẽ bị chậm (latency) gấp 3-4 lần.
  - **Trừ Tiền Ví**: Khi tạo giao dịch ở `transaction-service`, nó gọi gRPC `UpdateBalance` sang `wallet-service` để check xem ví đủ tiền không và trừ luôn. Nếu thành công thì mới lưu giao dịch xuống DB của transaction.
  - **Kiểm tra Quyền**: Chặn user không được xem ví nếu không có quyền chia sẻ (`ValidateWalletAccess`).

---

## 3. Apache Kafka: Luồng Dữ Liệu Thời Gian Thực (Event Streaming)
**Bản chất:** Xử lý bất đồng bộ (Asynchronous) thông lượng cực cao, khả năng lưu trữ (replay) dữ liệu. Pub/Sub scale lớn.
**Nhiệm vụ trong FPM:**
- Dùng để **phân phối dữ liệu (Data Pipeline)** và **đồng bộ trạng thái Event-Driven** giữa các hệ thống không cần phản hồi ngay.
- **Use-cases:**
  - **Đồng bộ Báo Cáo (`reporting-service`)**: Ngay khi giao dịch được tạo, `transaction-service` bắn một event `transaction.created` lên Kafka. `reporting-service` (và nhiều service khác trong tương lai) sẽ Subscribe event này và âm thầm cập nhật dữ liệu biểu đồ vào cơ sở dữ liệu phân tích mà không làm chậm quá trình lưu giao dịch của user.
  - **Audit Logs / System Logs**: Lưu lại toàn bộ lịch sử biến động số dư (Balance Changed) siêu tốc để sau này phục vụ đối soát.

---

## 4. RabbitMQ: Hàng Đợi Tác Vụ & Nhắn Tin (Message Queuing & Routing)
**Bản chất:** Hàng đợi thông điệp thông minh, định tuyến phức tạp, có cơ chế Retry, Retry Delay, và Dead Letter Queue (tin nhắn lỗi).
**Nhiệm vụ trong FPM:**
- Dùng để **Giao việc (Task Routing)** và các hệ thống **dễ gặp lỗi cần Retry**.
- **Use-cases:**
  - **Gửi Thông Báo (`notification-service`)**: Gửi Email / Push Notification / SMS thường dễ bị timeout do bên thứ 3 (như Gmail server). Bắn message vào RabbitMQ queue, nếu gửi lỗi, RabbitMQ tự động đẩy vào queue chờ 5 phút sau gửi lại.
  - **Xử Lý OCR / AI (`ocr-service`)**: User upload ảnh hóa đơn rất nặng. Đẩy việc này vào RabbitMQ. OCR Service rảnh rỗi sẽ bốc ảnh ra quét, xong xuôi mới gọi ngược lại (hoặc bắn tin nhắn lại) báo cho user.
  - **Lên lịch giao dịch định kỳ (Recurring Transactions)**: RabbitMQ có plugin `delayed-message-exchange` rất xịn để hẹn giờ tự động thực hiện các giao dịch lặp lại.

---

### Luồng Chạy Thực Tế (Sơ đồ kết hợp 4 công nghệ - Cập nhật v1)

Tưởng tượng User chụp ảnh hóa đơn và upload lên App:

1. **[REST API]** Mobile gửi ảnh qua POST `/api/v1/ocr/extract`.
2. **[Native OCR]** `ocr-service` dùng Tesseract quét ảnh, trả về JSON kết quả (số tiền, ngày, cửa hàng).
3. **[AI Gemini]** Mobile gửi text đó qua POST `/api/v1/ai/nlp` để Gemini phân loại danh mục (Category) tự động.
4. **[REST API]** User bấm "Xác nhận", Mobile gửi POST `/api/v1/transactions`.
5. **[gRPC]** `transaction-service` gọi gRPC `UpdateBalance` sang `wallet-service`.
6. **[Kafka]** `transaction-service` bắn event `transaction.created`.
7. **[Real-time]** `reporting-service` nhận event Kafka, xóa cache Redis và tính lại Dashboard ngay lập tức.
8. **[Push]** `notification-service` nhận event Kafka, gửi thông báo FCM "Bạn vừa tiêu 200k tại Circle K".

### Lợi ích của kiến trúc Hybrid:
- **Tính nhất quán (gRPC)**: Tiền bạc không bao giờ bị sai lệch.
- **Tốc độ (REST/gRPC)**: Trải nghiệm người dùng mượt mà.
- **Mở rộng (Kafka/MQ)**: Dễ dàng thêm tính năng mới (như Rewards service) chỉ bằng cách Subscribe Kafka mà không sửa code cũ.

---
**Internal Documents Link:**
- [Internal Communication Matrix](file:///d:/WorkSpace/App_Dev/FPM_Project/Backend/Documents/Internal_Communication_Matrix.md)
- [gRPC Details Review](file:///d:/WorkSpace/App_Dev/FPM_Project/Backend/Documents/Grpc_Usage_Review.md)
- [REST API Full Catalog](file:///d:/WorkSpace/App_Dev/FPM_Project/Backend/Documents/document_api.md)
