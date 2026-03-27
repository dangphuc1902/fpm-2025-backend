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

## Tóm Tắt Luồng Chạy Thực Tế (Sơ đồ kết hợp 4 công nghệ)

Tưởng tượng User bấm nút **"Lưu Giao Dịch"** trên Mobile App:

1. **[REST API]** Mobile gửi HTTP POST `/api/v1/transactions` tới API Gateway.
2. **[gRPC]** API Gateway bắn gRPC hỏi `user-auth-service`: "Token này của thằng nào? Của User 01. Hợp lệ không? Hợp lệ". Xong chuyển Request tới `transaction-service`.
3. **[gRPC]** `transaction-service` mở kết nối gRPC sang `wallet-service`: "Trừ 50k vào ví số 10". Wallet trả lời: "Đã trừ xong".
4. **[Kafka]** `transaction-service` lưu DB xong, bắn sự kiện rợp trời: `{"event": "tx_created", "amount": 50k}` lên Kafka.
5. **[REST API]** `transaction-service` trả HTTP 201 Created về cho Mobile App (User thấy thành công trên màn hình).
6. **(Ngầm) [Kafka]** `reporting-service` chộp được sự kiện trên Kafka, tự động cộng số liệu vào biểu đồ thống kê.
7. **(Ngầm) [RabbitMQ]** Bản thân thằng `transaction-service` song song đó cũng ném 1 viên gạch có dòng chữ "Báo notification" vào RabbitMQ. `notification-service` lấy viên gạch đó ra và Ping Mobile App: "Tài khoản bạn vừa bị trừ 50k".

### Lời Khuyên Cho Dự Án FPM 2025:
Với cấu hình này:
- Bạn sẽ học và ứng dụng được **100% các tech stack khủng nhất** của Backend hiện nay.
- Source Code không bị over-engineering nêú chia vai trò tuân thủ nghiêm ngặt **Kafka (Data streams)** và **RabbitMQ (Task/Worker queues)**.
