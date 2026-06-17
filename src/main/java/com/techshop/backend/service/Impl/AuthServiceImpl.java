package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.LoginRequest;
import com.techshop.backend.dto.request.RegisterRequest;
import com.techshop.backend.dto.response.JwtResponse;
import com.techshop.backend.dto.response.UserDTO;
import com.techshop.backend.entity.User;
import com.techshop.backend.enums.Role;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.repository.RefreshTokenRepository;
import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.security.JwtTokenProvider;
import com.techshop.backend.service.AuthService;
import com.techshop.backend.service.EmailService;
import com.techshop.backend.service.MembershipService;
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
    private final EmailService emailService;
    private final MembershipService membershipService;

    @Override
    public void register(RegisterRequest request) {

        // kiểm tra email tồn tại
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String lName = request.getLastName();
        if (lName == null || lName.trim().isEmpty()) {
            lName = ".";
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(lName)
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        // Gửi email chào mừng bất đồng bộ (không gây chậm luồng đăng ký)
        String fullNameGreeting = user.getFirstName();
        if (user.getLastName() != null && !user.getLastName().equals(".")) {
            fullNameGreeting += " " + user.getLastName();
        }
        emailService.sendWelcomeEmail(user.getEmail(), fullNameGreeting);

        // Phát voucher chào mừng Bronze 5%
        membershipService.issueWelcomeVoucher(user.getId());
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

        UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );

        return new JwtResponse(accessToken, refreshToken, userDTO);
    }
}