package com.techshop.backend.service.Impl;

import com.techshop.backend.entity.RefreshToken;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.repository.RefreshTokenRepository;
import com.techshop.backend.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // 🔥 Tạo refresh token và lưu DB
    @Override
    public RefreshToken createRefreshToken(String email, String token, long expiryMs) {

        // mỗi user chỉ giữ 1 refresh token (tránh spam login nhiều thiết bị)
        refreshTokenRepository.deleteByEmail(email);

        RefreshToken refreshToken = RefreshToken.builder()
                .email(email)
                .token(token)
                .expiryDate(Instant.now().plusMillis(expiryMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // 🔥 Verify refresh token
    @Override
    public RefreshToken verifyRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // check hết hạn
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {

            // token hết hạn thì xóa luôn
            refreshTokenRepository.delete(refreshToken);

            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return refreshToken;
    }

    @Override
    public void deleteByEmail(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }


}