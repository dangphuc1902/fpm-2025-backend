package com.fpm_2025.wallet_service.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String messgae){super(messgae);}
    private static final long serialVersionUID = 1L;
}
