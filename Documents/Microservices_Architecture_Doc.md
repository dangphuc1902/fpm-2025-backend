# Microservices Architecture & Technology Analysis (Comprehensive Version)

Tài liệu này cung cấp cái nhìn chi tiết nhất về hệ thống FPM-2025, giải thích cặn kẽ vai trò, cơ chế hoạt động và các yếu tố kỹ thuật giúp hệ thống vận hành ổn định.

---

## 1. Tổng quan Kiến trúc (Architecture Overview)

Hệ thống được xây dựng trên mô hình Microservices phân tán, sử dụng hệ sinh thái Spring Cloud để quản lý và điều phối các thành phần.

```mermaid
graph TD
    Client[Mobile/Client App] --> GW[API Gateway - Port 8080]
    
    subgraph Infrastructure
        Eureka[Eureka Server - Service Discovery]
        Config[Config Server - Centralized Config]
        Redis[Redis - Rate Limiting/Caching]
        Kafka[Kafka - Async Messaging]
        RabbitMQ[RabbitMQ - Task Queue]
    end

    subgraph Service Layer
        Auth[User Auth Service]
        Wallet[Wallet Service]
        Trans[Transaction Service]
        Report[Reporting Service]
        Noti[Notification Service]
        OCR[OCR Service]
        AI[AI Service]
    end

    GW --> Auth
    GW --> Wallet
    GW --> Trans
    GW --> Report
    
    Trans -- gRPC --> Wallet
    Trans -- Kafka --> Noti
    Trans -- RabbitMQ --> Report
    
    Wallet -- gRPC --> Auth
    
    Service Layer -.-> Eureka
    Service Layer -.-> Config
    GW -.-> Redis
```

---

## 2. Phân tích chi tiết các Thành phần Kỹ thuật

### 2.1 Service Discovery: Eureka Server
- **Công nghệ**: Spring Cloud Netflix Eureka.
- **Port vận hành**: `8761`.
- **Vai trò chi tiết**: 
    - **Self-Registration (Tự đăng ký)**: Khi bất kỳ microservice nào khởi động (ví dụ: Wallet Service), nó sẽ tự gửi một bản tin "xin chào" kèm theo địa chỉ IP và Port cho Eureka. Eureka sẽ lưu lại thông tin này vào danh mục (Registry).
    - **Heartbeat & Monitoring (Giám sát sức khỏe)**: Mỗi 30 giây (mặc định), các service phải gửi tín hiệu "nhịp đập trái tim" tới Eureka. Nếu Eureka không nhận được tín hiệu sau một khoảng thời gian, nó sẽ tự động gỡ bỏ service đó khỏi danh sách để ngăn các client gọi vào một service đã chết.
    - **Client-Side Discovery Support**: Cung cấp danh sách các địa chỉ khả dụng cho API Gateway và các Service khác để chúng có thể thực hiện Load Balancing (cân bằng tải) giữa các instance.
- **Phân tích sâu**:
    - **Bài toán**: Trong môi trường Cloud/Docker, IP của container là động và không bao giờ cố định. Eureka đóng vai trò như một "nhà mạng" lưu giữ danh bạ điện thoại luôn được cập nhật.
    - **Ưu/Nhược**: Rất mạnh trong việc tự động hóa nhưng có độ trễ nhỏ (30s) có thể gây ra lỗi "Service Unavailable" tạm thời.
    - **Độ phức tạp & Chi phí**: Trung bình thấp về phát triển; Tốn bộ nhớ RAM để duy trì bảng Registry lớn.

### 2.2 API Gateway: Spring Cloud Gateway
- **Công nghệ**: Spring Cloud Gateway (xây dựng trên Project Reactor/Netty).
- **Port vận hành**: `8080`.
- **Vai trò chi tiết**:
    - **Unified Entry Point (Điểm vào duy nhất)**: Toàn bộ Client (Mobile/Web) chỉ giao tiếp với duy nhất Gateway. Gateway che giấu hoàn toàn cấu trúc mạng phức tạp của các service bên trong.
    - **Request Routing (Định tuyến thông minh)**: Dựa trên đường dẫn (Path), Gateway quyết định chuyển tiếp yêu cầu đến service nào (Ví dụ: `/api/v1/wallets/**` sẽ được đẩy sang Wallet Service).
    - **Security Filter (Bộ lọc bảo mật)**: Gateway kiểm tra JWT Token ngay tại cửa ngõ. Nếu token không hợp lệ, yêu cầu bị bác bỏ ngay lập tức mà không cần đi sâu vào hệ thống.
    - **Rate Limiting (Giới hạn lưu lượng)**: Sử dụng Redis để đếm số lượng request của một User. Nếu gọi quá nhanh (ví dụ 100 lần/giây), Gateway sẽ trả về lỗi `429 Too Many Requests`.
- **Phân tích sâu**:
    - **Bài toán**: Ngăn chặn client truy cập trực tiếp vào backend, đồng thời giảm tải cho các service bằng cách thực hiện các nhiệm vụ chung (Auth, Log, Rate Limit) tại một nơi.
    - **Độ phức tạp & Chi phí**: Trung bình cao; Đây là thành phần chịu tải lớn nhất, cần được giám sát chặt chẽ.

### 2.3 Centralized Configuration: Config Server
- **Công nghệ**: Spring Cloud Config.
- **Port vận hành**: `8888`.
- **Vai trò chi tiết**:
    - **Externalized Configuration (Tách biệt cấu hình)**: Toàn bộ cài đặt của hệ thống (Connect DB, API Keys, Logic flags) được lưu trữ tại một repository riêng (thường là Git hoặc Local Path) thay vì nằm trong source code.
    - **Dynamic Refresh (Cập nhật nóng)**: Cho phép thay đổi cấu hình và áp dụng ngay lập tức cho các service (thông qua `/actuator/refresh`) mà không cần phải thực hiện quy trình Build -> Deploy lại toàn bộ service.
    - **Profile Management**: Quản lý các môi trường khác nhau (dev, staging, production) một cách dễ dàng và đồng bộ.
- **Phân tích sâu**:
    - **Bài toán**: Tránh việc phải cấu hình lặp đi lặp lại ở 10 service khác nhau. Đảm bảo tính bảo mật vì code không chứa các thông tin nhạy cảm.
    - **Độ phức tạp & Chi phí**: Thấp; Cực kỳ hiệu quả cho quản trị hệ thống.

### 2.4 High-Performance Communication: gRPC
- **Công nghệ**: Google RPC (Binary Format + Protobuf).
- **Vai trò chi tiết**:
    - **Internal Sync Call (Gọi nội bộ đồng bộ)**: Khi Transaction Service cần kiểm tra số dư của một ví trước khi tạo giao dịch, nó sẽ gọi sang Wallet Service qua gRPC.
    - **Efficiency (Hiệu suất vượt trội)**: Thay vì gửi dữ liệu dạng Text JSON (tốn kém băng thông), gRPC chuyển đổi dữ liệu thành mã nhị phân (Binary). HTTP/2 cho phép truyền nhiều dữ liệu trên cùng một kết nối, giảm độ trễ (Latency).
    - **Strict Contract (Hợp đồng chặt chẽ)**: Sử dụng file `.proto` để định nghĩa cấu trúc dữ liệu. Nếu code của 2 service không khớp với file proto, hệ thống sẽ báo lỗi ngay khi build.
- **Phân tích sâu**:
    - **Bài toán**: Cần tốc độ phản hồi cực nhanh cho các tác vụ then chốt (kiểm tra tiền, kiểm tra quyền).
    - **Độ phức tạp & Chi phí**: Cao; Đòi hỏi team dev phải học cách làm việc với file proto và mã nhị phân.

### 2.5 Message Brokers: Kafka & RabbitMQ
- **Công nghệ**: Apache Kafka (Lưu trữ log sự kiện) & RabbitMQ (Quản lý hàng chờ tác vụ).
- **Vai trò chi tiết**:
    - **Kafka (Event-Driven)**: Lưu trữ các sự kiện quan trọng (Ví dụ: "Giao dịch đã hoàn tất"). Các service khác như Notification hoặc Reporting sẽ "lắng nghe" Kafka để thực hiện các việc tiếp theo một cách độc lập.
    - **RabbitMQ (Task Queue)**: Phân phối các công việc nặng (Ví dụ: "Tạo file báo cáo PDF 1000 trang"). Một worker sẽ lấy công việc này ra xử lý từ từ mà không làm treo ứng dụng chính.
    - **Loose Coupling (Giảm sự phụ thuộc)**: Các service không cần biết về sự tồn tại của nhau, chúng chỉ cần biết "có tin nhắn mới trong hàng chờ".
- **Phân tích sâu**:
    - **Bài toán**: Giải quyết vấn đề nghẽn hệ thống khi có quá nhiều tác vụ cần xử lý cùng lúc và đảm bảo hệ thống không bị mất dữ liệu nếu một service bị sập tạm thời.
    - **Độ phức tạp & Chi phí**: Rất cao; Cần kiến thức sâu về Distributed Transaction và Message Guarantees.

### 2.6 Storage & Performance: MySQL & Redis
- **Công nghệ**: MySQL (SQL) & Redis (NoSQL Key-Value).
- **Vai trò chi tiết**:
    - **MySQL (Dữ liệu bền vững)**: Lưu trữ các thông tin cần sự chính xác tuyệt đối như: Tài khoản người dùng, Lịch sử giao dịch, Số dư ví. Tuân thủ tính chất ACID (Atomicity, Consistency, Isolation, Durability).
    - **Redis (Bộ nhớ tăng tốc)**: 
        - **Distributed Cache**: Lưu các dữ liệu hay dùng (Ví dụ: Tỷ giá ngoại tệ) để không phải truy vấn MySQL thường xuyên.
        - **Distributed Lock**: Đảm bảo tại một thời điểm chỉ có 1 tiến trình được sửa đổi một dữ liệu quan trọng để tránh tranh chấp (Race Condition).
        - **Rate Limiting Counter**: Lưu số lượng request của người dùng trong Gateway.
- **Phân tích sâu**:
    - **Bài toán**: Cân bằng giữa tính an toàn của dữ liệu (MySQL) và tốc độ phản hồi người dùng (Redis).

---

## 3. Tổng kết Phân tích Kỹ thuật

| Chỉ số | Đánh giá | Rủi ro tiềm ẩn |
| :--- | :--- | :--- |
| **Độ ổn định** | Rất tốt | Phụ thuộc vào gRPC và Eureka để các service "tìm thấy" nhau. |
| **Khả năng mở rộng** | Tuyệt vời | Có thể chạy 100 instance Wallet Service mà không cần đổi cấu hình. |
| **Độ khó bảo trì** | Cao | Cần hệ thống Monitoring (như Prometheus/Grafana) để giám sát traffic giữa các service. |
| **Tính bảo mật** | Chặt chẽ | Bảo mật 2 lớp: JWT tại Gateway và gRPC Security (nếu cấu hình) nội bộ. |

---
*Tài liệu này được biên soạn lại để cung cấp cách giải thích rõ ràng, chi tiết và đầy đủ nhất cho người quản trị dự án.*
