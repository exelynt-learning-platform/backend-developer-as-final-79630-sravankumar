package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.auth.LoginRequest;
import com.example.resourcebooking.dto.auth.LoginResponse;
import com.example.resourcebooking.dto.auth.RegisterRequest;
import com.example.resourcebooking.dto.user.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);
}
