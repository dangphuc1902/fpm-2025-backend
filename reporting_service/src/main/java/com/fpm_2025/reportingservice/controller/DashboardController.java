package com.fpm_2025.reportingservice.controller;

import com.fpm_2025.core.annotation.CurrentUser;
import com.fpm_2025.core.dto.BaseResponse;
import com.fpm_2025.reporting.service.dto.request.DashboardRequest;
import com.fpm_2025.reporting.service.dto.response.DashboardResponse;
import com.fpm_2025.reporting.service.service.DashboardService;
import com.fpm2025.security.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Cacheable(value = "dashboard", key = "#user.id + '-' + #month")
    public BaseResponse<DashboardResponse> getDashboard(
            @CurrentUser UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Getting dashboard for user: {}, month: {}", user.getId(), month);
        
        if (month == null) {
            month = LocalDate.now().withDayOfMonth(1);
        }
        
        DashboardRequest request = DashboardRequest.builder()
                .userId(user.getId())
                .month(month)
                .build();
        
        DashboardResponse response = dashboardService.getDashboard(request);
        
        return BaseResponse.success("Dashboard retrieved successfully", response);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<DashboardResponse> getQuickSummary(@CurrentUser UserPrincipal user) {
        log.info("Getting quick summary for user: {}", user.getId());
        
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        DashboardRequest request = DashboardRequest.builder()
                .userId(user.getId())
                .month(currentMonth)
                .build();
        
        DashboardResponse response = dashboardService.getQuickSummary(request);
        
        return BaseResponse.success(response);
    }
}