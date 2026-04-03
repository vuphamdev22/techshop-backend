package com.techshop.backend.service;

import com.techshop.backend.dto.request.LoginRequest;
import com.techshop.backend.dto.request.RegisterRequest;
import com.techshop.backend.dto.response.JwtResponse;

public interface AuthService {

    void register(RegisterRequest request);

    JwtResponse login(LoginRequest request);
}