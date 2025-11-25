package com.fpm_2025.wallet_service.exception;

import com.fpm2025.core.dto.response.*;
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
        String jsonRequest = gson.toJson(e);
        logger.info(jsonRequest);
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setStatusCode(500);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData("");
        logger.info(gson.toJson(baseResponse));
        return new ResponseEntity<>(baseResponse, HttpStatus.OK);
    }

    @ExceptionHandler(value = { MethodArgumentNotValidException.class })
    public ResponseEntity<?> handleValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>(); // Luuw theo dinh dang key, value errors.put(field,message);

        e.getBindingResult().getAllErrors().forEach(item -> {
            String field = ((FieldError) item).getField();
            String message = item.getDefaultMessage();

            errors.put(field, message);
        });

        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setStatusCode(400);
        baseResponse.setMessage("");
        baseResponse.setData(errors);

        return new ResponseEntity<>(baseResponse, HttpStatus.OK);
    }
}
