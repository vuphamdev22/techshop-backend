package com.techshop.backend.controller.auth;

import com.techshop.backend.dto.request.LoginRequest;
import com.techshop.backend.dto.request.RefreshRequest;
import com.techshop.backend.dto.request.RegisterRequest;
import com.techshop.backend.dto.response.JwtResponse;
import com.techshop.backend.entity.User;
import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.security.JwtTokenProvider;
import com.techshop.backend.service.AuthService;
import com.techshop.backend.service.Impl.RefreshTokenServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenServiceImpl refreshTokenService;

    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok("Register success");
    }

    // ✅ LOGIN (trả access + refresh token)
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    // 🔥 REFRESH TOKEN (QUAN TRỌNG)
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@RequestBody RefreshRequest request) {

        String refreshToken = request.getRefreshToken();
        // validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 👉 lấy lại role từ DB (để tạo access token mới)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ FIX: thêm userId vào đây
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                List.of(user.getRole().name())
        );


        return ResponseEntity.ok(new JwtResponse(newAccessToken, refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            String email = jwtTokenProvider.getEmailFromToken(token);

            refreshTokenService.deleteByEmail(email);
        }

        return ResponseEntity.ok("Logout success");
    }

    // ✅ TEST
    @GetMapping("/profile")
    public String profile(){
        return "You are logged in";
    }
}