package com.fpm_2025.wallet_service.service;

import com.fpm_2025.wallet_service.entity.CategoryEntity;
import com.fpm_2025.wallet_service.entity.TransactionEntity;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import com.fpm_2025.wallet_service.exception.ResourceNotFoundException;
import com.fpm_2025.wallet_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {
	
	@Autowired
	private TransactionRepository transactionRepository;
	@Autowired
	private WalletService walletService;
	@Autowired
	private CategoryRepository categoryRepository;

	@Transactional
	public TransactionResponse createTransaction(CreateTransactionRequest request, Long userId) {
		log.info("Creating transaction for user: {}, amount: {}", userId, request.getAmount());

		// Validate wallet access
		if (!walletService.validateWalletAccess(request.getWalletId(), userId)) {
			throw new ResourceNotFoundException("Wallet not found or access denied");
		}

		// Get wallet entity
		WalletEntity wallet = walletService.getWalletEntity(request.getWalletId(), userId);

		// Validate category
		CategoryEntity category = categoryRepository.findById(request.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

		// Ensure transaction type matches category type
		if (!category.getType().equals(request.getType())) {
			throw new IllegalArgumentException("Transaction type must match category type");
		}

		// Check sufficient balance for expenses
		if (request.getType() == CategoryType.EXPENSE) {
			if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
				throw new IllegalStateException("Insufficient balance in wallet");
			}
		}

		// Create transaction
		TransactionEntity transaction = TransactionEntity.builder().userId(userId).wallet(wallet).category(category)
				.amount(request.getAmount()).type(request.getType()).note(request.getNote())
				.transactionDate(
						request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
				.build();

		TransactionEntity savedTransaction = transactionRepository.save(transaction);

		// Update wallet balance
		boolean isAddition = request.getType() == CategoryType.INCOME;
		walletService.updateBalance(request.getWalletId(), userId, request.getAmount(), isAddition);

		log.info("Transaction created successfully with id: {}", savedTransaction.getId());

		// TODO: Publish event to RabbitMQ for analytics and notifications
		// publishTransactionCreatedEvent(savedTransaction);

		return mapToResponse(savedTransaction);
	}

	@Transactional
	public TransactionResponse updateTransaction(Long transactionId, UpdateTransactionRequest request, Long userId) {
		log.info("Updating transaction with id: {} for user: {}", transactionId, userId);

		TransactionEntity transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

		BigDecimal oldAmount = transaction.getAmount();
		CategoryType oldType = transaction.getType();
		boolean amountChanged = false;
		boolean typeChanged = false;

		// Update category if provided
		if (request.getCategoryId() != null && !request.getCategoryId().equals(transaction.getCategory().getId())) {
			CategoryEntity newCategory = categoryRepository.findById(request.getCategoryId()).orElseThrow(
					() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

			transaction.setCategory(newCategory);

			// If type is also being updated, ensure it matches new category
			if (request.getType() != null && !newCategory.getType().equals(request.getType())) {
				throw new IllegalArgumentException("Transaction type must match category type");
			}
		}

		// Update amount if provided
		if (request.getAmount() != null && request.getAmount().compareTo(oldAmount) != 0) {
			transaction.setAmount(request.getAmount());
			amountChanged = true;
		}

		// Update type if provided
		if (request.getType() != null && !request.getType().equals(oldType)) {
			transaction.setType(request.getType());
			typeChanged = true;
		}

		// Update other fields
		if (request.getNote() != null) {
			transaction.setNote(request.getNote());
		}

		if (request.getTransactionDate() != null) {
			transaction.setTransactionDate(request.getTransactionDate());
		}

		// Recalculate wallet balance if amount or type changed
		if (amountChanged || typeChanged) {
			WalletEntity wallet = transaction.getWallet();

			// Revert old transaction
			if (oldType == CategoryType.INCOME) {
				wallet.setBalance(wallet.getBalance().subtract(oldAmount));
			} else {
				wallet.setBalance(wallet.getBalance().add(oldAmount));
			}

			// Apply new transaction
			BigDecimal newAmount = request.getAmount() != null ? request.getAmount() : oldAmount;
			CategoryType newType = request.getType() != null ? request.getType() : oldType;

			if (newType == CategoryType.INCOME) {
				wallet.setBalance(wallet.getBalance().add(newAmount));
			} else {
				if (wallet.getBalance().compareTo(newAmount) < 0) {
					throw new IllegalStateException("Insufficient balance in wallet for this update");
				}
				wallet.setBalance(wallet.getBalance().subtract(newAmount));
			}
		}

		TransactionEntity updatedTransaction = transactionRepository.save(transaction);
		log.info("Transaction updated successfully with id: {}", updatedTransaction.getId());

		// TODO: Publish event to RabbitMQ
		// publishTransactionUpdatedEvent(updatedTransaction);

		return mapToResponse(updatedTransaction);
	}

	@Transactional
	public void deleteTransaction(Long transactionId, Long userId) {
		log.info("Deleting transaction with id: {} for user: {}", transactionId, userId);

		TransactionEntity transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

		// Revert wallet balance
		WalletEntity wallet = transaction.getWallet();
		if (transaction.getType() == CategoryType.INCOME) {
			wallet.setBalance(wallet.getBalance().subtract(transaction.getAmount()));
		} else {
			wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
		}

		transactionRepository.delete(transaction);
		log.info("Transaction deleted successfully with id: {}", transactionId);

		// TODO: Publish event to RabbitMQ
		// publishTransactionDeletedEvent(transaction);
	}

	public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable) {
		log.info("Fetching transactions for user: {}", userId);
		Page<TransactionEntity> transactions = transactionRepository.findByUserId(userId, pageable);
		return transactions.map(this::mapToResponse);
	}

	public Page<TransactionResponse> getUserTransactionsByType(Long userId, CategoryType type, Pageable pageable) {
		log.info("Fetching transactions for user: {} with type: {}", userId, type);
		Page<TransactionEntity> transactions = transactionRepository.findByUserIdAndType(userId, type, pageable);
		return transactions.map(this::mapToResponse);
	}

	public TransactionResponse getTransactionById(Long transactionId, Long userId) {
		log.info("Fetching transaction with id: {} for user: {}", transactionId, userId);
		TransactionEntity transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
		return mapToResponse(transaction);
	}

	public List<TransactionResponse> getTransactionsByDateRange(Long userId, LocalDateTime startDate,
			LocalDateTime endDate) {
		log.info("Fetching transactions for user: {} between {} and {}", userId, startDate, endDate);
		List<TransactionEntity> transactions = transactionRepository.findByUserIdAndDateRange(userId, startDate,
				endDate);
		return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	public Page<TransactionResponse> getWalletTransactions(Long walletId, Long userId, Pageable pageable) {
		log.info("Fetching transactions for wallet: {}", walletId);

		// Validate wallet access
		if (!walletService.validateWalletAccess(walletId, userId)) {
			throw new ResourceNotFoundException("Wallet not found or access denied");
		}

		Page<TransactionEntity> transactions = transactionRepository.findByWalletId(walletId, pageable);
		return transactions.map(this::mapToResponse);
	}

	public List<TransactionResponse> getCategoryTransactions(Long categoryId, Long userId) {
		log.info("Fetching transactions for category: {}", categoryId);
		List<TransactionEntity> transactions = transactionRepository.findByCategoryIdAndUserId(categoryId, userId);
		return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	public BigDecimal getTotalAmount(Long userId, CategoryType type, LocalDateTime startDate, LocalDateTime endDate) {
		log.info("Calculating total {} for user: {} between {} and {}", type, userId, startDate, endDate);
		BigDecimal total = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, type, startDate, endDate);
		return total != null ? total : BigDecimal.ZERO;
	}

	public long getUserTransactionCount(Long userId) {
		return transactionRepository.countByUserId(userId);
	}

	// Mapping method
	private TransactionResponse mapToResponse(TransactionEntity entity) {
		return TransactionResponse.builder().id(entity.getId()).userId(entity.getUserId())
				.walletId(entity.getWallet().getId()).walletName(entity.getWallet().getName())
				.categoryId(entity.getCategory().getId()).categoryName(entity.getCategory().getName())
				.amount(entity.getAmount()).type(entity.getType()).note(entity.getNote())
				.transactionDate(entity.getTransactionDate()).createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt()).build();
	}
}