package com.techshop.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final String JWT_SECRET = "mySuperSecretKey_mySuperSecretKey_mySuperSecretKey_123456";

    // 🔥 TÁCH 2 LOẠI TOKEN
    private final long ACCESS_TOKEN_EXP = 60 * 60 * 1000; // 15 phút
//    private final long ACCESS_TOKEN_EXP = 30 * 1000; // 60 giây
    private final long REFRESH_TOKEN_EXP = 7 * 24 * 60 * 60 * 1000; // 7 ngày

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    // ✅ ACCESS TOKEN (có role)
    public String generateAccessToken(Long userId, String email, List<String> roles) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXP);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ✅ LẤY USER ID (chỉ dùng cho accessToken)
    public Long getUserIdFromToken(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    // ✅ REFRESH TOKEN (KHÔNG cần role)
    public String generateRefreshToken(String email) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_EXP);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ✅ LẤY EMAIL
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // ✅ LẤY ROLE (chỉ dùng cho accessToken)
    public List<String> getRolesFromToken(String token) {
        return getClaims(token).get("roles", List.class);
    }

    // ✅ PARSE CHUNG
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ✅ VALIDATE TOKEN
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 🔥 (OPTIONAL) CHECK TOKEN HẾT HẠN RIÊNG
    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }
}