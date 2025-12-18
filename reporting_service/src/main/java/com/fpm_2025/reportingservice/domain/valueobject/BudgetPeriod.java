package com.fpm_2025.reportingservice.domain.valueobject;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum BudgetPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM;
    
    public LocalDate calculateEndDate(LocalDate startDate) {
        return switch (this) {
            case DAILY -> startDate;
            case WEEKLY -> startDate.plusWeeks(1).minusDays(1);
            case MONTHLY -> startDate.with(TemporalAdjusters.lastDayOfMonth());
            case QUARTERLY -> startDate.plusMonths(3).minusDays(1);
            case YEARLY -> startDate.with(TemporalAdjusters.lastDayOfYear());
            case CUSTOM -> null; // User defines end date
        };
    }
    
    public LocalDate getNextPeriodStart(LocalDate currentStart) {
        return switch (this) {
            case DAILY -> currentStart.plusDays(1);
            case WEEKLY -> currentStart.plusWeeks(1);
            case MONTHLY -> currentStart.plusMonths(1);
            case QUARTERLY -> currentStart.plusMonths(3);
            case YEARLY -> currentStart.plusYears(1);
            case CUSTOM -> null;
        };
    }
}