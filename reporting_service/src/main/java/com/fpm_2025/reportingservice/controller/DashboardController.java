package com.fpm_2025.reportingservice.controller;

import com.fpm_2025.reportingservice.dto.request.DashboardRequest;
import com.fpm_2025.reportingservice.dto.response.DashboardResponse;
import com.fpm_2025.reportingservice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Cacheable(value = "dashboard", key = "#userId + '-' + #yearMonth")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String yearMonth) {
        
        log.info("Getting dashboard for user: {}, month: {}", userId, yearMonth);
        
        if (yearMonth == null || yearMonth.isBlank()) {
            yearMonth = YearMonth.now().toString();
        }
        
        DashboardRequest request = DashboardRequest.builder()
                .userId(userId)
                .yearMonth(yearMonth)
                .build();
        
        DashboardResponse response = dashboardService.getDashboard(request);
        
        return ResponseEntity.ok(response);
    }
}