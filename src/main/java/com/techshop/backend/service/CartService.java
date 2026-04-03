package com.techshop.backend.service;

import com.techshop.backend.dto.request.AddToCartRequest;
import com.techshop.backend.dto.response.CartResponse;

public interface CartService {

    // 🟢 Thêm sản phẩm vào giỏ
    CartResponse addToCart(Long userId, AddToCartRequest request);

    // 🔵 Lấy giỏ hàng
    CartResponse getCart(Long userId);

    // 🟡 Cập nhật số lượng
    CartResponse updateQuantity(Long userId, Long cartItemId, Integer quantity);

    // 🔴 Xóa 1 item khỏi giỏ
    CartResponse removeItem(Long userId, Long cartItemId);

    // ⚫ Xóa toàn bộ giỏ hàng
    void clearCart(Long userId);

}