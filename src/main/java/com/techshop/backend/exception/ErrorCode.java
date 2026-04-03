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
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment not found"),
    PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Payment already exists for this order"),
    INVALID_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "Invalid payment method for online payment"),
    INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, "Invalid signature"),
    INVALID_PAYMENT_STATE(HttpStatus.BAD_REQUEST, "Invalid payment state"),
    INVALID_ORDER_STATE(HttpStatus.BAD_REQUEST, "Invalid order state"),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Out of stock"),
    INVALID_TRANSACTION(HttpStatus.BAD_REQUEST, "Invalid transaction"),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "Invalid status");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message){
        this.status = status;
        this.message = message;
    }
}
