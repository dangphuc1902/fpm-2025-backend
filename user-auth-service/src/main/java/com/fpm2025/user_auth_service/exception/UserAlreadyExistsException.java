package com.fpm2025.user_auth_service.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String messgae){super(messgae);}
    private static final long serialVersionUID = 1L;
}
