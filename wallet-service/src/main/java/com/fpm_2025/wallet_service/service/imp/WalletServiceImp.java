package com.fpm_2025.wallet_service.service.imp;

import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.response.WalletResponse;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.enums.WalletType;
import com.fpm_2025.wallet_service.event.model.TransactionCreatedEvent;

import java.math.BigDecimal;
import java.util.List;
// Quản lý ví người dùng: tạo/sửa/xoá ví, lấy ví theo user/type, cập nhật số dư, tính tổng số dư tất cả ví.
public interface WalletServiceImp {
	// Tạo ví mới cho user
    WalletResponse createWallet(CreateWalletRequest request, Long userId);
    // Lấy tất cả ví của user
    List<WalletResponse> getUserWallets(Long userId);
    // Lấy tất cả ví đang hoạt động của user
    List<WalletResponse> getUserActiveWallets(Long userId);
    // Lấy ví của user theo loại ví
    List<WalletResponse> getUserWalletsByType(Long userId, WalletType type);
    // Lấy ví theo id của user
    WalletResponse getWalletById(Long walletId, Long userId);
    // Cập nhật thông tin ví
    WalletResponse updateWallet(Long walletId, UpdateWalletRequest request, Long userId);
    // Xoá ví của user
    void deleteWallet(Long walletId, Long userId);
    // Tính tổng số dư tất cả ví của user
    BigDecimal getTotalBalance(Long userId);
	// Cập nhật số dư ví (thêm/giảm)
    WalletResponse updateBalance(Long walletId, Long userId, BigDecimal amount, boolean isAddition);
    // Kiểm tra quyền truy cập ví của user
    boolean validateWalletAccess(Long walletId, Long userId);
    // Lấy thực thể ví theo id và userId
    WalletEntity getWalletEntity(Long walletId, Long userId);
    //  Đếm số lượng ví của user
    long getUserWalletCount(Long userId);
    // Lấy thực thể ví theo userId và loại ví
    WalletEntity getWalletEntityByUserIdAndWalletType(Long userId, WalletType type);
    // Cập nhật số dư ví trực tiếp
    void updateBalance(WalletEntity wallet, BigDecimal newBalance);
	// Cập nhật số dư ví từ sự kiện giao dịch được tạo
    void updateBalanceFromTransaction(TransactionCreatedEvent event);
}
