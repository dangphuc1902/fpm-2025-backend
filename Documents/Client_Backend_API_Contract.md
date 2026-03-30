# FPM - Backend & Client API Contract
Tài liệu này định nghĩa các API (REST) giao tiếp giữa hệ thống Client (Mobile/Web) và các dịch vụ Backend liên quan đến các tính năng đang chuẩn bị xây dựng (Phase 1-4).

---

## 1. Tính năng Nhóm/Gia đình (Family) - [User Service]
*Dự kiến triển khai tại `user-auth-service`*

### 1.1 Tạo Gia đình (Create Family)
- **Endpoint**: `POST /api/v1/families`
- **Chức năng**: Cho phép một người dùng tạo một Family mới và nghiễm nhiên trở thành Chủ nhóm (Admin/Owner).
- **Request Body**:
  ```json
  {
      "name": "Gia đình nhà A",
      "description": "Gia đình nhỏ của tui"
  }
  ```
- **Response**: Trả về chi tiết của Family (ID, name, createdAt, role of currentUser).

### 1.2 Xem danh sách thành viên trong Gia đình (Get Family Members)
- **Endpoint**: `GET /api/v1/families/{familyId}/members`
- **Chức năng**: Lấy danh sách toàn bộ các thành viên hiện có.
- **Response**: List of members `{ userId, fullName, role, joinedAt, avatarUrl }`.

### 1.3 Mời thành viên (Invite to Family)
- **Endpoint**: `POST /api/v1/families/{familyId}/invite`
- **Chức năng**: Gửi thư/thông báo mời vào nhóm.
- **Request Body**:
  ```json
  {
      "email": "userB@gmail.com",
      "role": "MEMBER" // hoặc ADMIN
  }
  ```

---

## 2. Tính năng Ví chung (Shared Wallet) - [Wallet Service]
*Dự kiến triển khai tại `wallet-service`*

### 2.1 Tạo Ví dùng chung (Create Shared Wallet)
- **Endpoint**: `POST /api/v1/wallets`
- **Chức năng**: Mở rộng request tạo ví, truyền thêm `familyId` nếu là ví chung của cả nhóm.
- **Request Body**:
  ```json
  {
      "name": "Quỹ đi du lịch",
      "initialBalance": 0.0,
      "type": "SHARED",
      "familyId": 123 
  }
  ```

### 2.2 Cấp quyền/Chia sẻ ví cho User cụ thể
- **Endpoint**: `POST /api/v1/wallets/{walletId}/permissions`
- **Chức năng**: Phân quyền (READ, WRITE) cho một user cụ thể trong trường hợp chỉ share ví cho 1 vài người nhất định.
- **Request Body**:
  ```json
  {
      "userId": 456,
      "permissionLevel": "WRITE"
  }
  ```

---

*(Tài liệu này sẽ được update dần khi chúng ta hoàn thiện các Service tiếp theo là Transaction, Category, và Reporting)*
