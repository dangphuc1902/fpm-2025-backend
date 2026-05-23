package com.fpm_2025.reportingservice.controller;

import com.fpm_2025.reportingservice.dto.request.DashboardRequest;
import com.fpm_2025.reportingservice.dto.response.DashboardResponse;
import com.fpm_2025.reportingservice.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Cacheable(value = "dashboard", key = "#userId + '-' + #yearMonth")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String yearMonth) {

        logger.info("Getting dashboard for user: {}, month: {}", userId, yearMonth);

        if (yearMonth == null || yearMonth.isBlank()) {
            yearMonth = YearMonth.now().toString();
        }

        DashboardRequest request = new DashboardRequest(userId, yearMonth);
        DashboardResponse response = dashboardService.getDashboard(request);

        return ResponseEntity.ok(response);
    }
}