# 🛠️ FPM-2025: Internal Communication Matrix (gRPC, Kafka, RabbitMQ)

Tài liệu này tổng hợp toàn bộ các luồng giao tiếp nội bộ giữa các Microservices trong hệ thống FPM-2025.

---

## 1. gRPC Communication (Đồng bộ - Cần phản hồi ngay)
Sử dụng **Protobuf over HTTP/2** để đảm bảo tốc độ cao và type-safety.

| Source Service | Target Service | RPC Method | Purpose (Vai trò & Tác dụng) |
| :--- | :--- | :--- | :--- |
| `api-gateway` | `user-auth` | `ValidateToken` | Xác thực JWT token trên mỗi request từ Client. Đảm bảo an ninh hệ thống. |
| `api-gateway` | `user-auth` | `GetUserById` | Lấy thông tin user để đính kèm vào Header trước khi forward request. |
| `transaction` | `wallet` | `GetWalletById` | Kiểm tra sự tồn tại và loại ví trước khi tạo giao dịch. |
| `transaction` | `wallet` | `UpdateBalance` | **Quan trọng:** Trực tiếp trừ/cộng tiền vào ví. Đảm bảo tính nhất quán dữ liệu tài chính. |
| `transaction` | `wallet` | `ValidateWalletAccess` | Kiểm tra User có quyền chi tiêu từ ví này không (bao gồm cả Shared Wallet). |
| `reporting` | `wallet` | `GetWalletsByUserId` | Lấy danh sách ví để tổng hợp báo cáo theo từng ví. |

---

## 2. Kafka Events (Bất đồng bộ - Pub/Sub Event Streaming)
Dùng cho các luồng dữ liệu thời gian thực và đồng bộ trạng thái hệ thống.

| Topic | Publisher | Subscribers | Event Payload | Effect (Tác dụng) |
| :--- | :--- | :--- | :--- | :--- |
| `user.created` | `user-auth` | `wallet`, `notification` | `{userId, email}` | `wallet`: Tạo ví mặc định; `notification`: Gửi mail chào mừng. |
| `transaction.created` | `transaction` | `reporting`, `notification` | `{txId, amount, type, ...}` | `reporting`: Cập nhật biểu đồ; `notification`: Báo biến động số dư. |
| `transaction.updated` | `transaction` | `reporting` | `{txId, newAmount, ...}` | `reporting`: Xóa và tính toán lại cache dashboard. |
| `transaction.deleted` | `transaction` | `reporting` | `{txId}` | `reporting`: Cập nhật lại tổng chi tiêu tháng/năm. |
| `notification.parsed` | `notification`| `transaction` | `{amount, note, bankId}` | `transaction`: Tự động tạo giao dịch từ SMS ngân hàng. |

---

## 3. RabbitMQ Messages (Bất đồng bộ - Task Queues)
Dùng cho các tác vụ nặng hoặc cần cơ chế Retry/Delay.

| Queue Name | Source | Worker | Message Type | Effect (Tác dụng) |
| :--- | :--- | :--- | :--- | :--- |
| `budget.alerts` | `reporting` | `notification` | `BudgetAlertEvent` | Đẩy thông báo FCM khi user tiêu quá 80% hoặc 100% ngân sách. |
| `export.jobs` | `reporting` | `reporting` (internal) | `ExportRequest` | Xử lý xuất file Excel/PDF dung lượng lớn ở background. |

---

## 4. Tổng Kết Vai Trò

### gRPC (The Spine - Trình tự)
Đóng vai trò "xương sống" cho các nghiệp vụ **Transactional**. Nếu gRPC `UpdateBalance` thất bại, giao dịch sẽ không bao giờ được tạo. Điều này đảm bảo tiền của người dùng luôn được kiểm soát chặt chẽ.

### Kafka (The Brain - Phân tích)
Đóng vai trò "bộ não" phân tích dữ liệu. Nó giúp hệ thống Reporting và AI nhận được dữ liệu ngay lập tức mà không làm treo các service chính. Kiến trúc này giúp hệ thống chịu tải cao (High Throughput).

### RabbitMQ (The Hands - Thực thi)
Đóng vai trò "đôi tay" thực hiện các việc vặt: gửi tin nhắn, quét ảnh, xuất báo cáo. Cơ chế Queue giúp các tác vụ này không bị mất nếu service bị sập tạm thời.

---
**Project FPM-2025 - System Architecture Team**
