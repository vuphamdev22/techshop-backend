package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.AddToCartRequest;
import com.techshop.backend.dto.response.CartItemResponse;
import com.techshop.backend.dto.response.CartResponse;
import com.techshop.backend.entity.*;
import com.techshop.backend.enums.OrderStatus;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.repository.*;
import com.techshop.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {

        // 1. lấy hoặc tạo cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        // 2. lấy product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. check item
        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
        }

        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Override
    public CartResponse getCart(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        List<CartItemResponse> items = new ArrayList<>();
        double total = 0;

        for (CartItem item : cart.getItems()) {

            CartItemResponse res = new CartItemResponse();
            res.setId(item.getId());
            res.setProductId(item.getProduct().getId());
            res.setProductName(item.getProduct().getName());
            res.setPrice(item.getProduct().getPrice());
            res.setQuantity(item.getQuantity());

            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                res.setImage(item.getProduct().getImages().get(0).getImageUrl());
            }

            total += item.getProduct().getPrice() * item.getQuantity();

            items.add(res);
        }

        CartResponse response = new CartResponse();
        response.setItems(items);
        response.setTotalPrice(total);

        return response;
    }

    @Override
    public CartResponse updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 🔥 check item có thuộc user không (rất quan trọng)
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Override
    public CartResponse removeItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        cartItemRepository.delete(item);

        return getCart(userId);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        cart.getItems().clear();
        cartRepository.save(cart);
    }
}