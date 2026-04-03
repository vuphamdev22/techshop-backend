package com.techshop.backend.controller.user;

import com.techshop.backend.dto.request.AddToCartRequest;
import com.techshop.backend.dto.request.UpdateCartItemRequest;
import com.techshop.backend.dto.response.CartResponse;
import com.techshop.backend.security.JwtTokenProvider;
import com.techshop.backend.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtTokenProvider jwtTokenProvider;

    // 🟢 ADD TO CART
    @PostMapping("/add")
    public CartResponse addToCart(
            HttpServletRequest request,
            @RequestBody AddToCartRequest requestBody
    ) {
        Long userId = getUserId(request);
        return cartService.addToCart(userId, requestBody);
    }

    // 🔵 GET CART
    @GetMapping
    public CartResponse getCart(HttpServletRequest request) {
        Long userId = getUserId(request);
        return cartService.getCart(userId);
    }

    // 🟡 UPDATE QUANTITY
    @PutMapping("/update")
    public CartResponse updateQuantity(
            HttpServletRequest request,
            @RequestBody UpdateCartItemRequest requestBody
    ) {
        Long userId = getUserId(request);

        return cartService.updateQuantity(
                userId,
                requestBody.getCartItemId(),
                requestBody.getQuantity()
        );
    }

    // 🔴 REMOVE ITEM
    @DeleteMapping("/item/{cartItemId}")
    public CartResponse removeItem(
            HttpServletRequest request,
            @PathVariable Long cartItemId
    ) {
        Long userId = getUserId(request);
        return cartService.removeItem(userId, cartItemId);
    }

    // ⚫ CLEAR CART
    @DeleteMapping("/clear")
    public String clearCart(HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.clearCart(userId);
        return "Cart cleared";
    }

    // 🔥 HELPER: lấy userId từ JWT
    private Long getUserId(HttpServletRequest request) {
        String token = extractToken(request);
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    // 🔥 HELPER: extract token
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token");
        }

        return header.substring(7);
    }
}