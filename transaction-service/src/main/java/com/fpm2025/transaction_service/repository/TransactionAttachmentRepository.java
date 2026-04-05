package com.fpm2025.transaction_service.repository;

import com.fpm2025.transaction_service.entity.TransactionAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAttachmentRepository extends JpaRepository<TransactionAttachmentEntity, Long> {
    List<TransactionAttachmentEntity> findByTransactionId(Long transactionId);
}
