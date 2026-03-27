package com.fpm_2025.reportingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "period", length = 7) // e.g. "2025-03"
    private String period;

    @Column(name = "total_income", precision = 19, scale = 2)
    private java.math.BigDecimal totalIncome;

    @Column(name = "total_expense", precision = 19, scale = 2)
    private java.math.BigDecimal totalExpense;
}
