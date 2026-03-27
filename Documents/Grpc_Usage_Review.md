# Phân tích việc sử dụng gRPC trong Hệ thống FPM 2025 Backend

Dựa vào việc rà soát toàn bộ source code và các file `.proto` trong module `fpm-proto` (`libs/fpm-libs/fpm-proto`), dưới đây là báo cáo về cách giao tiếp gRPC được thiết kế và ứng dụng trong dự án hiện tại.

## 1. Kiến trúc Tổ chức gRPC
Thay vì định nghĩa `.proto` rải rác ở từng service, toàn bộ các interface gRPC (Protocol Buffers) được khai báo tập trung trong thư viện module dùng chung là **`fpm-libs/fpm-proto`**. 
- Các services giao tiếp với nhau qua module `fpm-grpc` (đóng vai trò cấu hình beans và auto-config cho gRPC Server/Client).
- 100% các file proto được quy định compile ra package `com.fpm2025.protocol.*`.

## 2. Các Service gRPC được định nghĩa (gRPC Contracts)

Hệ thống định nghĩa tổng cộng 5 gRPC Services tương ứng với các domain:

### 2.1. WalletGrpcService (`wallet.proto`)
Được implement tại `wallet-service`. Cung cấp dữ liệu về Ví cho các microservice khác.
- **Tính năng / RPCs**:
  - `GetWalletById`: Lấy thông tin cơ bản một ví.
  - `GetWalletsByUserId`: Trả về danh sách tất cả các ví của một user.
  - `UpdateBalance`: Thực hiện cộng/trừ tiền vào số dư của ví an toàn khi Transaction Service xử lý.
  - `ValidateWalletAccess`: Xác thực quyền truy cập đối với một Ví.
  - `CheckSufficientBalance`: Kiểm tra ví có đủ tiền thực hiện giao dịch không.

### 2.2. TransactionGrpcService (`transaction.proto`)
Cung cấp dữ liệu giao dịch cho hệ thống (đặc biệt là Báo Cáo - Reporting).
- **Tính năng / RPCs**:
  - `GetTransactionById`, `GetTransactionsByWallet`, `GetTransactionsByUser`.
  - `GetTransactionsByDateRange`: Dùng nhiều nhất bởi Reporting Service để xuất biểu đồ thu chi.
  - `CreateTransaction`: Tạo giao dịch mới thông qua gRPC (dành cho Notification/OCR Service gọi sang tự động).
  - `GetTotalSpending`: Tính toán tổng chi tiêu.

### 2.3. SharingGrpcService (`sharing.proto`)
Quản lý quyền chia sẻ ví và Family.
- **Tính năng / RPCs**:
  - `GetFamilyById`, `GetUserFamilies`.
  - `CheckFamilyMembership`: Dùng khi Wallet Service check user có nằm trong family hay không trước khi xét quyền.
  - `GetSharedWallets`: Lấy danh sách Wallet ID mà Family đó sở hữu.

### 2.4. UserGrpcService (`user.proto`)
Được implement tại `user-auth-service`.
- **Tính năng / RPCs**:
  - `GetUserById`, `GetUsersByIds`, `CheckUserExists`: Xác minh user phía Reporting / Wallet.
  - `ValidateToken`: Dùng bởi `api-gateway` để xác thực JWT Token hợp lệ trước khi cho đi sâu vào Gateway.
  - `GetFamilyMembers`: Trả về danh sách user ID.

### 2.5. CategoryGrpcService (`category.proto`)
Quản lý Categorize & Budget.
- **Tính năng / RPCs**:
  - `GetCategoryById`, `GetCategoriesByType`, `GetCategoryTree`.
  - `AutoCategorize`: Input Text -> Trả về mảng category phù hợp (VD: OCR quét ra "Highland" -> "Cà phê & Đồ uống").
  - `CheckBudgetStatus`: Check ngân sách trước khi Transaction Service trừ tiền.

## 3. Tổng Kết Use-Case gRPC Điển Hình
Trong FPM, **REST API** dùng cho giao tiếp **Client -> Server**. Còn lại **gRPC** được dùng nội bộ **Server -> Server (Microservices)**:
1. **API Gateway -> User-Auth**: Xác minh Token qua gRPC (Siêu nhanh, tránh delay).
2. **Transaction -> Wallet**: Gọi khối `UpdateBalance` sang Wallet, đảm bảo ACID cho ví.
3. **Reporting -> (Tất cả)**: Gọi gRPC đa luồng sang Wallet, Transaction, Category để compile báo cáo tài chính ngay lập tức.
4. **OCR/Notification -> Transaction**: Máy học & Hook trigger tạo giao dịch tốc độ cao không qua REST để tránh HTTP overhead.
