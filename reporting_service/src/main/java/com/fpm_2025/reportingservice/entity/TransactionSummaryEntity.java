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

    @Column(name = "period")
    private String period;
}
