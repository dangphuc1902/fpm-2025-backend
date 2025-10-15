package com.fpm2025.user_auth_service.exception;

public class UserEmailNotExistException extends RuntimeException {
	public UserEmailNotExistException(String message) {
		super(message);
	}
	
	private static final long serialVersionUID = 1L;

}
