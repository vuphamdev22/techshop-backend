package com.techshop.backend.service;

import com.techshop.backend.dto.request.PaymentCreateRequest;
import com.techshop.backend.dto.response.PaymentResponse;
import com.techshop.backend.entity.Payment;

import java.util.Map;

public interface PaymentService {

    /**
     * Tạo payment cho order (online payment)
     */
    PaymentResponse createPayment(Long orderId, PaymentCreateRequest request);

    /**
     * Xử lý callback từ cổng thanh toán
     */
    void handlePaymentCallback(String transactionId, boolean success, String note);

    /**
     * Lấy payment theo orderId
     */
    PaymentResponse getPaymentByOrderId(Long orderId);

    /**
     * Cập nhật trạng thái payment thủ công (cho admin)
     */
    PaymentResponse updatePaymentStatus(Long paymentId, String status);

    /**
     * Tạo mock payment URL (cho development)
     */
    String generateMockPaymentUrl(Payment payment);

    /**
     * Xử lý callback từ VNPay với validation
     */
    void handleVnPayCallback(Map<String, String> vnpParams);
}