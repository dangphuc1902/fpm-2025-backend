# Tài liệu Xử lý Đa tác vụ & Đa luồng (Multi-threading & Task Processing)

Tài liệu này giải thích cách hệ thống FPM-2025 xử lý song song, đồng thời (concurrency) và các cơ chế đa luồng để tối ưu hóa hiệu suất và khả năng đáp ứng.

---

## 1. Các Cấp độ Xử lý Đa tác vụ (Multitasking Levels)

Hệ thống xử lý đa tác vụ ở 3 cấp độ chính:
1.  **Cấp độ Instance (Horizontal Scaling)**: Chạy nhiều bản sao (containers) của cùng một service thông qua Docker. Mỗi instance xử lý một phần lượng tải.
2.  **Cấp độ Quy trình (Process Level)**: Sử dụng các cơ chế bất đồng bộ (Async) để giải hóng luồng chính (Main thread).
3.  **Cấp độ Hàng đợi (Messaging Level)**: Đưa các tác vụ nặng vào Message Broker (Kafka/RabbitMQ) để các worker xử lý song song ở background.

---

## 2. Cơ chế Đa luồng trong Spring Boot (Spring Async)

Dự án sử dụng giải pháp **Spring Task Execution** để xử lý các hàm không cần trả về kết quả ngay lập tức.

### 2.1 Cấu hình Thread Pool
Hệ thống sử dụng `ThreadPoolTaskExecutor` để quản lý vòng đời của các Thread, tránh việc khởi tạo Thread mới quá nhiều gây tốn tài nguyên.

- **Core Pool Size**: Số lượng thread tối thiểu luôn duy trì (ví dụ: 5).
- **Max Pool Size**: Số lượng thread tối đa có thể mở rộng khi tải cao (ví dụ: 10).
- **Queue Capacity**: Hàng chờ cho các task khi toàn bộ thread đang bận (ví dụ: 100).

### 2.2 Annotation `@Async`
Được sử dụng tại các Service xử gửi email, thông báo hoặc xuất dữ liệu:
```java
@Async("eventPublisherExecutor")
public void publishEvent(DomainEvent event) {
    // Logic xử lý gửi sự kiện diễn ra ở luồng riêng biệt
}
```
**Lợi ích**: User không phải đợi quá trình gửi thông báo hoàn tất để nhận được phản hồi "Thành công".

---

## 3. Xử lý Concurrency trong Message Brokers

### 3.1 Kafka Consumer Threading
Các Consumer (như trong **Notification Service**) được cấu hình để xử lý tin nhắn song song:
- **Concurrency level**: Cho phép chạy nhiều luồng lắng nghe trên cùng một Topic (dựa trên số lượng Partitions).
- **Nó giải quyết bài toán gì?**: Khi có hàng triệu thông báo cần gửi, hệ thống có thể tăng số lượng luồng xử lý để tiêu thụ hết tin nhắn trong Kafka nhanh nhất có thể.

### 3.2 RabbitMQ Consumer
Sử dụng `SimpleMessageListenerContainer` để điều phối số lượng worker xử lý các queue tác vụ báo cáo.

---

## 4. Mô hình Non-blocking (API Gateway)

Khác với các microservice thông thường sử dụng "Thread per Request" (mỗi request 1 thread), **API Gateway** sử dụng **Spring WebFlux (Netty)**.

- **Cơ chế**: Event Loop Model.
- **Đặc điểm**: Một số ít thread (thường bằng số nhân CPU) xử lý hàng ngàn kết nối đồng thời nhờ cơ chế non-blocking I/O.
- **Ưu điểm**: Cực kỳ tiết kiệm RAM và có khả năng chịu tải (throughput) cao hơn nhiều so với mô hình truyền thống.

---

## 5. Quản lý Tài nguyên & An toàn Đa luồng

### 5.1 Connection Pooling (HikariCP)
Mỗi service quản lý một danh sách các kết nối Database (Database Connection Pool). Việc này giúp các luồng (threads) có sẵn kết nối để dùng mà không phải thiết lập lại từ đầu.

### 5.2 Thread Safety
- Sử dụng các cấu trúc dữ liệu an toàn (ví dụ: `ConcurrentHashMap`) khi chia sẻ dữ liệu giữa các luồng.
- **Lưu ý quan trọng**: Tránh sử dụng biến tĩnh (`static variables`) để lưu thông tin trạng thái của User vì có thể gây rò rỉ dữ liệu giữa các luồng khác nhau.

---

## 6. Tổng kết Đánh giá

| Thành phần | Cơ chế chính | Ưu điểm |
| :--- | :--- | :--- |
| **User Auth** | Thread-per-request | Cách làm truyền thống, dễ debug. |
| **API Gateway** | Event Loop (Non-blocking) | Hiệu năng cực cao, ít tốn RAM. |
| **Messaging** | Parallel Consumers | Dễ dàng scale theo số lượng bản tin. |
| **Reporting** | Thread Pool Async | Không làm treo hệ thống khi xử lý dữ liệu lớn. |

---
*Tài liệu này mô tả cách hệ thống tận dụng sức mạnh của CPU đa nhân để xử lý hàng ngàn tác vụ đồng thời.*
