# 🚀 Family Pocket Manager (FPM) - Toàn bộ Tính năng & Luồng hoạt động

Tài liệu này tổng hợp toàn bộ các tính năng đã được triển khai trên cả Backend (Microservices) và Client (Android App), cùng với luồng nghiệp vụ chính của hệ thống.

---

## 🛠 1. Danh sách tính năng (Features List)

### 🔐 1.1. User & Authentication (Lõi bảo mật)
- **Đăng ký/Đăng nhập**: Cơ chế chuẩn REST với JWT.
- **Google OAuth2**: Đăng nhập bằng tài khoản Google (tự động tạo ví mặc định).
- **Quản lý Profile**: Cập nhật thông tin cá nhân, ảnh đại diện.
- **RBAC (Role-Based Access Control)**: Phân quyền Owner/Member trong nhóm gia đình.

### 💳 1.2. Wallet Management (Quản lý tài chính)
- **Ví cá nhân & Ví chia sẻ**: Hỗ trợ nhiều loại ví (Tiền mặt, Ngân hàng, Thẻ tín dụng).
- **Auto-Default Wallet**: Tự động tạo ví "Tiền mặt" cho người dùng mới qua Kafka.
- **API gRPC**: Kiểm tra quyền truy cập ví nhanh chóng giữa các Microservices.

### 📝 1.3. Transaction & Smart Input (Giao dịch thông minh)
- **Ghi chép giao dịch**: Thu, Chi, Chuyển khoản nội bộ.
- **Đính kèm minh chứng**: Tải ảnh hóa đơn lên server (Lưu trữ metadata).
- **Phân loại tự động**: Hệ thống gợi ý danh mục chi tiêu.

### 🤖 1.4. High-Tech Services (AI & OCR)
- **OCR Scanner**: Sử dụng Tesseract 5.x bóc tách thông tin hóa đơn (Số tiền, Cửa hàng, Ngày).
- **AI NLP (Natural Language Processing)**: Tự động điền form giao dịch từ văn bản thô (Nlp powered by Gemini 1.5 Flash).
- **Personal Finance Assistant**: Chat trực tiếp với Gemini để tư vấn tài chính, hỏi về chi tiêu trong tháng.

### 👨‍👩‍👧‍👦 1.5. Family & Invitations (Kết nối gia đình)
- **Cơ chế Mời/Nhận nhóm**: Gửi lời mời qua Email -> Người dùng nhận thông báo -> Chấp nhận/Từ chối.
- **Shared Budget**: Các thành viên trong gia đình cùng theo dõi một nguồn ngân sách chung.

### 📊 1.6. Reporting & Analytics (Báo cáo thông minh)
- **Dashboard Tổng quát**: Xem nhanh số dư, thu nhập, chi tiêu.
- **Biểu đồ xu hướng**: Theo dõi biến động tài chính theo tháng (Line Chart).
- **Phân tích danh mục**: Cơ cấu chi tiêu theo nhóm (Pie Chart).
- **Cảnh báo vượt hạn mức**: Thông báo tự động khi chi tiêu chạm ngưỡng ngân sách (Budget alert).

### 🔔 1.7. Notification System (Thông báo)
- **Push Notification**: Firebase Cloud Messaging (FCM).
- **Bank SMS Parser**: Tự động bóc tách tin nhắn biến động số dư từ MB Bank, VCB, MoMo... để tạo giao dịch tự động.

---

## 🔄 2. Luồng hoạt động chính (Standard Flows)

### 2.1. Luồng Đăng nhập & Khởi tạo (User Onboarding)
1. **User** -> Đăng ký qua App -> **user-auth-service** lưu DB.
2. **user-auth-service** bắn event `UserCreated` vào **Kafka**.
3. **wallet-service** tiêuhtu event -> Tự động tạo "Ví Tiền mặt" (VND) cho User.
4. **User** đăng nhập thành công -> Nhận JWT -> Vào màn hình Dashboard.

### 2.2. Luồng Quét hóa đơn (OCR Flow)
1. **User** chụp ảnh hóa đơn -> **Client** gửi file qua `MultipartBody` tới **api-gateway**.
2. **api-gateway** điều hướng tới **ocr-service**.
3. **ocr-service** sử dụng Tesseract để đọc text -> Trả về JSON (Amount, Merchant).
4. **Client** nhận JSON -> Tự động điền form -> User ấn "Lưu".

### 2.3. Luồng Quản lý Gia đình (Family Invite Flow)
1. **Owner** nhập email em gái -> Gửi yêu cầu tới **user-auth-service**.
2. **user-auth-service** tạo bản ghi Invitation (Pending) -> Gửi Push Notif tới **notification-service**.
3. **notification-service** gửi thông báo tới điện thoại người nhận qua **Firebase**.
4. **Người nhận** mở App -> Xem danh sách Invitation -> Ấn "Chấp nhận".
5. **user-auth-service** cập nhật trạng thái -> Thêm người dùng vào nhóm gia đình.

### 2.4. Luồng Phân tích & Báo cáo (Reporting Flow)
1. Bất kỳ giao dịch mới nào được tạo -> **transaction-service** bắn event vào **Kafka**.
2. **reporting-service** tiêu thụ event -> Cập nhật bảng `TransactionSummary` (Real-time).
3. **User** mở màn hình Báo cáo -> **reporting-service** tính toán dữ liệu Chart và trả về kết quả nhanh chóng.

---

## 🏗 3. Kiến trúc kỹ thuật (Tech Stack)
- **Frontend**: Android (Kotlin), Jetpack Compose, Retrofit, Coroutines, RoomDB.
- **Backend**: Spring Boot 3.x, Microservices Architecture.
- **Communication**: REST API (Client-Server), gRPC (Internal), Kafka/RabbitMQ (Event-driven).
- **Database**: MySQL (Dữ liệu chính), Redis (Rate limiting).
- **AI/ML**: Google Gemini 1.5 Flash API, Tesseract OCR Engine.

---
*Tài liệu được cập nhật ngày 05/04/2026 bởi TRỢ LÝ AI (Antigravity).*
