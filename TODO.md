# FPM Backend Project TODO List

Đây là danh sách các công việc cần hoàn thành để hoàn thiện dự án backend. Danh sách được sắp xếp theo thứ tự ưu tiên từ cao đến thấp.

## Giai đoạn 1: Xây dựng Lõi Hệ thống (Core System)

### 1. User Authentication Service (`user-auth-service`)
- [ ] Hoàn thiện logic đăng ký, đăng nhập, và đăng xuất.
- [ ] Implement JWT (JSON Web Token) generation và validation.
- [ ] Implement Refresh Token.
- [ ] Hoàn thiện gRPC service `auth.proto` để các service khác có thể xác thực người dùng.
- [ ] Viết Unit Test cho các chức năng chính.

### 2. Wallet Service (`wallet-service`)
- [ ] Thiết kế và tạo bảng `wallets` trong cơ sở dữ liệu.
- [ ] Implement logic tạo ví cho người dùng mới.
- [ ] Implement API để truy vấn số dư, lịch sử giao dịch ví.
- [ ] Implement logic cập nhật số dư (cộng/trừ tiền).
- [ ] Viết Unit Test.

### 3. Transaction Service (`transaction_service`)
- [ ] Thiết kế và tạo bảng `transactions` trong cơ sở dữ liệu.
- [ ] Implement gRPC service `transaction.proto` để xử lý tạo giao dịch.
- [ ] Xử lý logic nghiệp vụ cho các loại giao dịch (chuyển tiền, nạp tiền, rút tiền).
- [ ] Tích hợp với `wallet-service` để cập nhật số dư sau mỗi giao dịch.
- [ ] Sử dụng Kafka hoặc RabbitMQ để xử lý giao dịch một cách bất đồng bộ (nếu cần).

### 4. API Gateway (`api-gateway`)
- [ ] Cấu hình routing cho tất cả các public endpoint từ các microservice.
- [ ] Tích hợp bộ lọc (filter) để xác thực JWT token trên mỗi request.
- [ ] Cấu hình rate limiting và circuit breaker (sử dụng Resilience4j) để tăng tính ổn định.

## Giai đoạn 2: Tích hợp và Hoàn thiện

### 5. Reporting Service (`reporting_service`)
- [ ] Thiết kế và tạo các bảng cần thiết cho việc báo cáo.
- [ ] Sử dụng Kafka để lắng nghe các sự kiện giao dịch từ `transaction_service`.
- [ ] Xây dựng logic để tổng hợp dữ liệu và tạo báo cáo (ví dụ: báo cáo doanh thu, báo cáo giao dịch theo ngày/tháng).
- [ ] Implement API để xuất báo cáo.

### 6. Tích hợp giữa các Service
- [ ] Đảm bảo `user-auth-service` có thể giao tiếp với `wallet-service` để tạo ví khi người dùng mới đăng ký.
- [ ] Đảm bảo `api-gateway` gọi đúng gRPC service trên `user-auth-service` để xác thực.
- [ ] Đảm bảo `transaction_service` gọi đúng API/gRPC của `wallet-service` để cập nhật số dư.
- [ ] Đảm bảo `transaction_service` gửi message thành công đến Kafka để `reporting_service` có thể nhận.

### 7. Quản lý Cấu hình (Configuration Management)
- [ ] Cài đặt Spring Cloud Config Server (hoặc sử dụng giải pháp khác như Consul, etcd).
- [ ] Di chuyển tất cả các cấu hình trong `application.yml` của các service ra một Git repository riêng và quản lý tập trung.

## Giai đoạn 3: Kiểm thử và Triển khai

### 8. Testing Toàn diện
- [ ] Viết Integration Test cho các luồng nghiệp vụ chính (ví dụ: từ đăng nhập -> tạo giao dịch -> xem báo cáo).
- [ ] Viết Contract Test (sử dụng Spring Cloud Contract) để đảm bảo các service không phá vỡ giao tiếp với nhau khi thay đổi.
- [ ] Thực hiện Stress Test/Performance Test để đánh giá khả năng chịu tải của hệ thống.

### 9. Containerization và Orchestration
- [ ] Hoàn thiện file `docker-compose.yml` để có thể khởi chạy toàn bộ hệ thống trên môi trường local.
- [ ] Viết Dockerfile cho từng microservice.
- [ ] (Tùy chọn) Chuẩn bị các tệp manifest Kubernetes (Deployment, Service, Ingress) để sẵn sàng cho việc triển khai lên môi trường production.

### 10. CI/CD (Continuous Integration/Continuous Deployment)
- [ ] Thiết lập pipeline trên GitHub Actions (hoặc Jenkins, GitLab CI).
- [ ] Cấu hình pipeline để tự động build, test, và đóng gói Docker image mỗi khi có commit mới.
- [ ] Cấu hình pipeline để tự động deploy lên môi trường staging hoặc production.

## Giai đoạn 4: Hoàn thiện
### 11. Logging và Monitoring
- [ ] Thiết lập hệ thống logging tập trung (ELK Stack: Elasticsearch, Logstash, Kibana hoặc EFK).
- [ ] Tích hợp Prometheus và Grafana để giám sát các chỉ số hoạt động của từng service (CPU, RAM, JVM metrics, custom business metrics).
- [ ] Tích hợp Distributed Tracing (sử dụng OpenTelemetry hoặc Spring Cloud Sleuth/Zipkin) để theo dõi request qua các microservice.

### 12. Tài liệu hóa (Documentation)
- [ ] Viết tài liệu API chi tiết (sử dụng Swagger/OpenAPI).
- [ ] Viết tài liệu kiến trúc hệ thống, giải thích luồng hoạt động và sự tương tác giữa các service.
- [ ] Cập nhật file `README.md` với hướng dẫn cách cài đặt và chạy dự án.
