package com.fpm_2025.reportingservice.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "stats")
@IdClass(StatsEntity.StatsId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "period")
	private StatPeriod period;

	@Id
	@Column(name = "year_month", length = 7)
	private String yearMonth; // "2025-12"

	@Column(name = "total_expense", precision = 15, scale = 2)
	private BigDecimal totalExpense = BigDecimal.ZERO;

	@Column(name = "total_income", precision = 15, scale = 2)
	private BigDecimal totalIncome = BigDecimal.ZERO;

    @Type(JsonType.class)
    @Column(name = "category_breakdown", columnDefinition = "jsonb")
    private Map<String, BigDecimal> categoryBreakdown;

	@Column(name = "calculated_at")
	private java.time.LocalDateTime calculatedAt = java.time.LocalDateTime.now();

	// Composite Key class
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	public static class StatsId implements Serializable {
		private Long userId;
		private StatPeriod period;
		private String yearMonth;
	}
}

enum StatPeriod {
	DAY, WEEK, MONTH
}