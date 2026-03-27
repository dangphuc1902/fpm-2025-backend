# Đánh Giá Hiện Trạng và Lộ Trình Hoàn Thiện Dự Án (Roadmap)

Dựa trên bản thiết kế chi tiết tại `microservice_details_func.md`, dưới đây là đánh giá toàn diện về các microservices đã được hoàn thiện, những phần còn thiếu, và kế hoạch (roadmap) để hoàn thành toàn bộ hệ thống FPM 2025.

---

## 1. Trạng Thái Các Microservices Hiện Tại

### Quản lý Hạ Tầng (Infrastructure Services)
- [x] **`eureka-server` (Port 8761)**: Cơ chế đăng ký và phát hiện service (Service Registry) đã vận hành.
- [x] **`api-gateway` (Port 8080)**: Cổng vào của hệ thống đã setup.
- [ ] **`config-service` (Port 8888)**: Cấu hình tập trung thông qua Spring Cloud Config chưa được cấu trúc chuẩn hóa cho toàn dự án.

### Core Business Services
1. **`user-auth-service` (Port 8081)**
   - **Đã làm**: Cấu hình Spring Security, JWT, gRPC Client/Server (`fpm-grpc`, `fpm-proto`), giao diện cơ bản (`UserEntity`, `pom.xml` dependencies).
   - **Chưa hoàn thiện**: OAuth2 Google, Flow Invite vào Family, Quản trị Roles & Permissions đầy đủ.

2. **`wallet-service` (Port 8082)**
   - **Đã làm**: Khởi tạo CRUD cơ bản (`WalletController`, `WalletService`), Kafka Producer/Consumer, gRPC Provider cho `WalletServiceGrpcImpl` (Lấy thông tin ví và Update số dư). Cập nhật API Shared Wallets.
   - **Chưa hoàn thiện**: Caching chi tiết (Redis) cho toàn bộ truy vấn nặng. Auto-scheduler tính tiền tự động định kỳ nếu có.

3. **`reporting-service` (Port 8085)**
   - **Đã làm**: Đã thiết lập khung source. Tích hợp Kafka, PostgreSQL, Lombok, Eureka, gRPC clients.
   - **Chưa hoàn thiện**: Chưa có code Controller, Service tính toán. Phải dùng `TransactionGrpcService`, `CategoryGrpcService` để sinh biểu đồ (Pie, Bar). Chờ 2 service đó hoàn tất mới code được business.

4. **`transaction-service` (Port 8083)**
   - 🔴 **Trạng thái**: CHƯA KHỞI TẠO (Missing module).
   - Core chính của hệ thống. Xử lý logic rút/nạp tiền, phân trang transactions, OCR/Voice trigger.

5. **`category-service` (Port 8084)**
   - 🔴 **Trạng thái**: CHƯA KHỞI TẠO (Missing module).
   - Logic Budget và Category Tree.

6. **`ocr-service` (Port 8086)**
   - 🔴 **Trạng thái**: CHƯA KHỞI TẠO.
   - Xử lý scan bill, bóc tách bằng AI/Regex -> Kafka.

7. **`notification-service` (Port 8087)**
   - 🔴 **Trạng thái**: CHƯA KHỞI TẠO.
   - Nhận biến động số dư từ MB Bank/App Bank (Hook) -> Kafka -> Parsing tự động.

---

## 2. Kế Hoạch Triển Khai Tiếp Theo (Roadmap)

Để đưa dự án về đích một cách khoa học (không gãy logic dependency), chúng ta nên thực hiện theo lộ trình (Roadmap) sau, đi từ tầng Foundation lên tầng Feature:

### **Bước 1: Hoàn tất Dependency Modules (Core + Auth)**
- Đảm bảo `user-auth-service` xuất ra JWT chuẩn chỉnh để các test call (trên Postman) có Authorization Header tốt nhất.
- Đảm bảo `fpm-common`, `fpm-security`, `fpm-domain` (thuộc kho `libs/fpm-libs/`) được biên dịch (build) chuẩn xác. Không báo lỗi pom hay thiếu symbols.

### **Bước 2: Khởi tạo & Code `transaction-service` (ƯU TIÊN CAO)**
- Sinh Module maven `transaction-service`.
- Viết Entity `Transaction`, `RecurringTransaction`.
- Dựng REST API Controller: `/api/transactions` (CRUD cơ bản).
- Móc gRPC từ `transaction` -> `wallet` để trừ/cộng tiền chuẩn xác sau khi tạo transaction.
- Public `TransactionGrpcService` (Cho Reporting Service hứng data lúc sau).

### **Bước 3: Khởi tạo & Code `category-service`**
- Sinh Module `category-service`.
- Viết các Entity `Category`, `Budget`. Móc REST API.
- Cung cấp `CategoryGrpcService.AutoCategorize` để Transaction gọi sang tự xếp nhóm.

### **Bước 4: Hoàn thiện `reporting-service`**
- Vì đã có đủ gRPC data stream từ Transaction và Category, Reporting Service sẽ dễ dàng consume data.
- Code Dashboard Controller ghép lại các kết quả từ database hoặc gRPC query.

### **Bước 5: Phát triển Features Nâng Cao (OCR & Notification)**
- Module này nên làm bằng Python Fast API hoặc Spring Boot + thư viện Tesseract OCR tùy tech stack mong muốn. Đẩy file rác vào Kafka `ocr.completed` để transaction service lụm.

### **Bước 6: Tinh Chỉnh Kiến Trúc (Gateway & API Contract)**
- Map toàn bộ Config YAML qua `config-service`.
- Xác nhận các Router gateway hợp lệ.
- Export các file Swagger/OpenAPI.
