package com.fpm_2025.reportingservice.controller;

import com.fpm_2025.reportingservice.dto.BudgetDTO;
import com.fpm_2025.reportingservice.dto.BudgetSummaryDTO;
import com.fpm_2025.reportingservice.domain.model.Budget;
import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import com.fpm_2025.reportingservice.dto.request.BudgetRequest;
import com.fpm_2025.reportingservice.security.UserPrincipal;
import com.fpm_2025.reportingservice.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Budget Controller – Quản lý ngân sách chi tiêu theo danh mục.
 *
 * <p>Cung cấp các API để thiết lập hạn mức chi tiêu, theo dõi tiến độ và nhận cảnh báo
 * khi chi tiêu vượt ngưỡng.
 */
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Budgets", description = "Quản lý ngân sách (Budget Management)")
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * POST /api/v1/budgets
     * Tạo ngân sách mới cho user.
     */
    @PostMapping
    @Operation(summary = "Tạo ngân sách mới")
    public ResponseEntity<BudgetDTO> createBudget(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody BudgetDTO budgetDTO) {

        log.info("[BudgetController] Create budget: userId={}, category={}", 
                user.getId(), budgetDTO.getCategoryName());

        BudgetRequest request = toRequest(budgetDTO);
        Budget created = budgetService.createBudget(user.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    /**
     * GET /api/v1/budgets
     * Lấy danh sách tất cả ngân sách đang active của user.
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách ngân sách của tôi")
    public ResponseEntity<List<BudgetDTO>> getAllBudgets(
            @AuthenticationPrincipal UserPrincipal user) {

        log.info("[BudgetController] List budgets: userId={}", user.getId());
        List<Budget> budgets = budgetService.getActiveBudgets(user.getId());
        
        return ResponseEntity.ok(budgets.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * GET /api/v1/budgets/summary
     * Lấy tổng hợp tình hình ngân sách theo tháng.
     */
    @GetMapping("/summary")
    @Operation(summary = "Xem tổng kết ngân sách")
    public ResponseEntity<BudgetSummaryDTO> getBudgetSummary(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String yearMonth) {

        String month = (yearMonth != null && !yearMonth.isBlank()) 
                ? yearMonth : YearMonth.now().toString();

        log.info("[BudgetController] Budget summary: userId={}, month={}", user.getId(), month);

        List<Budget> budgets = budgetService.getActiveBudgets(user.getId());
        List<Budget> filtered = budgets.stream()
                .filter(b -> month.equals(b.getYearMonth()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(buildSummary(month, filtered));
    }

    /**
     * GET /api/v1/budgets/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một ngân sách")
    public ResponseEntity<BudgetDTO> getBudgetById(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        return ResponseEntity.ok(toDTO(budgetService.getBudgetById(user.getId(), id)));
    }

    /**
     * PUT /api/v1/budgets/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật ngân sách")
    public ResponseEntity<BudgetDTO> updateBudget(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody BudgetDTO budgetDTO) {

        log.info("[BudgetController] Update budget: id={}, userId={}", id, user.getId());
        
        BudgetRequest request = toRequest(budgetDTO);
        Budget updated = budgetService.updateBudget(user.getId(), id, request);
        
        return ResponseEntity.ok(toDTO(updated));
    }

    /**
     * DELETE /api/v1/budgets/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa ngân sách")
    public ResponseEntity<Map<String, String>> deleteBudget(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        log.info("[BudgetController] Delete budget: id={}, userId={}", id, user.getId());
        budgetService.deleteBudget(user.getId(), id);
        
        return ResponseEntity.ok(Map.of(
            "status", "success", 
            "message", "Ngân sách đã được xóa thành công"
        ));
    }

    // =========================================================================
    // Mappers & Private Helpers
    // =========================================================================

    private BudgetDTO toDTO(Budget b) {
        return BudgetDTO.builder()
                .id(b.getId())
                .categoryId(b.getCategoryId())
                .categoryName(b.getCategoryName())
                .amountLimit(b.getAmountLimit())
                .amountUsed(b.getAmountUsed())
                .remainingAmount(b.getRemainingAmount())
                .usagePercentage(b.getUsagePercentage())
                .period(b.getPeriod())
                .yearMonth(b.getYearMonth())
                .status(determineStatus(b))
                .isActive(b.getIsActive())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private BudgetRequest toRequest(BudgetDTO dto) {
        return BudgetRequest.builder()
                .categoryId(dto.getCategoryId())
                .categoryName(dto.getCategoryName())
                .amountLimit(dto.getAmountLimit())
                .period(dto.getPeriod() != null ? dto.getPeriod() : BudgetPeriod.MONTHLY)
                .yearMonth(dto.getYearMonth() != null ? dto.getYearMonth() : YearMonth.now().toString())
                .build();
    }

    private String determineStatus(Budget b) {
        if (!Boolean.TRUE.equals(b.getIsActive())) return "INACTIVE";
        if (b.isOverBudget()) return "OVER_BUDGET";
        
        BigDecimal pct = b.getUsagePercentage();
        if (pct.compareTo(BigDecimal.valueOf(95)) >= 0) return "CRITICAL";
        if (pct.compareTo(BigDecimal.valueOf(80)) >= 0) return "WARNING";
        
        return "ON_TRACK";
    }

    private BudgetSummaryDTO buildSummary(String yearMonth, List<Budget> budgets) {
        BigDecimal totalLimit = budgets.stream()
                .map(Budget::getAmountLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalUsed = budgets.stream()
                .map(Budget::getAmountUsed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal overallPct = totalLimit.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO 
                : totalUsed.divide(totalLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return BudgetSummaryDTO.builder()
                .yearMonth(yearMonth)
                .totalLimit(totalLimit)
                .totalUsed(totalUsed)
                .totalRemaining(totalLimit.subtract(totalUsed))
                .overallUsagePercentage(overallPct)
                .warningCount((int) budgets.stream().filter(b -> b.getUsagePercentage().compareTo(BigDecimal.valueOf(80)) >= 0 && !b.isOverBudget()).count())
                .overBudgetCount((int) budgets.stream().filter(Budget::isOverBudget).count())
                .totalBudgets(budgets.size())
                .budgets(budgets.stream().map(this::toDTO).collect(Collectors.toList()))
                .build();
    }
}
