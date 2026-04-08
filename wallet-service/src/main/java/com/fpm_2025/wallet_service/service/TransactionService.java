package com.fpm_2025.wallet_service.service;

import com.fpm2025.domain.dto.request.TransactionRequest;
import com.fpm2025.domain.dto.request.UpdateTransactionRequest;
import com.fpm2025.domain.dto.response.TransactionResponse;
import com.fpm2025.domain.event.TransactionCreatedEvent;
import com.fpm2025.domain.enums.CategoryType;
import com.fpm_2025.wallet_service.entity.CategoryEntity;
import com.fpm_2025.wallet_service.entity.TransactionEntity;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.exception.ResourceNotFoundException;
import com.fpm_2025.wallet_service.repository.CategoryRepository;
import com.fpm_2025.wallet_service.repository.TransactionRepository;
import com.fpm_2025.wallet_service.repository.WalletRepository;
import com.fpm_2025.wallet_service.service.imp.TransactionServiceImp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService implements TransactionServiceImp {

	@Autowired
	private TransactionRepository transactionRepository;
	@Autowired
	private WalletService walletService;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private WalletRepository walletRepository;
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Override
	@Transactional
	public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
		log.info("Creating transaction for user: {}, amount: {}", userId, request.getAmount());

		if (!walletService.validateWalletAccess(request.getWalletId(), userId)) {
			throw new ResourceNotFoundException("Wallet not found or access denied");
		}

		WalletEntity wallet = walletService.getWalletEntity(request.getWalletId(), userId);

		if (!wallet.getIsActive()) {
			throw new RuntimeException("Wallet is inactive");
		}

		CategoryEntity category = categoryRepository.findById(request.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

		if (!category.getType().equals(request.getType())) {
			throw new IllegalArgumentException("Transaction type must match category type");
		}

		TransactionEntity transaction = TransactionEntity.builder().userId(userId).wallet(wallet)
				.category(category).amount(request.getAmount()).type(request.getType()).note(request.getNote())
				.transactionDate(
						request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
				.build();

		TransactionEntity savedTransaction = transactionRepository.save(transaction);

		boolean isAddition = request.getType() == CategoryType.INCOME;
		walletService.updateBalance(request.getWalletId(), userId, request.getAmount(), isAddition);

		log.info("Transaction created successfully with user_id: {}", userId);

		TransactionCreatedEvent event = TransactionCreatedEvent.builder()
				.transactionId(savedTransaction.getId())
				.walletId(savedTransaction.getWallet().getId())
				.userId(savedTransaction.getUserId())
				.categoryId(savedTransaction.getCategory().getId())
				.amount(savedTransaction.getAmount())
				.type(savedTransaction.getType().name())
				.note(savedTransaction.getNote())
				.timestamp(java.time.Instant.now())
				.build();
				
		kafkaTemplate.send("transaction.created", event);

		return mapToResponse(savedTransaction);
	}

	@Override
	@Transactional
	public TransactionResponse updateTransaction(Long transactionId, UpdateTransactionRequest request, Long userId) {
	    log.info("Updating transaction with id: {} for user: {}", transactionId, userId);

	    TransactionEntity transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
	            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

	    BigDecimal oldAmount = transaction.getAmount();
	    CategoryType oldType = transaction.getType();
	    boolean amountChanged = false;
	    boolean typeChanged = false;

	    if (request.getCategoryId() != null && !request.getCategoryId().equals(transaction.getCategory().getId())) {
	        CategoryEntity newCategory = categoryRepository.findById(request.getCategoryId())
	                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
	        transaction.setCategory(newCategory);
	    }

	    if (request.getAmount() != null && request.getAmount().compareTo(oldAmount) != 0) {
	        transaction.setAmount(request.getAmount());
	        amountChanged = true;
	    }

	    if (request.getType() != null && !request.getType().equals(oldType)) {
	        transaction.setType(request.getType());
	        typeChanged = true;
	    }

	    if (request.getNote() != null) transaction.setNote(request.getNote());
	    if (request.getTransactionDate() != null) transaction.setTransactionDate(request.getTransactionDate());

	    if (amountChanged || typeChanged) {
	        WalletEntity wallet = transaction.getWallet();
	        BigDecimal walletBalance = wallet.getBalance();

	        if (oldType == CategoryType.INCOME) {
	            walletBalance = walletBalance.subtract(oldAmount);
	        } else {
	            walletBalance = walletBalance.add(oldAmount);
	        }

	        BigDecimal newAmount = request.getAmount() != null ? request.getAmount() : oldAmount;
	        CategoryType newType = request.getType() != null ? request.getType() : oldType;

	        if (newType == CategoryType.INCOME) {
	            walletBalance = walletBalance.add(newAmount);
	        } else {
	            walletBalance = walletBalance.subtract(newAmount);
	        }

	        walletService.updateBalance(wallet, walletBalance);
	    }

	    TransactionEntity updatedTransaction = transactionRepository.save(transaction);
	    log.info("Transaction updated successfully with id: {}", updatedTransaction.getId());

	    kafkaTemplate.send("transaction.updated", mapToResponse(updatedTransaction));
	    return mapToResponse(updatedTransaction);
	}

	@Override
	@Transactional
	public void deleteTransaction(Long transactionId, Long userId) {
		log.info("Deleting transaction with id: {} for user: {}", transactionId, userId);

		TransactionEntity transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

		WalletEntity wallet = transaction.getWallet();
		if (transaction.getType() == CategoryType.EXPENSE) {
			wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
		} else {
			wallet.setBalance(wallet.getBalance().subtract(transaction.getAmount()));
		}
		walletRepository.save(wallet);
		transactionRepository.delete(transaction);
		log.info("Transaction deleted successfully with id: {}", transactionId);

		kafkaTemplate.send("transaction.deleted", transactionId);
	}

	@Override
	public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable) {
		log.info("Fetching transactions for user: {}", userId);
		Page<TransactionEntity> transactions = transactionRepository.findByUserId(userId, pageable);
		return transactions.map(this::mapToResponse);
	}

	@Override
	public Page<TransactionResponse> getUserTransactionsByType(Long userId, CategoryType type, Pageable pageable) {
		log.info("Fetching transactions for user: {} with type: {}", userId, type);
		Page<TransactionEntity> transactions = transactionRepository.findByUserIdAndType(userId, type, pageable);
		return transactions.map(this::mapToResponse);
	}

	@Override
	public TransactionResponse getTransactionById(Long transactionId, Long userId) {
		log.info("Fetching transaction with id: {} for user: {}", transactionId, userId);
		TransactionEntity transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
		return mapToResponse(transaction);
	}

	@Override
	public List<TransactionResponse> getTransactionsByDateRange(Long userId, LocalDateTime startDate,
			LocalDateTime endDate) {
		log.info("Fetching transactions for user: {} between {} and {}", userId, startDate, endDate);
		List<TransactionEntity> transactions = transactionRepository.findByUserIdAndDateRange(userId, startDate,
				endDate);
		return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	@Override
	public Page<TransactionResponse> getWalletTransactions(Long walletId, Long userId, Pageable pageable) {
		log.info("Fetching transactions for wallet: {}", walletId);

		if (!walletService.validateWalletAccess(walletId, userId)) {
			throw new ResourceNotFoundException("Wallet not found or access denied");
		}

		Page<TransactionEntity> transactions = transactionRepository.findByWalletId(walletId, pageable);
		return transactions.map(this::mapToResponse);
	}

	@Override
	public List<TransactionResponse> getCategoryTransactions(Long categoryId, Long userId) {
		log.info("Fetching transactions for category: {}", categoryId);
		List<TransactionEntity> transactions = transactionRepository.findByCategoryIdAndUserId(categoryId, userId);
		return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	@Override
	public BigDecimal getTotalAmount(Long userId, CategoryType type, LocalDateTime startDate, LocalDateTime endDate) {
		log.info("Calculating total {} for user: {} between {} and {}", type, userId, startDate, endDate);
		BigDecimal total = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, type, startDate, endDate);
		return total != null ? total : BigDecimal.ZERO;
	}

	@Override
	public long getUserTransactionCount(Long userId) {
		return transactionRepository.countByUserId(userId);
	}

	private TransactionResponse mapToResponse(TransactionEntity entity) {
		return TransactionResponse.builder()
				.id(entity.getId())
				.userId(entity.getUserId())
				.walletId(entity.getWallet().getId())
				.walletName(entity.getWallet().getName())
				.categoryId(entity.getCategory().getId())
				.categoryName(entity.getCategory().getName())
				.categoryIcon(entity.getCategory().getIconPath())
				.amount(entity.getAmount())
				.type(entity.getType())
				.note(entity.getNote())
				.transactionDate(entity.getTransactionDate())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}