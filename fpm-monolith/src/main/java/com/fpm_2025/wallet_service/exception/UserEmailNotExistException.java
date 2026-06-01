package com.fpm_2025.wallet_service.exception;

public class UserEmailNotExistException extends RuntimeException {
	public UserEmailNotExistException(String message) {
		super(message);
	}
	
	private static final long serialVersionUID = 1L;

}
