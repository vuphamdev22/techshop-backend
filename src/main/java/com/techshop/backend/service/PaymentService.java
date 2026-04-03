package com.techshop.backend.service;

import com.techshop.backend.dto.request.PaymentCreateRequest;
import com.techshop.backend.dto.response.PaymentResponse;
import com.techshop.backend.entity.Payment;

import java.util.Map;

public interface PaymentService {

    /**
     * Tạo payment cho order (online payment)
     */
    PaymentResponse createPayment(Long orderId, PaymentCreateRequest request, String ipAddress);

    /**
     * Xử lý return từ VNPAY (REAL PAYMENT)
     */
    boolean handleVnpayReturn(Map<String, String> params);

    /**
     * Lấy payment theo orderId
     */
    PaymentResponse getPaymentByOrderId(Long orderId);

    /**
     * Cập nhật trạng thái payment thủ công (cho admin)
     */
    PaymentResponse updatePaymentStatus(Long paymentId, String status);

    /**
     * Tạo URL thanh toán VNPAY
     */
    String generateVnPayUrl(Payment payment, String ipAddress);
    /**
     * Lấy orderId từ txnRef
     */
    String getOrderIdFromTxnRef(Map<String, String> params);
}