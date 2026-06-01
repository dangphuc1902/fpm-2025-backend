package com.fpm_2025.wallet_service.service.imp;

//Mục đích: Tối ưu hiệu suất bằng cách cache dữ liệu thường dùng (balance, danh sách ví).
public interface CacheServiceImp {
    // Wallet Balance Cache: Cache số dư ví: Lưu balance của ví theo walletId.
    void cacheWalletBalance(Long walletId, Object balance);
    // Lấy số dư ví từ cache theo walletId. Tránh query database nhiều lần.
    Object getWalletBalance(Long walletId);
    // Xoá cache số dư ví khi có thay đổi (thêm/sửa/xoá giao dịch ảnh hưởng đến ví).
    void evictWalletBalance(Long walletId);

    // User Wallet List Cache
    void cacheUserWallets(Long userId, Object wallets);
    // Lấy danh sách ví của user từ cache theo userId.
    Object getUserWallets(Long userId);
    // Xoá cache danh sách ví của user khi có thay đổi (thêm/sửa/xoá ví).
    void evictUserWallets(Long userId);
}
