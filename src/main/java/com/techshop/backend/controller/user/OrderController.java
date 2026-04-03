package com.techshop.backend.controller.user;

import com.techshop.backend.dto.request.CheckoutRequest;
import com.techshop.backend.dto.response.OrderResponse;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.security.JwtTokenProvider;
import com.techshop.backend.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtTokenProvider jwtTokenProvider;

    // 🟢 CHECKOUT
    @PostMapping("/checkout")
    public OrderResponse checkout(
            HttpServletRequest request,
            @RequestBody CheckoutRequest body
    ) {
        Long userId = getUserId(request);
        String ipAddress = getClientIp(request);

        return orderService.checkout(userId, body, ipAddress);
    }

    // 🔵 MY ORDERS
    @GetMapping
    public List<OrderResponse> getMyOrders(HttpServletRequest request) {
        Long userId = getUserId(request);
        return orderService.getMyOrders(userId);
    }

    // 🟣 ORDER DETAIL
    @GetMapping("/{orderId}")
    public OrderResponse getOrderDetail(
            HttpServletRequest request,
            @PathVariable Long orderId
    ) {
        Long userId = getUserId(request);
        return orderService.getOrderDetail(userId, orderId);
    }

    // ================= HELPER =================

    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String token = header.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * 🔥 Lấy IP thật (fix cho production sau này)
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");

        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}