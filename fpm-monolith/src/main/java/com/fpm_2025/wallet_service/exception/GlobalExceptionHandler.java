package com.fpm_2025.wallet_service.exception;
 
import com.fpm2025.domain.common.BaseResponse;
import com.fpm_2025.fpm_microservice_libs.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
 
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        
        BaseResponse<Void> errorResponse = BaseResponse.error(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
            
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
 
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BaseResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        log.error("Duplicate resource: {}", ex.getMessage());
        
        BaseResponse<Void> errorResponse = BaseResponse.error(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
            
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
 
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<BaseResponse<Void>> handleInsufficientBalanceException(
            InsufficientBalanceException ex, WebRequest request) {
        log.error("Insufficient balance: {}", ex.getMessage());
        
        BaseResponse<Void> errorResponse = BaseResponse.error(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}