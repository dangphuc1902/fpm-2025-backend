package com.fpm2025.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu thông báo SMS từ ngân hàng (raw content).
 * Table: bank_notifications (notification_db)
 */
@Entity
@Table(name = "bank_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "parsed_amount", precision = 15, scale = 2)
    private java.math.BigDecimal parsedAmount;

    /** EXPENSE | INCOME */
    @Column(name = "parsed_type", length = 10)
    private String parsedType;

    @Column(name = "parsed_account", length = 100)
    private String parsedAccount;

    @Column(name = "parsed_note", columnDefinition = "TEXT")
    private String parsedNote;

    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;

    /** ID giao dịch được tạo sau khi parse (nếu có) */
    @Column(name = "transaction_id")
    private Long transactionId;

    /** MD5 checksum của rawContent để deduplicate */
    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
