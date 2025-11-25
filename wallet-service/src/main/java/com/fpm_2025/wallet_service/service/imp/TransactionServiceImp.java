package com.fpm_2025.wallet_service.service.imp;

import com.fpm_2025.wallet_service.dto.payload.request.CreateTransactionRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateTransactionRequest;
import com.fpm_2025.wallet_service.dto.payload.response.TransactionResponse;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
// CRUD giao dịch, lọc theo ví/user/category/date, tính tổng, hỗ trợ paging. (Core của quản lý tài chính(Nhập chi tiêu, báo cáo,...))
public interface TransactionServiceImp {
	// Tạo giao dịch mới(Cập nhật ví, publish Kafka)
    TransactionResponse createTransaction(CreateTransactionRequest request, Long userId);
    // Sửa giao dịch (Revert cũ trên ví, áp dụng mới, publish Kafka)
    TransactionResponse updateTransaction(Long transactionId, UpdateTransactionRequest request, Long userId);
    // Xoá giao dịch (Revert balance )
    void deleteTransaction(Long transactionId, Long userId);
    // Lấy giao dịch của user với phân trang.
    Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable);
    // Lấy giao dịch của user theo loại (EXPENSE/INCOME) với phân trang.
    Page<TransactionResponse> getUserTransactionsByType(Long userId, CategoryType type, Pageable pageable);
    // Lấy giao dịch theo id của user
    TransactionResponse getTransactionById(Long transactionId, Long userId);
    // Lấy giao dịch của user trong khoảng thời gian
    List<TransactionResponse> getTransactionsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    // Lấy giao dịch của ví với phân trang
    Page<TransactionResponse> getWalletTransactions(Long walletId, Long userId, Pageable pageable);
    // Lấy giao dịch của danh mục
    List<TransactionResponse> getCategoryTransactions(Long categoryId, Long userId);
    // Tính tổng số tiền giao dịch của user theo loại trong khoảng thời gian
    BigDecimal getTotalAmount(Long userId, CategoryType type, LocalDateTime startDate, LocalDateTime endDate);
    long getUserTransactionCount(Long userId);
}
