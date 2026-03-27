# Shared Wallet API Contract (Wallet Service)

Tài liệu này mô tả các API endpoints được bổ sung vào `wallet-service` để hỗ trợ tính năng Chia sẻ Ví (Shared Wallet). Client (Frontend/Mobile) sẽ sử dụng các API này để đồng bộ và thực hiện các chức năng chia sẻ ví giữa các thành viên.

## Base Path: `/api/v1/wallets`

### 1. Tạo Ví Mới (Hỗ trợ Ví Gia đình/Nhóm)
**Endpoint**: `POST /api/v1/wallets`  
**Description**: Tạo một ví mới. Payload hiện tại hỗ trợ thêm trường `familyId` để liên kết ví với một gia đình (Family) cụ thể.

**Request Body** (`CreateWalletRequest`):
```json
{
  "name": "Quỹ du lịch gia đình",
  "type": "SHARED",
  "currency": "VND",
  "initialBalance": 10000000,
  "icon": "ic_wallet_travel",
  "familyId": 123  // (Tùy chọn) ID của family nếu ví này thuộc về một gia đình
}
```

**Response Body** (`WalletResponse`):
```json
{
  "code": 200,
  "message": "Wallet created successfully",
  "data": {
    "id": 1,
    "userId": 456,
    "familyId": 123,
    "name": "Quỹ du lịch gia đình",
    "type": "SHARED",
    "currency": "VND",
    "balance": 10000000,
    "icon": "ic_wallet_travel",
    "isActive": true
  }
}
```

---

### 2. Chia sẻ Ví cho người dùng khác
**Endpoint**: `POST /api/v1/wallets/{id}/share`  
**Description**: Cấp quyền truy cập ví cho một người dùng khác. Người gọi API (caller) bắt buộc phải là chủ sở hữu (owner) của ví.

**Request Body** (`ShareWalletRequest`):
```json
{
  "userId": 789, // ID của user được chia sẻ
  "permissionLevel": "VIEWER" // Quyền truy cập: "VIEWER", "EDITOR", "ADMIN"
}
```

**Response Body** (`WalletPermissionResponse`):
```json
{
  "code": 201,
  "message": "Wallet shared successfully",
  "data": {
    "id": 1,
    "walletId": 10,
    "userId": 789,
    "permissionLevel": "VIEWER",
    "createdAt": "2023-11-01T12:00:00Z"
  }
}
```

---

### 3. Lấy danh sách người dùng đang được chia sẻ ví
**Endpoint**: `GET /api/v1/wallets/{id}/shares`  
**Description**: Lấy danh sách tất cả các người dùng (kèm quyền hạn) đã được cấp quyền truy cập vào ví này.

**Response**:
```json
{
  "code": 200,
  "message": "Shared users retrieved successfully",
  "data": [
    {
      "id": 1,
      "walletId": 10,
      "userId": 789,
      "permissionLevel": "VIEWER",
      "createdAt": "2023-11-01T12:00:00Z"
    }
  ]
}
```

---

### 4. Xóa/Thu hồi quyền truy cập ví của một người dùng
**Endpoint**: `DELETE /api/v1/wallets/{id}/share/{targetUserId}`  
**Description**: Thu hồi quyền truy cập ví của một user cụ thể. Chủ sở hữu ví mới có quyền thực hiện hành động này.

**Response**:
```json
{
  "code": 200,
  "message": "Wallet share removed successfully",
  "data": null
}
```

---

### 5. Lấy danh sách các Ví được người khác chia sẻ với mình
**Endpoint**: `GET /api/v1/wallets/shared`  
**Description**: Lấy danh sách các ví mà các người dùng khác đã chia sẻ cho user đang đăng nhập.

**Response**:
```json
{
  "code": 200,
  "message": "Shared wallets retrieved successfully",
  "data": [
    {
      "id": 10,
      "userId": 456, // ID của người chủ thật sự của ví
      "familyId": null,
      "name": "Quỹ chung bạn bè",
      "type": "SHARED",
      "currency": "VND",
      "balance": 5000000,
      "icon": "ic_wallet_friends",
      "isActive": true
    }
  ]
}
```

## Lưu ý (Notes for Client Integration)
1. Trường `familyId`: Khi tạo ví dành cho một nhóm/gia đình, Client cần truyền `familyId`. Điều này giúp Frontend có thể gom nhóm các ví theo từng "nhóm/gia đình" khi hiển thị giao diện.
2. Việc phân định danh sách: API `GET /api/v1/wallets` sẽ chỉ trả về danh sách các ví do user đang đăng nhập **tạo và làm chủ**. Để tải danh sách ví **được người khác chia sẻ**, Client cần gọi tường minh API `GET /api/v1/wallets/shared`. Việc tách đôi hai API giúp Frontend chủ động hơn trong cấu trúc state.
3. Kiểm tra Authorization ở Frontend: Dựa vào thuộc tính `permissionLevel` trả về từ API chia sẻ, Client nên ẩn/hiện form thêm giao dịch (Transaction) của ví. Thay vì chỉ bắt lỗi ở Backend (ví dụ `VIEWER` cố thêm giao dịch), Client chặn trước bằng giao diện để UX tốt hơn.
