package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.CheckoutRequest;
import com.techshop.backend.dto.response.OrderResponse;
import com.techshop.backend.entity.*;
import com.techshop.backend.enums.OrderStatus;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.dto.request.PaymentCreateRequest;
import com.techshop.backend.enums.PaymentMethod;
import com.techshop.backend.mapper.OrderMapper;
import com.techshop.backend.repository.CartRepository;
import com.techshop.backend.repository.OrderRepository;
import com.techshop.backend.service.PaymentService;
import com.techshop.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional // 🔥 rất quan trọng (rollback nếu lỗi)
    public OrderResponse checkout(Long userId, CheckoutRequest request, String ipAddress) {

        // 🔥 1. lấy cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        // 🔥 2. tạo order
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // 🔥 PAYMENT DEFAULT (không phá logic cũ)
        order.setPaymentMethod(request.getPaymentMethod());
        order.setIsPaid(false);

        // 🔥 3. mapping shipping
        ShippingAddress shipping = new ShippingAddress();
        shipping.setFirstName(request.getFirstName());
        shipping.setLastName(request.getLastName());
        shipping.setEmail(request.getEmail());
        shipping.setPhone(request.getPhone());
        shipping.setAddress(request.getAddress());
        shipping.setCity(request.getCity());
        shipping.setState(request.getState());
        shipping.setZipCode(request.getZipCode());

        order.setShippingAddress(shipping);

        // 🔥 4. mapping order items
        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (CartItem item : cart.getItems()) {

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(item.getProduct());
            oi.setQuantity(item.getQuantity());
            oi.setPrice(item.getProduct().getPrice());

            total += item.getQuantity() * item.getProduct().getPrice();

            orderItems.add(oi);
        }

        order.setItems(orderItems);
        order.setTotalPrice(total);

        // 🔥 5. save order
        orderRepository.save(order);

        // 🔥 6. clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        // 🔥 7. Handle payment
        String paymentUrl = null;
        if (request.getPaymentMethod() != PaymentMethod.COD) {
            // Online payment: create payment and get URL
            PaymentCreateRequest paymentRequest = new PaymentCreateRequest();
            paymentRequest.setMethod(request.getPaymentMethod());
            var paymentResponse = paymentService.createPayment(order.getId(), paymentRequest);
            paymentUrl = paymentResponse.getPaymentUrl();
        }

        // 🔥 8. return
        return OrderMapper.toResponseWithPaymentUrl(order, paymentUrl);
    }

    @Override
    public List<OrderResponse> getMyOrders(Long userId) {

        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getOrderDetail(Long userId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return OrderMapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus newStatus;

        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase()); // 🔥 fix input
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        orderRepository.save(order);

        return OrderMapper.toResponse(order);
    }

    @Override    @Transactional
    public OrderResponse markOrderAsPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Chỉ cho COD orders
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        // Chỉ nếu chưa paid
        if (order.getIsPaid()) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        order.setIsPaid(true);
        order.setStatus(OrderStatus.CONFIRMED);

        Order savedOrder = orderRepository.save(order);
        return OrderMapper.toResponse(savedOrder);
    }

    @Override    public OrderResponse getOrderDetailForAdmin(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        return OrderMapper.toResponse(order);
    }

    // 🔥 validate flow (chuẩn business)
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {

        // ❌ trạng thái cuối không update
        if (current == OrderStatus.CANCELLED || current == OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_FINAL_STATUS);
        }

        // ❌ phải confirm trước khi ship
        if (current == OrderStatus.PENDING && next == OrderStatus.SHIPPED) {
            throw new AppException(ErrorCode.MUST_CONFIRM_BEFORE_SHIPPING);
        }

        // ❌ không quay ngược trạng thái
        if (current == OrderStatus.CONFIRMED && next == OrderStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}