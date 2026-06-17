package com.techshop.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        System.out.println(" [JWT DEBUG] Request URI: " + request.getRequestURI() + " | Method: " + request.getMethod());
        System.out.println(" [JWT DEBUG] Auth Header: " + header);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            boolean isValid = jwtTokenProvider.validateToken(token);
            System.out.println(" [JWT DEBUG] Token Valid: " + isValid);

            if (isValid) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                System.out.println(" [JWT DEBUG] Token Email: " + email);

                // 🔥 LẤY ROLE TỪ TOKEN (KHÔNG QUERY DB)
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);
                System.out.println(" [JWT DEBUG] Token Roles: " + roles);

                List<SimpleGrantedAuthority> authorities =
                        roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());
                System.out.println(" [JWT DEBUG] Granted Authorities: " + authorities);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}