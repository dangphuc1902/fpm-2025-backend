package com.fpm2025.user_auth_service.service.imp;

 import java.util.Map;

import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.payload.request.UserLoginRequest;
import com.fpm2025.user_auth_service.payload.request.UserRegisterRequest;

import jakarta.servlet.http.HttpServletResponse;


public interface AuthorServiceImp {

     String checkLogin(UserLoginRequest userLoginRequest, HttpServletResponse response);
     UserEntity registerUser(UserRegisterRequest userRegister);
     Map<String, Object> handleGoogleLogin(String code);
}
