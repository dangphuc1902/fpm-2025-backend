package com.fpm2025.user_auth_service.exception;
 
import com.fpm2025.domain.common.BaseResponse;
import com.fpm_2025.fpm_microservice_libs.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
 
@RestControllerAdvice
@Slf4j
public class UserAuthGlobalExceptionHandler extends BaseGlobalExceptionHandler {
 
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<BaseResponse<Void>> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, WebRequest request) {
        log.error("User already exists: {}", ex.getMessage());
        
        BaseResponse<Void> errorResponse = BaseResponse.error(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
            
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
 
    @ExceptionHandler(UserEmailNotExistException.class)
    public ResponseEntity<BaseResponse<Void>> handleUserEmailNotExistException(
            UserEmailNotExistException ex, WebRequest request) {
        log.error("User email not found: {}", ex.getMessage());
        
        BaseResponse<Void> errorResponse = BaseResponse.error(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
            
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<BaseResponse<Void>> handleInvalidPasswordException(
            InvalidPasswordException ex, WebRequest request) {
        log.error("Invalid password error: {}", ex.getMessage());
        
        BaseResponse<Void> errorResponse = BaseResponse.error(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
            
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.error("Malformed JSON payload error: {}", ex.getMessage());
        
        String specificMessage = "Malformed JSON request body";
        if (ex.getMostSpecificCause() != null) {
            specificMessage = ex.getMostSpecificCause().getMessage();
        }
        
        BaseResponse<Void> errorResponse = BaseResponse.error(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            specificMessage,
            request.getDescription(false).replace("uri=", "")
        );
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
