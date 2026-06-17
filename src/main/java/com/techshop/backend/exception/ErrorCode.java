package com.techshop.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Invalid password"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "Cart not found"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CartItem not found"),
    CART_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Cart is empty"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh token not found"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh token expired"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "Invalid order status"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid status transition"),
    CANNOT_UPDATE_FINAL_STATUS(HttpStatus.BAD_REQUEST, "Cannot change final status"),
    MUST_CONFIRM_BEFORE_SHIPPING(HttpStatus.BAD_REQUEST, "Must confirm before shipping"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "Brand not found"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment not found"),
    PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Payment already exists for this order"),
    INVALID_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "Invalid payment method for online payment"),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "Invalid status"),
    INVALID_STOCK_VALUE(HttpStatus.BAD_REQUEST, "Stock must be zero or greater"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "User account is disabled"),
    PAYMENT_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create payment"),
    PAYMENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process payment"),
    BRAND_HAS_PRODUCTS(HttpStatus.CONFLICT, "Cannot delete brand with associated products"),
    CATEGORY_HAS_PRODUCTS(HttpStatus.CONFLICT, "Cannot delete category with associated products"),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "Coupon not found"),
    COUPON_ALREADY_EXISTS(HttpStatus.CONFLICT, "Coupon code already exists"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "Coupon has expired"),
    COUPON_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Coupon usage limit reached"),
    COUPON_INACTIVE(HttpStatus.BAD_REQUEST, "Coupon is inactive");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message){
        this.status = status;
        this.message = message;
    }
}
