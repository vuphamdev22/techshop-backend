package com.techshop.backend.service;

import com.techshop.backend.dto.request.CheckoutRequest;
import com.techshop.backend.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse checkout(Long userId, CheckoutRequest request, String ipAddress);

    List<OrderResponse> getMyOrders(Long userId);

    OrderResponse getOrderDetail(Long userId, Long orderId);

    List<OrderResponse> getAllOrders();

    OrderResponse updateOrderStatus(Long orderId, String status);

    OrderResponse getOrderDetailForAdmin(Long orderId);

    OrderResponse markOrderAsPaid(Long orderId);
}