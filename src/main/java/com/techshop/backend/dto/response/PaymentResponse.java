package com.techshop.backend.dto.response;

import com.techshop.backend.enums.PaymentMethod;
import com.techshop.backend.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionId;
    private Double amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String note;
    private String paymentUrl; // chỉ trả về khi tạo mới
}