package com.fpm_2025.wallet_service.exception;

import com.fpm2025.domain.common.BaseResponse;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ResponeException {
    private Logger logger  = LoggerFactory.getLogger(ResponeException.class);
    private Gson gson = new Gson();

    @ExceptionHandler(value = { RuntimeException.class })
    public ResponseEntity<?> handleException(Exception e) {
        logger.error("Internal Server Error: ", e);
        BaseResponse<String> baseResponse = new BaseResponse<>();
        baseResponse.setStatusCode(500);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData("");
        return new ResponseEntity<>(baseResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = { MethodArgumentNotValidException.class })
    public ResponseEntity<?> handleValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(item -> {
            String field = ((FieldError) item).getField();
            String message = item.getDefaultMessage();

            errors.put(field, message);
        });

        BaseResponse<Map<String, String>> baseResponse = new BaseResponse<>();
        baseResponse.setStatusCode(400);
        baseResponse.setMessage("Validation Failed");
        baseResponse.setData(errors);

        return new ResponseEntity<>(baseResponse, HttpStatus.BAD_REQUEST);
    }
}
