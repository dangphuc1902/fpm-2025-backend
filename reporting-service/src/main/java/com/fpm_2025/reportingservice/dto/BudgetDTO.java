package com.fpm_2025.reportingservice.dto;

import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object cho Budget.
 *
 * <p>Được dùng cho cả request (tạo/cập nhật) và response (trả về cho client).
 * Các trường computed (remainingAmount, usagePercentage, status) chỉ có nghĩa
 * trong response — client không cần gửi các trường này khi tạo/cập nhật budget.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDTO {

    /** ID (null khi tạo mới, có giá trị khi response) */
    private Long id;

    /** ID danh mục chi tiêu */
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    /** Tên danh mục (hiển thị cho client) */
    @NotBlank(message = "Category name is required")
    private String categoryName;

    /** Hạn mức chi tiêu (VND) */
    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be positive")
    private BigDecimal amountLimit;

    /** Đã chi tiêu (computed, chỉ có trong response) */
    @Builder.Default
    private BigDecimal amountUsed = BigDecimal.ZERO;

    /** Còn lại = amountLimit - amountUsed (computed) */
    private BigDecimal remainingAmount;

    /** Phần trăm đã dùng 0-100+ (computed) */
    private BigDecimal usagePercentage;

    /** Chu kỳ ngân sách */
    @NotNull(message = "Budget period is required")
    @Builder.Default
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    /**
     * Tháng áp dụng — format: "yyyy-MM" (VD: "2025-04")
     * Bắt buộc khi period=MONTHLY
     */
    @NotBlank(message = "Year month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Year month must be in format yyyy-MM")
    private String yearMonth;

    /** Trạng thái: ON_TRACK | WARNING | CRITICAL | OVER_BUDGET | INACTIVE */
    private String status;

    /** Còn active không */
    @Builder.Default
    private Boolean isActive = true;

    /** Timestamps */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
