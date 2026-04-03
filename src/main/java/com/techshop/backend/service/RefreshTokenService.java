package com.techshop.backend.service;

import com.techshop.backend.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String email, String token, long expiryMs);
    RefreshToken verifyRefreshToken(String token);
    void deleteByEmail(String email);
}