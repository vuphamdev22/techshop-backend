package com.techshop.backend.controller.admin;


import com.techshop.backend.dto.request.UpdateOrderStatusRequest;
import com.techshop.backend.dto.response.OrderResponse;
import com.techshop.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    // 🔵 ALL ORDERS
    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    // 🟣 ORDER DETAIL (admin xem mọi order)
    @GetMapping("/{id}")
    public OrderResponse getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetailForAdmin(id);
    }

    // 🔴 UPDATE STATUS
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderStatus(id, request.getStatus());
    }

    // 💰 MANUAL PAY COD
    @PutMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> markAsPaid(@PathVariable Long id) {
        OrderResponse response = orderService.markOrderAsPaid(id);
        return ResponseEntity.ok(response);
    }
}