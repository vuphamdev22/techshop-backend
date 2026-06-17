package com.techshop.backend.config;

import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.security.JwtAuthenticationFilter;
import com.techshop.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    // ✅ FIX: truyền đủ dependency
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // 🔓 PUBLIC
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/public/track/**").permitAll()
                        .requestMatchers("/api/payment/vnpay-return").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()

                        // 🔓 public categories
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                        // 🔓 public chatbot & websockets
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/ws-chat/**").permitAll()

                        // 🔓 public product
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // 🔓 public coupons
                        .requestMatchers(HttpMethod.GET, "/api/coupons").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()

                        // ✅ CART (phải login)
                        .requestMatchers("/api/cart/**").authenticated()

                        // ✅ ORDERS (phải login)
                        .requestMatchers("/api/orders/**").authenticated()

                        // ✅ MEMBERSHIP (user endpoints require login, admin requires ADMIN role)
                        .requestMatchers("/api/membership/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/membership/**").authenticated()

                        // 🔒 ADMIN
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // 🔒 các API auth còn lại phải login
                        .requestMatchers("/api/auth/**").authenticated()

                        // preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}