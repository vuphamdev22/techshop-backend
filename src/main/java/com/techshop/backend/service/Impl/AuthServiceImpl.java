package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.LoginRequest;
import com.techshop.backend.dto.request.RegisterRequest;
import com.techshop.backend.dto.response.JwtResponse;
import com.techshop.backend.entity.User;
import com.techshop.backend.enums.Role;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.repository.RefreshTokenRepository;
import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.security.JwtTokenProvider;
import com.techshop.backend.service.AuthService;
import com.techshop.backend.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void register(RegisterRequest request) {

        // kiểm tra email tồn tại
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);
    }

    @Override
    public JwtResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.USER_DISABLED);
        }

        // 🔥 FIX: thêm userId
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                List.of(user.getRole().name())
        );

        // 🔥 REFRESH TOKEN
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getEmail()
        );

        // 🔥 🔥 🔥 THÊM ĐOẠN NÀY (QUAN TRỌNG NHẤT)
        refreshTokenService.createRefreshToken(
                user.getEmail(),
                refreshToken,
                7 * 24 * 60 * 60 * 1000
        );

        return new JwtResponse(accessToken, refreshToken);
    }
}